package com.clinic.patient.merge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MergeHistoryRepository extends JpaRepository<MergeHistory, Long> {

    List<MergeHistory> findBySourcePatientIdOrTargetPatientIdOrderByPerformedAtDesc(Long sourcePatientId,
                                                                                   Long targetPatientId);

    Optional<MergeHistory> findFirstBySourcePatientIdAndActionOrderByPerformedAtDesc(Long sourcePatientId,
                                                                                   MergeAction action);
}
