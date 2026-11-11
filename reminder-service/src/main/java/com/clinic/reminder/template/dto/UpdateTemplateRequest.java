package com.clinic.reminder.template.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTemplateRequest(
    @NotBlank(message = "Tiêu đề không được để trống")
    String subject,

    @NotBlank(message = "Nội dung không được để trống")
    String body
) {
}
