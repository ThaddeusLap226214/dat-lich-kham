package com.clinic.scheduling.event;

import java.time.LocalDateTime;

public record AppointmentEvent(
    String eventId,
    AppointmentEventType eventType,
    LocalDateTime occurredAt,
    Long appointmentId,
    Long patientId,
    Long doctorId,
    Long roomId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String cancelReason,
    Long performedByAccountId) {

    public enum AppointmentEventType {
        APPOINTMENT_CONFIRMED,
        APPOINTMENT_CANCELLED
    }
}
