package com.clinic.patient.event;

import java.time.LocalDateTime;

// Nội dung JSON gửi qua RabbitMQ; Scheduling Service có bản sao record này với cùng tên trường
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
