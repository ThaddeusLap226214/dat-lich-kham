package com.clinic.auth.account.dto;

import com.clinic.auth.account.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
    @NotBlank @Size(min = 4, max = 50) String username,
    @NotBlank @Size(min = 6, max = 72) String password,
    @NotBlank @Size(max = 255) String fullName,
    @Email String email,
    @Pattern(regexp = "^0\\d{9}$", message = "phải gồm 10 chữ số, bắt đầu bằng 0") String phone,
    @NotNull Role role,
    Long doctorId) {
}
