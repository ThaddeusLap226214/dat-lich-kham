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

        when(reminderLogRepository.findByStatus(ReminderStatus.FAILED)).thenReturn(List.of(previouslyFailed));
        when(schedulingClient.findConfirmedBetween(any(), any())).thenReturn(Collections.emptyList());

        RunResult result = reminderService.runOnce();

        assertThat(result.sent()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(0);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        assertThat(previouslyFailed.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(previouslyFailed.getAttemptCount()).isEqualTo(2);
        assertThat(previouslyFailed.getLastError()).isNull();
    }
}
