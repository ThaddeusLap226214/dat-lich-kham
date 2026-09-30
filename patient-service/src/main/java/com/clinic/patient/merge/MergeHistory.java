package com.clinic.patient.merge;

import com.clinic.patient.patient.PatientStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "merge_history")
@Getter
@Setter
@NoArgsConstructor
public class MergeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MergeAction action;

    // Hồ sơ bị gộp (B); sau khi gộp B có trạng thái MERGED
    @Column(name = "source_patient_id", nullable = false)
    private Long sourcePatientId;

    // Hồ sơ được giữ lại (A)
    @Column(name = "target_patient_id", nullable = false)
    private Long targetPatientId;

    // Trạng thái của B trước khi gộp, để khi tách trả B về đúng trạng thái cũ
    @Enumerated(EnumType.STRING)
    @Column(name = "source_previous_status", nullable = false, length = 10)
    private PatientStatus sourcePreviousStatus;

    @Column(name = "duplicate_candidate_id")
    private Long duplicateCandidateId;

    // Với UNMERGE: trỏ về dòng MERGE mà lần tách này hoàn tác
    @Column(name = "related_merge_id")
    private Long relatedMergeId;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "performed_by_account_id", nullable = false)
    private Long performedByAccountId;

    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private LocalDateTime performedAt;

    @Column(length = 500)
    private String note;
}
