package com.clinic.provider.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DoctorDayOffRequest(
    @NotNull LocalDate offDate,
    @Size(max = 255) String reason) {
}

