package com.clinic.scheduling.reassignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientReassignmentRepository extends JpaRepository<PatientReassignment, Long> {

    List<PatientReassignment> findByMergeEventIdAndRevertedAtIsNull(String mergeEventId);
}
