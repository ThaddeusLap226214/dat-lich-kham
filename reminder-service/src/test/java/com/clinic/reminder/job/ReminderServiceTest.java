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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private SchedulingClient schedulingClient;

    @Mock
    private PatientClient patientClient;

    @Mock
    private ReminderLogRepository reminderLogRepository;

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private JavaMailSender mailSender;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(
                schedulingClient,
                patientClient,
                reminderLogRepository,
                messageTemplateRepository,
                mailSender,
                24,
                "no-reply@datlichkham.local"
        );
    }

    @Test
    @DisplayName("P5-2: Không có lịch hẹn nào trong 24 giờ tới -> trả về RunResult(0, 0, 0, 0)")
    void runOnce_noAppointments_returnsZero() {
        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(Collections.emptyList());
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());

        RunResult result = reminderService.runOnce();

        assertThat(result.checked()).isEqualTo(0);
        assertThat(result.sent()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(0);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("P5-2: Lịch hẹn có email -> gửi thư nhắc thành công và lưu log SENT")
    void runOnce_validAppointment_sendsEmailAndRecordsSent() {
        LocalDateTime start = LocalDateTime.now().plusHours(12);
        AppointmentInfo appt = new AppointmentInfo(10L, 1L, 2L, 3L, start, start.plusMinutes(20), "CONFIRMED");
        PatientContact contact = new PatientContact(1L, "Lê Thị Hoa", "hoa.le@example.com", "0901234567", null);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(Collections.emptyList());
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(List.of(appt));
        when(reminderLogRepository.findByAppointmentIdInAndReminderType(List.of(10L), ReminderType.BEFORE_APPOINTMENT))
                .thenReturn(Collections.emptyList());
        when(patientClient.findByIds(any())).thenReturn(List.of(contact));
        when(messageTemplateRepository.findById(ReminderType.BEFORE_APPOINTMENT)).thenReturn(Optional.of(
                new MessageTemplate(ReminderType.BEFORE_APPOINTMENT, "Nhắc lịch {startTime}", "Chào {patientName}, mã {appointmentId}")
        ));

        RunResult result = reminderService.runOnce();

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(0);

        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msgCaptor.capture());
        SimpleMailMessage sentMsg = msgCaptor.getValue();
        assertThat(sentMsg.getTo()).containsExactly("hoa.le@example.com");
        assertThat(sentMsg.getFrom()).isEqualTo("no-reply@datlichkham.local");
        assertThat(sentMsg.getText()).contains("Lê Thị Hoa").contains("10");

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        ReminderLog saved = logCaptor.getValue();
        assertThat(saved.getAppointmentId()).isEqualTo(10L);
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("P5-2: Bệnh nhân không có email -> ghi log SKIPPED theo CS30")
    void runOnce_noEmail_recordsSkipped() {
        LocalDateTime start = LocalDateTime.now().plusHours(5);
        AppointmentInfo appt = new AppointmentInfo(11L, 2L, 2L, 3L, start, start.plusMinutes(20), "CONFIRMED");
        PatientContact contact = new PatientContact(2L, "Trần Văn Nam", null, "0909999999", null);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(Collections.emptyList());
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(List.of(appt));
        when(reminderLogRepository.findByAppointmentIdInAndReminderType(List.of(11L), ReminderType.BEFORE_APPOINTMENT))
                .thenReturn(Collections.emptyList());
        when(patientClient.findByIds(any())).thenReturn(List.of(contact));

        RunResult result = reminderService.runOnce();

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.sent()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(0);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        ReminderLog saved = logCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.SKIPPED);
        assertThat(saved.getLastError()).isEqualTo("Hồ sơ không có email");
    }

    @Test
    @DisplayName("P5-2: Lịch đã được nhắc trước đó -> bỏ qua không gửi lặp (chạy lần 2 sent = 0)")
    void runOnce_alreadySent_doesNotSendAgain() {
        LocalDateTime start = LocalDateTime.now().plusHours(8);
        AppointmentInfo appt = new AppointmentInfo(12L, 1L, 2L, 3L, start, start.plusMinutes(20), "CONFIRMED");

        ReminderLog existing = new ReminderLog();
        existing.setAppointmentId(12L);
        existing.setStatus(ReminderStatus.SENT);
        existing.setReminderType(ReminderType.BEFORE_APPOINTMENT);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(Collections.emptyList());
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(List.of(appt));
        when(reminderLogRepository.findByAppointmentIdInAndReminderType(List.of(12L), ReminderType.BEFORE_APPOINTMENT))
                .thenReturn(List.of(existing));

        RunResult result = reminderService.runOnce();

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.sent()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(0);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("P5-2: Mail server lỗi -> ghi log FAILED và tăng attemptCount")
    void runOnce_mailError_recordsFailed() {
        LocalDateTime start = LocalDateTime.now().plusHours(10);
        AppointmentInfo appt = new AppointmentInfo(13L, 1L, 2L, 3L, start, start.plusMinutes(20), "CONFIRMED");
        PatientContact contact = new PatientContact(1L, "Nguyễn An", "an@example.com", "0912345678", null);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(Collections.emptyList());
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(List.of(appt));
        when(reminderLogRepository.findByAppointmentIdInAndReminderType(List.of(13L), ReminderType.BEFORE_APPOINTMENT))
                .thenReturn(Collections.emptyList());
        when(patientClient.findByIds(any())).thenReturn(List.of(contact));

        doThrow(new MailSendException("Connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        RunResult result = reminderService.runOnce();

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.sent()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(1);

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        ReminderLog saved = logCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(saved.getLastError()).contains("Connection refused");
    }

    @Test
    @DisplayName("P5-2: Gửi lại các dòng FAILED từ trước theo CS36")
    void runOnce_retriesPreviouslyFailedLogs() {
        ReminderLog previouslyFailed = new ReminderLog();
        previouslyFailed.setId(99L);
        previouslyFailed.setAppointmentId(20L);
        previouslyFailed.setPatientId(1L);
        previouslyFailed.setAppointmentStart(LocalDateTime.now().plusHours(4));
        previouslyFailed.setReminderType(ReminderType.BEFORE_APPOINTMENT);
        previouslyFailed.setRecipientEmail("retry@example.com");
        previouslyFailed.setStatus(ReminderStatus.FAILED);
        previouslyFailed.setAttemptCount(1);

        LocalDateTime start = previouslyFailed.getAppointmentStart();
        AppointmentInfo appt = new AppointmentInfo(20L, 1L, 2L, 3L, start, start.plusMinutes(20), "CONFIRMED");

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(previouslyFailed));
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(List.of(appt));
        when(reminderLogRepository.findByAppointmentIdInAndReminderType(List.of(20L), ReminderType.BEFORE_APPOINTMENT))
                .thenReturn(List.of(previouslyFailed));

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(0);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        assertThat(previouslyFailed.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(previouslyFailed.getAttemptCount()).isEqualTo(2);
        assertThat(previouslyFailed.getLastError()).isNull();
    }

    @Test
    @DisplayName("Review 3.1: Không thử lại khi đã vượt quá 3 lần gửi (attemptCount >= 3)")
    void runOnce_maxAttemptsExceeded_doesNotRetry() {
        ReminderLog failedMax = new ReminderLog();
        failedMax.setId(101L);
        failedMax.setAppointmentId(21L);
        failedMax.setAppointmentStart(LocalDateTime.now().plusHours(2));
        failedMax.setReminderType(ReminderType.BEFORE_APPOINTMENT);
        failedMax.setRecipientEmail("retry@example.com");
        failedMax.setStatus(ReminderStatus.FAILED);
        failedMax.setAttemptCount(3);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(failedMax));
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(0);
        // Dòng đã hết lượt thử không được đếm lại vào failed ở mỗi lần quét
        assertThat(result.failed()).isEqualTo(0);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Review 3.1: Bỏ qua không gửi lại nếu lịch hẹn đã qua giờ khám")
    void runOnce_pastAppointment_doesNotRetry() {
        ReminderLog pastAppt = new ReminderLog();
        pastAppt.setId(102L);
        pastAppt.setAppointmentId(22L);
        pastAppt.setAppointmentStart(LocalDateTime.now().minusMinutes(10));
        pastAppt.setReminderType(ReminderType.BEFORE_APPOINTMENT);
        pastAppt.setRecipientEmail("retry@example.com");
        pastAppt.setStatus(ReminderStatus.FAILED);
        pastAppt.setAttemptCount(1);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(pastAppt));
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(0);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Review 2.2: Thử lại thành công khi trước đó thiếu email do Patient Service sập")
    void runOnce_retryReFetchesMissingEmail() {
        ReminderLog noEmailFailed = new ReminderLog();
        noEmailFailed.setId(103L);
        noEmailFailed.setAppointmentId(23L);
        noEmailFailed.setPatientId(99L);
        noEmailFailed.setAppointmentStart(LocalDateTime.now().plusHours(3));
        noEmailFailed.setReminderType(ReminderType.CONFIRMATION);
        noEmailFailed.setRecipientEmail(null);
        noEmailFailed.setStatus(ReminderStatus.FAILED);
        noEmailFailed.setAttemptCount(1);

        PatientContact recoveredContact = new PatientContact(99L, "Trần Văn E", "e@example.com", "0911223344", null);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(noEmailFailed));
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());
        when(patientClient.findByIds(List.of(99L))).thenReturn(List.of(recoveredContact));
        when(messageTemplateRepository.findById(ReminderType.CONFIRMATION)).thenReturn(Optional.of(
                new MessageTemplate(ReminderType.CONFIRMATION, "Xác nhận {appointmentId}", "Kính gửi {patientName}")
        ));

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(1);
        assertThat(noEmailFailed.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(noEmailFailed.getRecipientEmail()).isEqualTo("e@example.com");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("P5-3: Khả năng phục hồi - Sau khi start lại, lần quét kế tiếp quét được lịch vừa đặt trong 24h và gửi nhắc")
    void runOnce_afterRestart_scansAndRemindsNewlyBookedAppointment() {
        LocalDateTime start = LocalDateTime.now().plusHours(18);
        AppointmentInfo appt = new AppointmentInfo(50L, 1L, 2L, 3L, start, start.plusMinutes(20), "CONFIRMED");
        PatientContact contact = new PatientContact(1L, "Nguyễn Văn F", "f@example.com", "0905555555", null);

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(Collections.emptyList());
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(List.of(appt));
        when(reminderLogRepository.findByAppointmentIdInAndReminderType(List.of(50L), ReminderType.BEFORE_APPOINTMENT))
                .thenReturn(Collections.emptyList());
        when(patientClient.findByIds(any())).thenReturn(List.of(contact));
        when(messageTemplateRepository.findById(ReminderType.BEFORE_APPOINTMENT)).thenReturn(Optional.of(
                new MessageTemplate(ReminderType.BEFORE_APPOINTMENT, "Nhắc lịch {startTime}", "Chào {patientName}")
        ));

        RunResult result = reminderService.runOnce();

        assertThat(result.checked()).isEqualTo(1);
        assertThat(result.sent()).isEqualTo(1);
        verify(mailSender).send(any(SimpleMailMessage.class));

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(logCaptor.getValue().getAppointmentId()).isEqualTo(50L);
    }

    private ReminderLog failedLog(long appointmentId, ReminderType type, String email, String lastError) {
        ReminderLog l = new ReminderLog();
        l.setId(appointmentId + 1000);
        l.setAppointmentId(appointmentId);
        l.setPatientId(7L);
        l.setAppointmentStart(LocalDateTime.now().plusHours(6));
        l.setReminderType(type);
        l.setRecipientEmail(email);
        l.setStatus(ReminderStatus.FAILED);
        l.setAttemptCount(1);
        l.setLastError(lastError);
        return l;
    }

    @Test
    @DisplayName("Thử lại thư hủy khi Patient vẫn tắt -> giữ lý do hủy trong lastError cho lần sau")
    void retry_patientStillDown_keepsCancelReason() {
        ReminderLog log = failedLog(30L, ReminderType.CANCELLATION, null,
                "Patient Service không phản hồi: timeout [reason:Bác sĩ nghỉ đột xuất]");

        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());
        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(log));
        when(patientClient.findByIds(List.of(7L))).thenThrow(new RuntimeException("timeout"));

        RunResult result = reminderService.runOnce();

        assertThat(result.failed()).isEqualTo(1);
        assertThat(log.getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(log.getAttemptCount()).isEqualTo(2);
        assertThat(log.getLastError()).contains("[reason:Bác sĩ nghỉ đột xuất]");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Thử lại thư hủy đã có email -> thư dùng tên thật và lý do hủy")
    void retry_emailKnown_usesPatientNameAndReason() {
        ReminderLog log = failedLog(31L, ReminderType.CANCELLATION, "lan@example.com",
                "Connection refused [reason:Bác sĩ nghỉ đột xuất]");
        PatientContact contact = new PatientContact(7L, "Phạm Thị Lan", "lan@example.com", "0905000000", null);

        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());
        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(log));
        when(patientClient.findByIds(List.of(7L))).thenReturn(List.of(contact));

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(1);
        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msg.capture());
        assertThat(msg.getValue().getText()).contains("Phạm Thị Lan").contains("Bác sĩ nghỉ đột xuất");
    }

    @Test
    @DisplayName("Thử lại khi đã có email nhưng Patient tắt -> vẫn gửi, xưng Quý khách")
    void retry_emailKnown_patientDown_stillSends() {
        ReminderLog log = failedLog(32L, ReminderType.CONFIRMATION, "lan@example.com", "Connection refused");

        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());
        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(log));
        when(patientClient.findByIds(List.of(7L))).thenThrow(new RuntimeException("timeout"));

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(1);
        assertThat(log.getStatus()).isEqualTo(ReminderStatus.SENT);
        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msg.capture());
        assertThat(msg.getValue().getText()).contains("Quý khách");
    }

    @Test
    @DisplayName("Thư nhắc lỗi của lịch không còn CONFIRMED (đã hủy) -> không gửi lại, không đếm failed")
    void retry_reminderForCancelledAppointment_notSent() {
        ReminderLog log = failedLog(33L, ReminderType.BEFORE_APPOINTMENT, "lan@example.com", "Connection refused");

        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());
        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(log));

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(0);
        assertThat(log.getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(log.getAttemptCount()).isEqualTo(1);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Scheduling tắt -> hoãn gửi lại thư nhắc, thư xác nhận vẫn được gửi lại")
    void retry_schedulingDown_skipsReminderButRetriesConfirmation() {
        ReminderLog reminder = failedLog(34L, ReminderType.BEFORE_APPOINTMENT, "lan@example.com", "Connection refused");
        ReminderLog confirmation = failedLog(35L, ReminderType.CONFIRMATION, "lan@example.com", "Connection refused");

        when(schedulingClient.findConfirmedBetween(any(), any())).thenThrow(new RuntimeException("Scheduling down"));
        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(reminder, confirmation));

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(0);
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(confirmation.getStatus()).isEqualTo(ReminderStatus.SENT);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
