package com.clinic.patient.patient.dto;

import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.IdType;
import com.clinic.patient.patient.Patient;
import com.clinic.patient.patient.PatientStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
    Long id,
    Long ownerAccountId,
    String fullName,
    LocalDate dateOfBirth,
    Gender gender,
    String phone,
    String email,
    String address,
    IdType idType,
    String idNumber,
    String guardianName,
    String guardianPhone,
    String guardianRelation,
    PatientStatus status,
    Long mergedIntoId,
    LocalDateTime createdAt) {

    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.getId(), p.getOwnerAccountId(), p.getFullName(), p.getDateOfBirth(),
            p.getGender(), p.getPhone(), p.getEmail(), p.getAddress(), p.getIdType(), p.getIdNumber(),
            p.getGuardianName(), p.getGuardianPhone(), p.getGuardianRelation(), p.getStatus(), p.getMergedIntoId(),
            p.getCreatedAt());
    }
}
