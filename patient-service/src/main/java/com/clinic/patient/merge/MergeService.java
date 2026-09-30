package com.clinic.patient.merge;

import com.clinic.patient.common.NotImplementedYetException;
import com.clinic.patient.duplicate.DuplicateCandidateRepository;
import com.clinic.patient.event.PatientEventPublisher;
import com.clinic.patient.merge.dto.MergeHistoryResponse;
import com.clinic.patient.patient.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MergeService {

    private final PatientRepository patientRepository;
    private final DuplicateCandidateRepository duplicateCandidateRepository;
    private final MergeHistoryRepository mergeHistoryRepository;
    private final PatientEventPublisher eventPublisher;

    public MergeService(PatientRepository patientRepository, DuplicateCandidateRepository duplicateCandidateRepository,
                        MergeHistoryRepository mergeHistoryRepository, PatientEventPublisher eventPublisher) {
        this.patientRepository = patientRepository;
        this.duplicateCandidateRepository = duplicateCandidateRepository;
        this.mergeHistoryRepository = mergeHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    public MergeHistoryResponse merge(Long candidateId, Long keepPatientId, String note, Long reviewerAccountId) {
        throw new NotImplementedYetException("gộp: hồ sơ bị gộp → MERGED, ghi merge_history, phát sự kiện PATIENT_MERGED");
    }

    public MergeHistoryResponse unmerge(Long patientId, String note, Long reviewerAccountId) {
        throw new NotImplementedYetException("tách: trả hồ sơ về trạng thái cũ, ghi merge_history, phát sự kiện PATIENT_UNMERGED");
    }

    public List<MergeHistoryResponse> history(Long patientId) {
        throw new NotImplementedYetException("lịch sử gộp / tách liên quan tới hồ sơ");
    }
}
