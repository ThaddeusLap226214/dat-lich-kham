package com.clinic.provider.schedule.dto;

import com.clinic.provider.schedule.WorkSchedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleResponse(
    Long id,
    Long doctorId,
    Long roomId,
    String roomName,
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    int slotMinutes,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    boolean active) {

    public static WorkScheduleResponse from(WorkSchedule w) {
        return new WorkScheduleResponse(w.getId(), w.getDoctor().getId(), w.getRoom().getId(), w.getRoom().getName(),
            w.getDayOfWeek(), w.getStartTime(), w.getEndTime(), w.getSlotMinutes(), w.getEffectiveFrom(),
            w.getEffectiveTo(), w.isActive());
    }
}
