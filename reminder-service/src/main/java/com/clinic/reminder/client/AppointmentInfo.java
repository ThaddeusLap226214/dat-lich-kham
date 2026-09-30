package com.clinic.reminder.client;

import java.time.LocalDateTime;

// Các trường cần dùng từ AppointmentResponse của Scheduling Service (trường thừa bị bỏ qua khi đọc JSON)
public record AppointmentInfo(Long id, Long patientId, Long doctorId, Long roomId, LocalDateTime startTime,
                              LocalDateTime endTime, String status) {
}
