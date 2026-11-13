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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final Pattern REASON_PATTERN = Pattern.compile("\\[reason:(.*?)\\]");

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

    // Fix lỗi 2.3: Bỏ @Transactional để mỗi dòng nhật ký commit ngay, tránh rollback làm gửi trùng thư
    public RunResult runOnce() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusHours(hoursBefore);

        int checked = 0;
        int sent = 0;
        int skipped = 0;
        int failed = 0;

        // F41 & CS28: Lấy lịch CONFIRMED trong 24 giờ tới (0 < start_time - now <= 24h)
        List<AppointmentInfo> appointments = null;
        boolean schedulingDown = false;
        try {
            appointments = schedulingClient.findConfirmedBetween(now, to);
        } catch (Exception e) {
            log.warn("Không thể lấy danh sách lịch hẹn từ Scheduling Service (service có thể đang tắt): {}", e.getMessage());
            schedulingDown = true;
        }
        // null = không biết lịch nào còn CONFIRMED (Scheduling lỗi)
        Set<Long> confirmedIds = schedulingDown ? null
                : appointments == null ? Set.of()
                : appointments.stream().map(AppointmentInfo::id).collect(Collectors.toSet());

        // CS36: Thử lại các dòng FAILED từ trước
        List<ReminderLog> previouslyFailedLogs = reminderLogRepository.findByStatus(ReminderStatus.FAILED);
        for (ReminderLog failedLog : previouslyFailedLogs) {
            switch (retryFailedLog(failedLog, confirmedIds)) {
                case SENT -> sent++;
                case FAILED -> failed++;
                case SKIPPED -> skipped++;
                case NOT_RETRIED -> { }
            }
        }

        if (schedulingDown) {
            return new RunResult(checked, sent, skipped, failed);
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

        // Lọc bỏ các lịch đã có log
        List<AppointmentInfo> needReminder = appointments.stream()
                .filter(a -> existingLogs.get(a.id()) == null)
                .toList();

        if (needReminder.isEmpty()) {
            return new RunResult(checked, sent, skipped, failed);
        }

        // Lấy thông tin email từ Patient Service
        Set<Long> patientIds = needReminder.stream().map(AppointmentInfo::patientId).collect(Collectors.toSet());
        Map<Long, PatientContact> contactMap;
        try {
            contactMap = getPatientContacts(patientIds);
        } catch (Exception e) {
            log.warn("Không thể lấy thông tin bệnh nhân từ Patient Service: {}", e.getMessage());
            // Fix lỗi 2.2: Patient tắt thì lưu FAILED để cron lần sau thử lại
            for (AppointmentInfo appt : needReminder) {
                ReminderLog logEntry = new ReminderLog();
                logEntry.setAppointmentId(appt.id());
                logEntry.setPatientId(appt.patientId());
                logEntry.setAppointmentStart(appt.startTime());
                logEntry.setReminderType(ReminderType.BEFORE_APPOINTMENT);
                logEntry.setAttemptCount(1);
                logEntry.setStatus(ReminderStatus.FAILED);
                logEntry.setLastError(truncate("Patient Service không phản hồi: " + e.getMessage(), 1000));
                reminderLogRepository.save(logEntry);
                failed++;
            }
            return new RunResult(checked, sent, skipped, failed);
        }

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
            String patientName = (contact != null && contact.fullName() != null && !contact.fullName().isBlank())
                    ? contact.fullName() : "Quý khách";
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

    private enum RetryOutcome { SENT, FAILED, SKIPPED, NOT_RETRIED }

    private static final int MAX_ATTEMPTS = 3;

    // confirmedIds: lịch còn CONFIRMED trong 24 giờ tới; null nếu Scheduling lỗi
    private RetryOutcome retryFailedLog(ReminderLog failedLog, Set<Long> confirmedIds) {
        if (failedLog.getAttemptCount() >= MAX_ATTEMPTS) {
            return RetryOutcome.NOT_RETRIED;
        }

        if (failedLog.getAppointmentStart() != null && failedLog.getAppointmentStart().isBefore(LocalDateTime.now())) {
            log.info("Lịch hẹn {} đã qua giờ khám, không gửi nhắc lại", failedLog.getAppointmentId());
            return RetryOutcome.NOT_RETRIED;
        }

        if (isBeforeAppointment(failedLog.getReminderType())) {
            if (confirmedIds == null) {
                return RetryOutcome.NOT_RETRIED;
            }
            if (!confirmedIds.contains(failedLog.getAppointmentId())) {
                log.info("Lịch hẹn {} không còn CONFIRMED, không gửi nhắc lại", failedLog.getAppointmentId());
                return RetryOutcome.NOT_RETRIED;
            }
        }

        // Đọc lý do hủy trước khi lastError bị ghi đè ở các nhánh lỗi bên dưới
        String cancelReason = extractCancelReason(failedLog.getLastError());
        String reasonSuffix = cancelReason != null ? " [reason:" + cancelReason + "]" : "";

        PatientContact contact = null;
        try {
            List<PatientContact> contacts = patientClient.findByIds(List.of(failedLog.getPatientId()));
            if (contacts != null && !contacts.isEmpty()) {
                contact = contacts.get(0);
            }
        } catch (Exception e) {
            if (failedLog.getRecipientEmail() == null || failedLog.getRecipientEmail().isBlank()) {
                failedLog.setAttemptCount(failedLog.getAttemptCount() + 1);
                failedLog.setLastError(truncate("Lần " + failedLog.getAttemptCount()
                        + ": Patient Service không phản hồi" + reasonSuffix, 1000));
                reminderLogRepository.save(failedLog);
                return RetryOutcome.FAILED;
            }
            // Đã có email từ lần trước: vẫn gửi, chỉ thiếu tên
            log.warn("Không lấy được tên bệnh nhân {} khi gửi lại: {}", failedLog.getPatientId(), e.getMessage());
        }

        if (contact != null && contact.email() != null && !contact.email().isBlank()) {
            failedLog.setRecipientEmail(contact.email());
        }
        if (failedLog.getRecipientEmail() == null || failedLog.getRecipientEmail().isBlank()) {
            failedLog.setStatus(ReminderStatus.SKIPPED);
            failedLog.setLastError("Hồ sơ không có email");
            reminderLogRepository.save(failedLog);
            return RetryOutcome.SKIPPED;
        }
        String patientName = (contact != null && contact.fullName() != null && !contact.fullName().isBlank())
                ? contact.fullName() : "Quý khách";

        failedLog.setAttemptCount(failedLog.getAttemptCount() + 1);
        try {
            MessageTemplate template = getTemplate(failedLog.getReminderType());
            String formattedTime = failedLog.getAppointmentStart() != null ? failedLog.getAppointmentStart().format(TIME_FORMATTER) : "";
            String subject = render(template.getSubject(), patientName, formattedTime, failedLog.getAppointmentId(), cancelReason);
            String body = render(template.getBody(), patientName, formattedTime, failedLog.getAppointmentId(), cancelReason);

            sendEmail(failedLog.getRecipientEmail(), subject, body);
            failedLog.setStatus(ReminderStatus.SENT);
            failedLog.setSentAt(LocalDateTime.now());
            failedLog.setLastError(null);
            reminderLogRepository.save(failedLog);
            return RetryOutcome.SENT;
        } catch (Exception e) {
            log.warn("Thử lại gửi thư cho lịch {} (lần {}) thất bại: {}",
                    failedLog.getAppointmentId(), failedLog.getAttemptCount(), e.getMessage());
            failedLog.setLastError(truncate("Lần " + failedLog.getAttemptCount() + ": " + e.getMessage() + reasonSuffix, 1000));
            reminderLogRepository.save(failedLog);
            return RetryOutcome.FAILED;
        }
    }

    private boolean isBeforeAppointment(ReminderType type) {
        return type == ReminderType.BEFORE_APPOINTMENT || type == ReminderType.BEFORE_APPOINTMENT_2H;
    }

    private String extractCancelReason(String lastError) {
        if (lastError == null) {
            return null;
        }
        Matcher matcher = REASON_PATTERN.matcher(lastError);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Map<Long, PatientContact> getPatientContacts(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PatientContact> contacts = patientClient.findByIds(patientIds);
        if (contacts == null) {
            return Collections.emptyMap();
        }
        return contacts.stream().collect(Collectors.toMap(PatientContact::id, c -> c, (a, b) -> a));
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
