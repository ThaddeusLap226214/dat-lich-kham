package com.clinic.reminder.template;

import com.clinic.reminder.common.BusinessException;
import com.clinic.reminder.job.ReminderService;
import com.clinic.reminder.log.ReminderType;
import com.clinic.reminder.template.dto.MessageTemplateResponse;
import com.clinic.reminder.template.dto.UpdateTemplateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reminders/templates")
public class TemplateController {

    private final MessageTemplateRepository templateRepository;
    private final ReminderService reminderService;

    public TemplateController(MessageTemplateRepository templateRepository, ReminderService reminderService) {
        this.templateRepository = templateRepository;
        this.reminderService = reminderService;
    }

    @GetMapping
    public List<MessageTemplateResponse> listTemplates(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        checkAdminRole(role);

        // Lấy 3 loại mẫu chuẩn: BEFORE_APPOINTMENT, CONFIRMATION, CANCELLATION
        List<ReminderType> coreTypes = List.of(
                ReminderType.BEFORE_APPOINTMENT,
                ReminderType.CONFIRMATION,
                ReminderType.CANCELLATION
        );

        return coreTypes.stream()
                .map(reminderService::getTemplate)
                .map(MessageTemplateResponse::from)
                .toList();
    }

    @PutMapping("/{type}")
    public MessageTemplateResponse updateTemplate(
            @PathVariable ReminderType type,
            @Valid @RequestBody UpdateTemplateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long accountId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        checkAdminRole(role);

        if (request.subject() == null || request.subject().isBlank() ||
            request.body() == null || request.body().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Tiêu đề và nội dung không được để trống");
        }

        MessageTemplate template = templateRepository.findById(type)
                .orElseGet(() -> reminderService.getTemplate(type));

        template.setSubject(request.subject().trim());
        template.setBody(request.body().trim());
        template.setUpdatedByAccountId(accountId);

        MessageTemplate saved = templateRepository.save(template);
        return MessageTemplateResponse.from(saved);
    }

    private void checkAdminRole(String role) {
        if (role != null && !"ADMIN".equals(role)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Chỉ quản trị viên mới có quyền quản lý mẫu thư");
        }
    }
}
