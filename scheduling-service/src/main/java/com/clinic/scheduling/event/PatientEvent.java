package com.clinic.scheduling.event;

import java.time.LocalDateTime;

// Bản sao hợp đồng sự kiện của Patient Service (cùng tên trường); không dùng chung code giữa hai service
public record PatientEvent(
    String eventId,
    PatientEventType eventType,
    LocalDateTime occurredAt,
    Long sourcePatientId,
    Long targetPatientId,
    Long mergeHistoryId,
    String relatedMergeEventId,
    Long performedByAccountId) {

    public enum PatientEventType {
        PATIENT_MERGED,
        PATIENT_UNMERGED
    }
}
