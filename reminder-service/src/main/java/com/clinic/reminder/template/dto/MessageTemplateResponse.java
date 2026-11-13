package com.clinic.reminder.template.dto;

import com.clinic.reminder.log.ReminderType;
import com.clinic.reminder.template.MessageTemplate;

import java.time.LocalDateTime;

public record MessageTemplateResponse(
    ReminderType type,
    String subject,
    String body,
    Long updatedByAccountId,
    LocalDateTime updatedAt
) {
    public static MessageTemplateResponse from(MessageTemplate t) {
        return new MessageTemplateResponse(
            t.getType(),
            t.getSubject(),
            t.getBody(),
            t.getUpdatedByAccountId(),
            t.getUpdatedAt()
        );
    }
}
