package com.clinic.reminder.job;

import com.clinic.reminder.client.AppointmentInfo;
import com.clinic.reminder.client.PatientClient;
import com.clinic.reminder.client.PatientContact;
import com.clinic.reminder.client.SchedulingClient;
import com.clinic.reminder.log.ReminderLog;
import com.clinic.reminder.log.ReminderLogRepository;
import com.clinic.reminder.log.ReminderStatus;
import com.clinic.reminder.log.ReminderType;
import com.clinic.reminder.log.dto.RunResult;
import com.clinic.reminder.template.MessageTemplate;
import com.clinic.reminder.template.MessageTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final SchedulingClient schedulingClient;
    private final PatientClient patientClient;
    private final ReminderLogRepository reminderLogRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final JavaMailSender mailSender;
    private final int hoursBefore;
    private final String fromAddress;

    public ReminderService(SchedulingClient schedulingClient,
                           PatientClient patientClient,
                           ReminderLogRepository reminderLogRepository,
                           MessageTemplateRepository messageTemplateRepository,
                           JavaMailSender mailSender,
                           @Value("${reminder.hours-before:24}") int hoursBefore,
                           @Value("${reminder.from-address:no-reply@datlichkham.local}") String fromAddress) {
        this.schedulingClient = schedulingClient;
        this.patientClient = patientClient;
        this.reminderLogRepository = reminderLogRepository;
        this.messageTemplateRepository = messageTemplateRepository;
        this.mailSender = mailSender;
        this.hoursBefore = hoursBefore;
        this.fromAddress = fromAddress;
    }

    @Transactional
    public RunResult runOnce() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusHours(hoursBefore);

        int checked = 0;
        int sent = 0;
        int skipped = 0;
        int failed = 0;

        // CS36: Lấy các dòng FAILED từ trước để gửi lại ở lần quét này
        List<ReminderLog> previouslyFailedLogs = reminderLogRepository.findByStatus(ReminderStatus.FAILED);
        for (ReminderLog failedLog : previouslyFailedLogs) {
            boolean success = retryFailedLog(failedLog);
            if (success) {
                sent++;
            } else {
                failed++;
            }
        }

        // F41 & CS28: Lấy lịch CONFIRMED trong 24 giờ tới (0 < start_time - now <= 24h)
        List<AppointmentInfo> appointments;
        try {
            appointments = schedulingClient.findConfirmedBetween(now, to);
        } catch (Exception e) {
            log.error("Không thể lấy danh sách lịch hẹn từ Scheduling Service: {}", e.getMessage());
            throw e;
        }

        if (appointments == null || appointments.isEmpty()) {
            return new RunResult(checked, sent, skipped, failed);
        }

        checked = appointments.size();

        // Tìm các lịch đã có log loại BEFORE_APPOINTMENT
        List<Long> apptIds = appointments.stream().map(AppointmentInfo::id).toList();
        Map<Long, ReminderLog> existingLogs = reminderLogRepository
                .findByAppointmentIdInAndReminderType(apptIds, ReminderType.BEFORE_APPOINTMENT)
                .stream()
                .collect(Collectors.toMap(ReminderLog::getAppointmentId, l -> l, (a, b) -> a));

        // Lọc bỏ các lịch đã có log SENT hoặc SKIPPED
        List<AppointmentInfo> needReminder = appointments.stream()
                .filter(a -> {
                    ReminderLog l = existingLogs.get(a.id());
                    return l == null;
                })
                .toList();

        if (needReminder.isEmpty()) {
            return new RunResult(checked, sent, skipped, failed);
        }

        // Lấy thông tin email từ Patient Service
        Set<Long> patientIds = needReminder.stream().map(AppointmentInfo::patientId).collect(Collectors.toSet());
        Map<Long, PatientContact> contactMap = getPatientContacts(patientIds);
        MessageTemplate template = getTemplate(ReminderType.BEFORE_APPOINTMENT);

        for (AppointmentInfo appt : needReminder) {
            ReminderLog logEntry = new ReminderLog();
            logEntry.setAppointmentId(appt.id());
            logEntry.setPatientId(appt.patientId());
            logEntry.setAppointmentStart(appt.startTime());
            logEntry.setReminderType(ReminderType.BEFORE_APPOINTMENT);
            logEntry.setAttemptCount(1);

            PatientContact contact = contactMap.get(appt.patientId());
            String email = contact != null ? contact.email() : null;

            // CS30: Hồ sơ không có email -> SKIPPED
            if (email == null || email.isBlank()) {
                logEntry.setStatus(ReminderStatus.SKIPPED);
                logEntry.setRecipientEmail(null);
                logEntry.setLastError("Hồ sơ không có email");
                logEntry.setSentAt(null);
                reminderLogRepository.save(logEntry);
                skipped++;
                continue;
            }

            logEntry.setRecipientEmail(email);
            String formattedTime = appt.startTime() != null ? appt.startTime().format(TIME_FORMATTER) : "";
            String patientName = contact.fullName() != null ? contact.fullName() : "Quý khách";
            String subject = render(template.getSubject(), patientName, formattedTime, appt.id(), null);
            String body = render(template.getBody(), patientName, formattedTime, appt.id(), null);

            try {
                sendEmail(email, subject, body);
                logEntry.setStatus(ReminderStatus.SENT);
                logEntry.setSentAt(LocalDateTime.now());
                logEntry.setLastError(null);
                sent++;
            } catch (Exception ex) {
                log.warn("Gửi thư nhắc cho lịch {} thất bại: {}", appt.id(), ex.getMessage());
                logEntry.setStatus(ReminderStatus.FAILED);
                logEntry.setLastError(truncate(ex.getMessage(), 1000));
                failed++;
            }

            reminderLogRepository.save(logEntry);
        }

        return new RunResult(checked, sent, skipped, failed);
    }

    private boolean retryFailedLog(ReminderLog failedLog) {
        if (failedLog.getRecipientEmail() == null || failedLog.getRecipientEmail().isBlank()) {
            failedLog.setStatus(ReminderStatus.SKIPPED);
            failedLog.setLastError("Hồ sơ không có email");
            reminderLogRepository.save(failedLog);
            return false;
        }

        failedLog.setAttemptCount(failedLog.getAttemptCount() + 1);
        try {
            MessageTemplate template = getTemplate(failedLog.getReminderType());
            String formattedTime = failedLog.getAppointmentStart() != null ? failedLog.getAppointmentStart().format(TIME_FORMATTER) : "";
            String subject = render(template.getSubject(), "Quý khách", formattedTime, failedLog.getAppointmentId(), null);
            String body = render(template.getBody(), "Quý khách", formattedTime, failedLog.getAppointmentId(), null);

            sendEmail(failedLog.getRecipientEmail(), subject, body);
            failedLog.setStatus(ReminderStatus.SENT);
            failedLog.setSentAt(LocalDateTime.now());
            failedLog.setLastError(null);
            reminderLogRepository.save(failedLog);
            return true;
        } catch (Exception e) {
            log.warn("Thử lại gửi thư cho lịch {} (lần {}) thất bại: {}",
                    failedLog.getAppointmentId(), failedLog.getAttemptCount(), e.getMessage());
            failedLog.setLastError(truncate("Lần " + failedLog.getAttemptCount() + ": " + e.getMessage(), 1000));
            reminderLogRepository.save(failedLog);
            return false;
        }
    }

    private Map<Long, PatientContact> getPatientContacts(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<PatientContact> contacts = patientClient.findByIds(patientIds);
            if (contacts == null) {
                return Collections.emptyMap();
            }
            return contacts.stream().collect(Collectors.toMap(PatientContact::id, c -> c, (a, b) -> a));
        } catch (Exception e) {
            log.error("Không thể lấy thông tin bệnh nhân từ Patient Service: {}", e.getMessage());
            throw e;
        }
    }

    public MessageTemplate getTemplate(ReminderType type) {
        return messageTemplateRepository.findById(type)
                .orElseGet(() -> defaultTemplate(type));
    }

    private MessageTemplate defaultTemplate(ReminderType type) {
        return switch (type) {
            case BEFORE_APPOINTMENT -> new MessageTemplate(
                    ReminderType.BEFORE_APPOINTMENT,
                    "Nhắc lịch khám lúc {startTime}",
                    "Kính gửi {patientName},\n\nPhòng khám nhắc bạn có lịch khám lúc {startTime} (mã lịch hẹn {appointmentId}).\nVui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân.\n\nTrân trọng."
            );
            case BEFORE_APPOINTMENT_2H -> new MessageTemplate(
                    ReminderType.BEFORE_APPOINTMENT_2H,
                    "Nhắc lịch khám sắp diễn ra lúc {startTime}",
                    "Kính gửi {patientName},\n\nLịch khám của bạn sắp diễn ra lúc {startTime} (mã lịch hẹn {appointmentId}).\nVui lòng đến phòng khám đúng giờ.\n\nTrân trọng."
            );
            case CONFIRMATION -> new MessageTemplate(
                    ReminderType.CONFIRMATION,
                    "Xác nhận lịch khám số {appointmentId}",
                    "Kính gửi {patientName},\n\nLịch khám của bạn lúc {startTime} đã được xác nhận (mã lịch hẹn {appointmentId}).\nVui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân.\n\nTrân trọng."
            );
            case CANCELLATION -> new MessageTemplate(
                    ReminderType.CANCELLATION,
                    "Lịch khám số {appointmentId} đã được hủy",
                    "Kính gửi {patientName},\n\nLịch khám lúc {startTime} (mã lịch hẹn {appointmentId}) đã được hủy.\nLý do: {cancelReason}\nNếu cần khám, vui lòng đặt lịch mới trên hệ thống.\n\nTrân trọng."
            );
        };
    }

    public String render(String templateText, String patientName, String formattedStartTime, Long appointmentId, String cancelReason) {
        if (templateText == null) {
            return "";
        }
        return templateText
                .replace("{patientName}", patientName != null ? patientName : "Quý khách")
                .replace("{startTime}", formattedStartTime != null ? formattedStartTime : "")
                .replace("{appointmentId}", appointmentId != null ? String.valueOf(appointmentId) : "")
                .replace("{cancelReason}", cancelReason != null ? cancelReason : "Không có lý do cụ thể");
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
