package com.clinic.gateway.composite;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Dữ liệu đọc từ API nội bộ của các service; chỉ khai báo trường cần dùng, trường thừa bị bỏ qua khi đọc JSON
public final class RemoteData {

    public record Appointment(Long id, Long patientId, Long doctorId, Long roomId, LocalDateTime startTime,
                              LocalDateTime endTime, String status, String reason) {
    }

    public record Doctor(Long id, String fullName, String title, String specialtyName) {
    }

    public record Room(Long id, String code, String name) {
    }

    public record Patient(Long id, String fullName, LocalDate dateOfBirth) {
    }

    private RemoteData() {
    }
}
