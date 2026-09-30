package com.clinic.patient.patient.dto;

import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.Patient;
import com.clinic.patient.patient.PatientStatus;

import java.time.LocalDate;

// Dạng rút gọn cho API nội bộ (Gateway ghép tên bệnh nhân, Reminder lấy email)
public record PatientSummary(
    Long id,
    String fullName,
    LocalDate dateOfBirth,
    Gender gender,
    String phone,
    String email,
    String guardianPhone,
    PatientStatus status,
    Long mergedIntoId) {

    public static PatientSummary from(Patient p) {
        return new PatientSummary(p.getId(), p.getFullName(), p.getDateOfBirth(), p.getGender(), p.getPhone(),
            p.getEmail(), p.getGuardianPhone(), p.getStatus(), p.getMergedIntoId());
    }
}
