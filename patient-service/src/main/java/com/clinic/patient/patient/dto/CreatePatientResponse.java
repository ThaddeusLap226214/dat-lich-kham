package com.clinic.patient.patient.dto;

public record CreatePatientResponse(PatientResponse patient, Outcome outcome, boolean duplicateQueued) {

    public enum Outcome {
        REUSED_EXISTING,
        CREATED_ACTIVE,
        CREATED_TEMPORARY
    }
}
