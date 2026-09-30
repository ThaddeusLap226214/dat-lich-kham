package com.clinic.provider.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Kết quả API nội bộ cho Scheduling Service: các ca làm việc của một bác sĩ trong một ngày cụ thể
public record WorkingHoursResponse(Long doctorId, LocalDate date, List<Shift> shifts) {

    public record Shift(Long workScheduleId, Long roomId, LocalTime startTime, LocalTime endTime, int slotMinutes) {
    }
}
