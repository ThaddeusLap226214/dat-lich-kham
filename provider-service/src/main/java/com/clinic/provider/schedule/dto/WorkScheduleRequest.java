package com.clinic.provider.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record WorkScheduleRequest(
    @NotNull Long roomId,
    @NotNull @Min(1) @Max(7) Integer dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull @Min(5) @Max(120) Integer slotMinutes,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Boolean active) {
}
