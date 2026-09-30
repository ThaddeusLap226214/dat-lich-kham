package com.clinic.provider.catalog.dto;

import com.clinic.provider.catalog.Doctor;

public record DoctorResponse(
    Long id,
    String fullName,
    String title,
    Long specialtyId,
    String specialtyName,
    String phone,
    String email,
    boolean active) {

    public static DoctorResponse from(Doctor d) {
        return new DoctorResponse(d.getId(), d.getFullName(), d.getTitle(), d.getSpecialty().getId(),
            d.getSpecialty().getName(), d.getPhone(), d.getEmail(), d.isActive());
    }
}
