package com.clinic.patient.duplicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuplicateCandidateRepository extends JpaRepository<DuplicateCandidate, Long> {

    @EntityGraph(attributePaths = {"patient", "candidatePatient"})
    Page<DuplicateCandidate> findByStatusOrderByCreatedAtDesc(CandidateStatus status, Pageable pageable);

    long countByStatus(CandidateStatus status);

    boolean existsByPatientIdAndCandidatePatientId(Long patientId, Long candidatePatientId);
}
