package com.clinic.provider.catalog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DoctorRequest(
    @NotBlank @Size(max = 255) String fullName,
    @Size(max = 50) String title,
    @NotNull Long specialtyId,
    @Pattern(regexp = "^0\\d{9}$", message = "phải gồm 10 chữ số, bắt đầu bằng 0") String phone,
    @Email String email,
    Boolean active) {
}
