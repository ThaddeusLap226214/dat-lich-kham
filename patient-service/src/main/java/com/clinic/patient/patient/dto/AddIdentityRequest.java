package com.clinic.patient.patient.dto;

import com.clinic.patient.patient.IdType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddIdentityRequest(@NotNull IdType idType, @NotBlank @Size(max = 20) String idNumber) {
}
