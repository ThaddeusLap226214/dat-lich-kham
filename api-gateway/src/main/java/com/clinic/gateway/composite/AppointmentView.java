package com.clinic.gateway.composite;

import java.time.LocalDateTime;

public record AppointmentView(
    Long appointmentId,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String reason,
    DoctorView doctor,
    RoomView room,
    PatientView patient) {

    public record DoctorView(Long id, String fullName, String title, String specialtyName) {
    }

    public record RoomView(Long id, String code, String name) {
    }

    public record PatientView(Long id, String fullName, String dateOfBirth) {
    }
}
