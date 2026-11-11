package com.clinic.reminder.event;

import com.clinic.reminder.client.PatientClient;
import com.clinic.reminder.client.PatientContact;
import com.clinic.reminder.job.ReminderService;
import com.clinic.reminder.log.ReminderLog;
import com.clinic.reminder.log.ReminderLogRepository;
import com.clinic.reminder.log.ReminderStatus;
import com.clinic.reminder.log.ReminderType;
import com.clinic.reminder.template.MessageTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentEventHandlerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private ReminderLogRepository reminderLogRepository;

    @Mock
    private PatientClient patientClient;

    @Mock
    private ReminderService reminderService;

    private AppointmentEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AppointmentEventHandler(
                processedEventRepository,
                reminderLogRepository,
                patientClient,
                reminderService
        );
    }

    private AppointmentEvent createEvent(String eventId, AppointmentEvent.AppointmentEventType type, Long apptId, Long patientId, String cancelReason) {
        return new AppointmentEvent(
                eventId,
                type,
                LocalDateTime.now(),
                apptId,
                patientId,
                1L,
                1L,
                LocalDateTime.of(2026, 11, 15, 9, 0),
                LocalDateTime.of(2026, 11, 15, 9, 20),
                cancelReason,
                5L
        );
    }

    @Test
    @DisplayName("P5-8: Nhận sự kiện APPOINTMENT_CONFIRMED -> Gửi email xác nhận và lưu log CONFIRMATION")
    void handle_confirmedEvent_sendsConfirmation() {
        AppointmentEvent event = createEvent("evt-conf-1", AppointmentEvent.AppointmentEventType.APPOINTMENT_CONFIRMED, 100L, 10L, null);
        PatientContact contact = new PatientContact(10L, "Nguyễn Văn A", "nva@example.com", "0901234567", null);
        MessageTemplate template = new MessageTemplate(ReminderType.CONFIRMATION, "Xác nhận lịch {appointmentId}", "Chào {patientName}");

        when(processedEventRepository.existsById("evt-conf-1")).thenReturn(false);
        when(reminderLogRepository.findByAppointmentIdAndReminderType(100L, ReminderType.CONFIRMATION)).thenReturn(Optional.empty());
        when(patientClient.findByIds(List.of(10L))).thenReturn(List.of(contact));
        when(reminderService.getTemplate(ReminderType.CONFIRMATION)).thenReturn(template);
        when(reminderService.render(anyString(), any(), any(), any(), any())).thenCallRealMethod();

        handler.handle(event);

        verify(reminderService).sendEmail(eq("nva@example.com"), anyString(), anyString());

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        ReminderLog saved = logCaptor.getValue();
        assertThat(saved.getAppointmentId()).isEqualTo(100L);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.CONFIRMATION);
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(saved.getRecipientEmail()).isEqualTo("nva@example.com");

        ArgumentCaptor<ProcessedEvent> eventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("evt-conf-1");
    }

    @Test
    @DisplayName("P5-10: Nhận sự kiện APPOINTMENT_CANCELLED -> Gửi email báo hủy kèm lý do và lưu log CANCELLATION")
    void handle_cancelledEvent_sendsCancellationWithReason() {
        AppointmentEvent event = createEvent("evt-cancel-1", AppointmentEvent.AppointmentEventType.APPOINTMENT_CANCELLED, 101L, 11L, "Bác sĩ bận cấp cứu");
        PatientContact contact = new PatientContact(11L, "Trần Thị B", "ttb@example.com", "0912345678", null);
        MessageTemplate template = new MessageTemplate(ReminderType.CANCELLATION, "Hủy lịch {appointmentId}", "Lý do: {cancelReason}");

        when(processedEventRepository.existsById("evt-cancel-1")).thenReturn(false);
        when(reminderLogRepository.findByAppointmentIdAndReminderType(101L, ReminderType.CANCELLATION)).thenReturn(Optional.empty());
        when(patientClient.findByIds(List.of(11L))).thenReturn(List.of(contact));
        when(reminderService.getTemplate(ReminderType.CANCELLATION)).thenReturn(template);
        when(reminderService.render(anyString(), any(), any(), any(), any())).thenCallRealMethod();

        handler.handle(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(reminderService).sendEmail(eq("ttb@example.com"), anyString(), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("Bác sĩ bận cấp cứu");

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        ReminderLog saved = logCaptor.getValue();
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.CANCELLATION);
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.SENT);

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("CS36: Sự kiện nhận lặp (cùng eventId) -> Bỏ qua không gửi lại thư thứ 2")
    void handle_duplicateEvent_isIgnored() {
        AppointmentEvent event = createEvent("evt-duplicate", AppointmentEvent.AppointmentEventType.APPOINTMENT_CONFIRMED, 102L, 12L, null);

        when(processedEventRepository.existsById("evt-duplicate")).thenReturn(true);

        handler.handle(event);

        verify(reminderService, never()).sendEmail(any(), any(), any());
        verify(reminderLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("CS30: Bệnh nhân không có email -> Lưu log SKIPPED và vẫn đánh dấu đã xử lý")
    void handle_noEmail_recordsSkipped() {
        AppointmentEvent event = createEvent("evt-no-email", AppointmentEvent.AppointmentEventType.APPOINTMENT_CONFIRMED, 103L, 13L, null);
        PatientContact contact = new PatientContact(13L, "Lê Văn C", null, "0987654321", null);

        when(processedEventRepository.existsById("evt-no-email")).thenReturn(false);
        when(reminderLogRepository.findByAppointmentIdAndReminderType(103L, ReminderType.CONFIRMATION)).thenReturn(Optional.empty());
        when(patientClient.findByIds(List.of(13L))).thenReturn(List.of(contact));

        handler.handle(event);

        verify(reminderService, never()).sendEmail(any(), any(), any());

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        ReminderLog saved = logCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.SKIPPED);
        assertThat(saved.getLastError()).isEqualTo("Hồ sơ không có email");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("CS36: Gửi thư thất bại -> Lưu log FAILED để F41 gửi lại sau")
    void handle_sendMailError_recordsFailed() {
        AppointmentEvent event = createEvent("evt-mail-fail", AppointmentEvent.AppointmentEventType.APPOINTMENT_CONFIRMED, 104L, 14L, null);
        PatientContact contact = new PatientContact(14L, "Phạm D", "d@example.com", "0933333333", null);
        MessageTemplate template = new MessageTemplate(ReminderType.CONFIRMATION, "Tiêu đề", "Nội dung");

        when(processedEventRepository.existsById("evt-mail-fail")).thenReturn(false);
        when(reminderLogRepository.findByAppointmentIdAndReminderType(104L, ReminderType.CONFIRMATION)).thenReturn(Optional.empty());
        when(patientClient.findByIds(List.of(14L))).thenReturn(List.of(contact));
        when(reminderService.getTemplate(ReminderType.CONFIRMATION)).thenReturn(template);

        doThrow(new RuntimeException("Mail server down"))
                .when(reminderService).sendEmail(any(), any(), any());

        handler.handle(event);

        ArgumentCaptor<ReminderLog> logCaptor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(logCaptor.capture());
        ReminderLog saved = logCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(saved.getLastError()).contains("Mail server down");

        // Vẫn lưu processed_events để tránh tin nhắn bị redeliver lặp vô hạn
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}
