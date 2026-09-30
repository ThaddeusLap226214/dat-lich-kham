package com.clinic.scheduling.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record HoldRequest(
    @NotNull Long doctorId,
    @NotNull Long patientId,
    @NotNull @Future LocalDateTime startTime,
    @Size(max = 500) String reason) {
}
