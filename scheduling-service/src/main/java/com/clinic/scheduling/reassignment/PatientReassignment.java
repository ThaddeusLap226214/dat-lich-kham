package com.clinic.scheduling.reassignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "patient_reassignments")
@Getter
@Setter
@NoArgsConstructor
public class PatientReassignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "from_patient_id", nullable = false)
    private Long fromPatientId;

    @Column(name = "to_patient_id", nullable = false)
    private Long toPatientId;

    // eventId của sự kiện PATIENT_MERGED đã gây ra lần chuyển này
    @Column(name = "merge_event_id", nullable = false, length = 36)
    private String mergeEventId;

    @CreationTimestamp
    @Column(name = "reassigned_at", nullable = false, updatable = false)
    private LocalDateTime reassignedAt;

    @Column(name = "reverted_at")
    private LocalDateTime revertedAt;
}
