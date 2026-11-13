package com.clinic.reminder.log;

import com.clinic.reminder.common.BusinessException;
import com.clinic.reminder.job.ReminderService;
import com.clinic.reminder.log.dto.ReminderLogResponse;
import com.clinic.reminder.log.dto.RunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderControllerTest {

    @Mock
    private ReminderLogRepository reminderLogRepository;

    @Mock
    private ReminderService reminderService;

    private ReminderController controller;

    @BeforeEach
    void setUp() {
        controller = new ReminderController(reminderLogRepository, reminderService);
    }

    private ReminderLog sampleLog(Long id, Long appointmentId, ReminderStatus status, ReminderType type) {
        ReminderLog log = new ReminderLog();
        log.setId(id);
        log.setAppointmentId(appointmentId);
        log.setPatientId(10L);
        log.setAppointmentStart(LocalDateTime.of(2026, 10, 26, 8, 0));
        log.setReminderType(type);
        log.setRecipientEmail("patient@example.com");
        log.setStatus(status);
        log.setAttemptCount(1);
        log.setCreatedAt(LocalDateTime.of(2026, 10, 25, 8, 0));
        return log;
    }

    @Test
    @DisplayName("P5-1: Lấy 100 dòng mới nhất khi không truyền tham số lọc")
    void logs_noFilter_returnsTop100() {
        ReminderLog log1 = sampleLog(1L, 100L, ReminderStatus.SENT, ReminderType.BEFORE_APPOINTMENT);
        when(reminderLogRepository.findLogs(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(List.of(log1));

        List<ReminderLogResponse> result = controller.logs(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).status()).isEqualTo(ReminderStatus.SENT);
        assertThat(result.get(0).reminderType()).isEqualTo(ReminderType.BEFORE_APPOINTMENT);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reminderLogRepository).findLogs(eq(null), eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 100));
    }

    @Test
    @DisplayName("P5-1: Lọc theo trạng thái status")
    void logs_filterByStatus() {
        ReminderLog log = sampleLog(2L, 101L, ReminderStatus.FAILED, ReminderType.CONFIRMATION);
        when(reminderLogRepository.findLogs(eq(ReminderStatus.FAILED), eq(null), any(Pageable.class)))
                .thenReturn(List.of(log));

        List<ReminderLogResponse> result = controller.logs(ReminderStatus.FAILED, null, "ADMIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ReminderStatus.FAILED);
        verify(reminderLogRepository).findLogs(eq(ReminderStatus.FAILED), eq(null), eq(PageRequest.of(0, 100)));
    }

    @Test
    @DisplayName("P5-1: Lọc theo loại thư type")
    void logs_filterByType() {
        ReminderLog log = sampleLog(3L, 102L, ReminderStatus.SENT, ReminderType.CANCELLATION);
        when(reminderLogRepository.findLogs(eq(null), eq(ReminderType.CANCELLATION), any(Pageable.class)))
                .thenReturn(List.of(log));

        List<ReminderLogResponse> result = controller.logs(null, ReminderType.CANCELLATION, "RECEPTIONIST");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).reminderType()).isEqualTo(ReminderType.CANCELLATION);
        verify(reminderLogRepository).findLogs(eq(null), eq(ReminderType.CANCELLATION), eq(PageRequest.of(0, 100)));
    }

    @Test
    @DisplayName("P5-1: Lọc kết hợp cả status và type")
    void logs_filterByStatusAndType() {
        ReminderLog log = sampleLog(4L, 103L, ReminderStatus.SKIPPED, ReminderType.BEFORE_APPOINTMENT);
        when(reminderLogRepository.findLogs(eq(ReminderStatus.SKIPPED), eq(ReminderType.BEFORE_APPOINTMENT), any(Pageable.class)))
                .thenReturn(List.of(log));

        List<ReminderLogResponse> result = controller.logs(ReminderStatus.SKIPPED, ReminderType.BEFORE_APPOINTMENT, "ADMIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ReminderStatus.SKIPPED);
        assertThat(result.get(0).reminderType()).isEqualTo(ReminderType.BEFORE_APPOINTMENT);
    }

    @Test
    @DisplayName("P5-1: Chặn vai trò PATIENT xem nhật ký nhắc lịch (trả về 403 FORBIDDEN)")
    void logs_forbiddenForPatient() {
        assertThatThrownBy(() -> controller.logs(null, null, "PATIENT"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FORBIDDEN");
                });
    }

    @Test
    @DisplayName("P5-1: Quét ngay - cho phép vai trò ADMIN")
    void runNow_allowedForAdmin() {
        RunResult expected = new RunResult(5, 4, 1, 0);
        when(reminderService.runOnce()).thenReturn(expected);

        RunResult actual = controller.runNow("ADMIN");

        assertThat(actual).isEqualTo(expected);
        verify(reminderService).runOnce();
    }

    @Test
    @DisplayName("P5-1: Quét ngay - chặn vai trò RECEPTIONIST hoặc PATIENT (403 FORBIDDEN)")
    void runNow_forbiddenForNonAdmin() {
        assertThatThrownBy(() -> controller.runNow("RECEPTIONIST"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}
