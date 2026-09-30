package com.clinic.reminder.log.dto;

import com.clinic.reminder.log.ReminderLog;
import com.clinic.reminder.log.ReminderStatus;
import com.clinic.reminder.log.ReminderType;

import java.time.LocalDateTime;

public record ReminderLogResponse(
    Long id,
    Long appointmentId,
    Long patientId,
    LocalDateTime appointmentStart,
    ReminderType reminderType,
    String recipientEmail,
    ReminderStatus status,
    int attemptCount,
    String lastError,
    LocalDateTime sentAt,
    LocalDateTime createdAt) {

    public static ReminderLogResponse from(ReminderLog l) {
        return new ReminderLogResponse(l.getId(), l.getAppointmentId(), l.getPatientId(), l.getAppointmentStart(),
            l.getReminderType(), l.getRecipientEmail(), l.getStatus(), l.getAttemptCount(), l.getLastError(),
            l.getSentAt(), l.getCreatedAt());
    }
}
