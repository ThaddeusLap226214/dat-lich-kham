package com.clinic.reminder.event;

import com.clinic.reminder.client.PatientClient;
import com.clinic.reminder.client.PatientContact;
import com.clinic.reminder.job.ReminderService;
import com.clinic.reminder.log.ReminderLog;
import com.clinic.reminder.log.ReminderLogRepository;
import com.clinic.reminder.log.ReminderStatus;
import com.clinic.reminder.log.ReminderType;
import com.clinic.reminder.template.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventHandler.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final ProcessedEventRepository processedEventRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final PatientClient patientClient;
    private final ReminderService reminderService;

    public AppointmentEventHandler(ProcessedEventRepository processedEventRepository,
                                   ReminderLogRepository reminderLogRepository,
                                   PatientClient patientClient,
                                   ReminderService reminderService) {
        this.processedEventRepository = processedEventRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.patientClient = patientClient;
        this.reminderService = reminderService;
    }

    @Transactional
    public void handle(AppointmentEvent event) {
        // CS36: Bỏ qua nếu eventId đã được xử lý (chống xử lý lặp)
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Sự kiện {} eventId={} đã được xử lý trước đó, bỏ qua", event.eventType(), event.eventId());
            return;
        }

        ReminderType reminderType;
        if (event.eventType() == AppointmentEvent.AppointmentEventType.APPOINTMENT_CONFIRMED) {
            reminderType = ReminderType.CONFIRMATION;
        } else if (event.eventType() == AppointmentEvent.AppointmentEventType.APPOINTMENT_CANCELLED) {
            reminderType = ReminderType.CANCELLATION;
        } else {
            // CS31: Lịch EXPIRED, COMPLETED, NO_SHOW không gửi thư
            markEventProcessed(event);
            return;
        }

        // Kiểm tra xem lịch hẹn đã có log loại này chưa (chống gửi lặp)
        ReminderLog logEntry = reminderLogRepository
                .findByAppointmentIdAndReminderType(event.appointmentId(), reminderType)
                .orElse(null);

        if (logEntry != null && (logEntry.getStatus() == ReminderStatus.SENT || logEntry.getStatus() == ReminderStatus.SKIPPED)) {
            log.info("Lịch hẹn {} đã có log {} trạng thái {}, bỏ qua",
                    event.appointmentId(), reminderType, logEntry.getStatus());
            markEventProcessed(event);
            return;
        }

        if (logEntry == null) {
            logEntry = new ReminderLog();
            logEntry.setAppointmentId(event.appointmentId());
            logEntry.setPatientId(event.patientId());
            logEntry.setAppointmentStart(event.startTime());
            logEntry.setReminderType(reminderType);
            logEntry.setAttemptCount(1);
        }

        // Lấy thông tin email của bệnh nhân từ Patient Service (F27)
        PatientContact contact = null;
        boolean patientServiceFailed = false;
        String patientServiceError = null;

        try {
            List<PatientContact> contacts = patientClient.findByIds(List.of(event.patientId()));
            if (contacts != null && !contacts.isEmpty()) {
                contact = contacts.get(0);
            }
        } catch (Exception e) {
            patientServiceFailed = true;
            patientServiceError = e.getMessage();
            log.warn("Không thể lấy email bệnh nhân {} từ Patient Service: {}", event.patientId(), e.getMessage());
        }

        // Fix lỗi 2.2: Nếu Patient Service lỗi -> lưu FAILED để cron thử lại sau, KHÔNG coi là SKIPPED
        if (patientServiceFailed) {
            logEntry.setStatus(ReminderStatus.FAILED);
            logEntry.setRecipientEmail(null);
            String reasonSuffix = event.cancelReason() != null ? " [reason:" + event.cancelReason() + "]" : "";
            logEntry.setLastError(truncate("Patient Service không phản hồi: " + patientServiceError + reasonSuffix, 1000));
            logEntry.setSentAt(null);
            reminderLogRepository.save(logEntry);
            markEventProcessed(event);
            return;
        }

        String recipientEmail = contact != null ? contact.email() : null;
        String patientName = (contact != null && contact.fullName() != null && !contact.fullName().isBlank())
                ? contact.fullName() : "Quý khách";

        // CS30: Hồ sơ thực sự không có email -> SKIPPED
        if (recipientEmail == null || recipientEmail.isBlank()) {
            logEntry.setStatus(ReminderStatus.SKIPPED);
            logEntry.setRecipientEmail(null);
            logEntry.setLastError("Hồ sơ không có email");
            logEntry.setSentAt(null);
            reminderLogRepository.save(logEntry);
            markEventProcessed(event);
            return;
        }

        logEntry.setRecipientEmail(recipientEmail);
        MessageTemplate template = reminderService.getTemplate(reminderType);
        String formattedTime = event.startTime() != null ? event.startTime().format(TIME_FORMATTER) : "";
        String subject = reminderService.render(template.getSubject(), patientName, formattedTime, event.appointmentId(), event.cancelReason());
        String body = reminderService.render(template.getBody(), patientName, formattedTime, event.appointmentId(), event.cancelReason());

        try {
            reminderService.sendEmail(recipientEmail, subject, body);
            logEntry.setStatus(ReminderStatus.SENT);
            logEntry.setSentAt(LocalDateTime.now());
            logEntry.setLastError(null);
            log.info("Đã gửi email {} cho lịch hẹn {} tới {}", reminderType, event.appointmentId(), recipientEmail);
        } catch (Exception ex) {
            log.warn("Gửi thư {} cho lịch {} thất bại: {}", reminderType, event.appointmentId(), ex.getMessage());
            logEntry.setStatus(ReminderStatus.FAILED);
            String reasonSuffix = event.cancelReason() != null ? " [reason:" + event.cancelReason() + "]" : "";
            logEntry.setLastError(truncate(ex.getMessage() + reasonSuffix, 1000));
        }

        reminderLogRepository.save(logEntry);
        markEventProcessed(event);
    }

    private void markEventProcessed(AppointmentEvent event) {
        ProcessedEvent processedEvent = new ProcessedEvent(event.eventId(), event.eventType().name());
        processedEventRepository.save(processedEvent);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
