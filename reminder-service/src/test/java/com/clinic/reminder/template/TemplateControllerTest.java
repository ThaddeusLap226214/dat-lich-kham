package com.clinic.reminder.template;

import com.clinic.reminder.common.BusinessException;
import com.clinic.reminder.job.ReminderService;
import com.clinic.reminder.log.ReminderType;
import com.clinic.reminder.template.dto.MessageTemplateResponse;
import com.clinic.reminder.template.dto.UpdateTemplateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    @Mock
    private MessageTemplateRepository templateRepository;

    @Mock
    private ReminderService reminderService;

    private TemplateController controller;

    @BeforeEach
    void setUp() {
        controller = new TemplateController(templateRepository, reminderService);
    }

    @Test
    @DisplayName("P5-11: Quản trị viên xem danh sách 3 mẫu thư chuẩn")
    void listTemplates_asAdmin_returnsTemplates() {
        when(reminderService.getTemplate(ReminderType.BEFORE_APPOINTMENT))
                .thenReturn(new MessageTemplate(ReminderType.BEFORE_APPOINTMENT, "Nhắc {startTime}", "Nội dung 1"));
        when(reminderService.getTemplate(ReminderType.CONFIRMATION))
                .thenReturn(new MessageTemplate(ReminderType.CONFIRMATION, "Xác nhận {appointmentId}", "Nội dung 2"));
        when(reminderService.getTemplate(ReminderType.CANCELLATION))
                .thenReturn(new MessageTemplate(ReminderType.CANCELLATION, "Hủy {appointmentId}", "Nội dung 3"));

        List<MessageTemplateResponse> templates = controller.listTemplates("ADMIN");

        assertThat(templates).hasSize(3);
        assertThat(templates.get(0).type()).isEqualTo(ReminderType.BEFORE_APPOINTMENT);
        assertThat(templates.get(1).type()).isEqualTo(ReminderType.CONFIRMATION);
        assertThat(templates.get(2).type()).isEqualTo(ReminderType.CANCELLATION);
    }

    @Test
    @DisplayName("P5-11: Chặn vai trò không phải ADMIN xem mẫu thư (403 FORBIDDEN)")
    void listTemplates_asPatient_throwsForbidden() {
        assertThatThrownBy(() -> controller.listTemplates("PATIENT"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("P5-11: Quản lý sửa tiêu đề, nội dung mẫu thư thành công")
    void updateTemplate_asAdmin_updatesTemplate() {
        MessageTemplate existing = new MessageTemplate(ReminderType.BEFORE_APPOINTMENT, "Cũ", "Cũ");
        when(templateRepository.findById(ReminderType.BEFORE_APPOINTMENT)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any(MessageTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTemplateRequest request = new UpdateTemplateRequest("Tiêu đề mới lúc {startTime}", "Nội dung mới cho {patientName}");
        MessageTemplateResponse response = controller.updateTemplate(ReminderType.BEFORE_APPOINTMENT, request, 1L, "ADMIN");

        assertThat(response.subject()).isEqualTo("Tiêu đề mới lúc {startTime}");
        assertThat(response.body()).isEqualTo("Nội dung mới cho {patientName}");
        assertThat(response.updatedByAccountId()).isEqualTo(1L);

        ArgumentCaptor<MessageTemplate> captor = ArgumentCaptor.forClass(MessageTemplate.class);
        verify(templateRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Tiêu đề mới lúc {startTime}");
    }

    @Test
    @DisplayName("P5-11: Chặn vai trò RECEPTIONIST hoặc PATIENT sửa mẫu thư (403 FORBIDDEN)")
    void updateTemplate_asNonAdmin_throwsForbidden() {
        UpdateTemplateRequest request = new UpdateTemplateRequest("Tiêu đề", "Nội dung");

        assertThatThrownBy(() -> controller.updateTemplate(ReminderType.BEFORE_APPOINTMENT, request, 2L, "RECEPTIONIST"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("P5-11: Tiêu đề hoặc nội dung để trống -> Báo lỗi 400 INVALID_INPUT")
    void updateTemplate_blankFields_throwsBadRequest() {
        UpdateTemplateRequest request = new UpdateTemplateRequest("   ", "Nội dung");

        assertThatThrownBy(() -> controller.updateTemplate(ReminderType.BEFORE_APPOINTMENT, request, 1L, "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("INVALID_INPUT");
                });
    }
}
