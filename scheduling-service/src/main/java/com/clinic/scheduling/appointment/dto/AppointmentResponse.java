package com.clinic.scheduling.appointment.dto;

import com.clinic.scheduling.appointment.Appointment;
import com.clinic.scheduling.appointment.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
    Long id,
    Long patientId,
    Long doctorId,
    Long roomId,
    Long workScheduleId,
    Long bookedByAccountId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    AppointmentStatus status,
    LocalDateTime holdExpiresAt,
    String reason,
    String cancelReason,
    Long cancelledByAccountId,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt,
    LocalDateTime completedAt,
    Long completedByAccountId,
    LocalDateTime noShowAt,
    Long noShowByAccountId,
    LocalDateTime createdAt) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
            a.getId(),
            a.getPatientId(),
            a.getDoctorId(),
            a.getRoomId(),
            a.getWorkScheduleId(),
            a.getBookedByAccountId(),
            a.getStartTime(),
            a.getEndTime(),
            a.getStatus(),
            a.getHoldExpiresAt(),
            a.getReason(),
            a.getCancelReason(),
            a.getCancelledByAccountId(),
            a.getConfirmedAt(),
            a.getCancelledAt(),
            a.getCompletedAt(),
            a.getCompletedByAccountId(),
            a.getNoShowAt(),
            a.getNoShowByAccountId(),
            a.getCreatedAt()
        );
    }
}
