package com.clinic.patient.duplicate.dto;

import com.clinic.patient.duplicate.CandidateStatus;
import com.clinic.patient.duplicate.DuplicateCandidate;
import com.clinic.patient.duplicate.DuplicateReason;
import com.clinic.patient.patient.dto.PatientResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DuplicateCandidateResponse(
    Long id,
    DuplicateReason reason,
    BigDecimal score,
    CandidateStatus status,
    PatientResponse patient,
    PatientResponse candidatePatient,
    Long reviewedByAccountId,
    LocalDateTime reviewedAt,
    String reviewNote,
    LocalDateTime createdAt) {

    public static DuplicateCandidateResponse from(DuplicateCandidate c) {
        return new DuplicateCandidateResponse(c.getId(), c.getReason(), c.getScore(), c.getStatus(),
            PatientResponse.from(c.getPatient()), PatientResponse.from(c.getCandidatePatient()),
            c.getReviewedByAccountId(), c.getReviewedAt(), c.getReviewNote(), c.getCreatedAt());
    }
}
