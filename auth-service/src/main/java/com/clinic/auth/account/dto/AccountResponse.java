package com.clinic.auth.account.dto;

import com.clinic.auth.account.Account;
import com.clinic.auth.account.Role;

public record AccountResponse(
    Long id,
    String username,
    String fullName,
    String email,
    String phone,
    Role role,
    Long doctorId,
    boolean enabled) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getUsername(), a.getFullName(), a.getEmail(), a.getPhone(),
            a.getRole(), a.getDoctorId(), a.isEnabled());
    }
}
