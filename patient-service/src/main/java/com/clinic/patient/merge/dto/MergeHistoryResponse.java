package com.clinic.patient.merge.dto;

import com.clinic.patient.merge.MergeAction;
import com.clinic.patient.merge.MergeHistory;
import com.clinic.patient.patient.PatientStatus;

import java.time.LocalDateTime;

public record MergeHistoryResponse(
    Long id,
    MergeAction action,
    Long sourcePatientId,
    Long targetPatientId,
    PatientStatus sourcePreviousStatus,
    Long duplicateCandidateId,
    Long relatedMergeId,
    String eventId,
    Long performedByAccountId,
    LocalDateTime performedAt,
    String note) {

    public static MergeHistoryResponse from(MergeHistory h) {
        return new MergeHistoryResponse(h.getId(), h.getAction(), h.getSourcePatientId(), h.getTargetPatientId(),
            h.getSourcePreviousStatus(), h.getDuplicateCandidateId(), h.getRelatedMergeId(), h.getEventId(),
            h.getPerformedByAccountId(), h.getPerformedAt(), h.getNote());
    }
}
