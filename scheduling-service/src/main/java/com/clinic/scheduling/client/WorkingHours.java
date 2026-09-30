package com.clinic.scheduling.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Dữ liệu nhận từ Provider Service: GET /internal/v1/doctors/{id}/working-hours?date=
public record WorkingHours(Long doctorId, LocalDate date, List<Shift> shifts) {

    public record Shift(Long workScheduleId, Long roomId, LocalTime startTime, LocalTime endTime, int slotMinutes) {
    }
}
