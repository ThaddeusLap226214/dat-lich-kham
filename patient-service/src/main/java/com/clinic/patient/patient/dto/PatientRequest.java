package com.clinic.patient.patient.dto;

import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.IdType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRequest(
    @NotBlank @Size(max = 255) String fullName,
    @NotNull @Past LocalDate dateOfBirth,
    @NotNull Gender gender,
    @Pattern(regexp = "^0\\d{9}$", message = "phải gồm 10 chữ số, bắt đầu bằng 0") String phone,
    @Email String email,
    @Size(max = 500) String address,
    IdType idType,
    @Size(max = 20) String idNumber,
    @Size(max = 255) String guardianName,
    @Pattern(regexp = "^0\\d{9}$", message = "phải gồm 10 chữ số, bắt đầu bằng 0") String guardianPhone,
    @Size(max = 50) String guardianRelation) {
}
