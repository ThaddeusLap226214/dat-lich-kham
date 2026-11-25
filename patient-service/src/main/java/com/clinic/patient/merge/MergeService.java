package com.clinic.patient.merge;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.duplicate.CandidateStatus;
import com.clinic.patient.duplicate.DuplicateCandidate;
import com.clinic.patient.duplicate.DuplicateCandidateRepository;
import com.clinic.patient.event.PatientEvent;
import com.clinic.patient.event.PatientEventPublisher;
import com.clinic.patient.merge.dto.MergeHistoryResponse;
import com.clinic.patient.patient.Patient;
import com.clinic.patient.patient.PatientRepository;
import com.clinic.patient.patient.PatientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MergeService {

    private final PatientRepository patientRepository;
    private final DuplicateCandidateRepository duplicateCandidateRepository;
    private final MergeHistoryRepository mergeHistoryRepository;
    private final PatientEventPublisher eventPublisher;

    public MergeService(PatientRepository patientRepository,
                        DuplicateCandidateRepository duplicateCandidateRepository,
                        MergeHistoryRepository mergeHistoryRepository,
                        PatientEventPublisher eventPublisher) {
        this.patientRepository = patientRepository;
        this.duplicateCandidateRepository = duplicateCandidateRepository;
        this.mergeHistoryRepository = mergeHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MergeHistoryResponse merge(Long candidateId, Long keepPatientId, String note, Long reviewerAccountId) {
        DuplicateCandidate candidate = duplicateCandidateRepository.findById(candidateId)
            .orElseThrow(() -> BusinessException.notFound("Cặp hồ sơ nghi trùng", candidateId));

        if (candidate.getStatus() != CandidateStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "CANDIDATE_ALREADY_REVIEWED",
                "Cặp hồ sơ đã được duyệt trước đó");
        }

        Long patientId = candidate.getPatient().getId();
        Long candidatePatientId = candidate.getCandidatePatient().getId();

        if (!keepPatientId.equals(patientId) && !keepPatientId.equals(candidatePatientId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KEEP_PATIENT_NOT_IN_PAIR",
                "Hồ sơ giữ lại phải thuộc cặp nghi trùng");
        }

        Patient targetPatient = keepPatientId.equals(patientId)
            ? candidate.getPatient()
            : candidate.getCandidatePatient();
        Patient sourcePatient = keepPatientId.equals(patientId)
            ? candidate.getCandidatePatient()
            : candidate.getPatient();

        if (sourcePatient.getStatus() == PatientStatus.MERGED) {
            throw new BusinessException(HttpStatus.CONFLICT, "PATIENT_MERGED",
                "Hồ sơ đã được gộp trước đó");
        }

        PatientStatus sourcePreviousStatus = sourcePatient.getStatus();

        // 1. Cập nhật hồ sơ bị gộp: sang MERGED, trỏ merged_into_id
        sourcePatient.setStatus(PatientStatus.MERGED);
        sourcePatient.setMergedIntoId(targetPatient.getId());
        patientRepository.save(sourcePatient);

        // 2. Cập nhật cặp hàng chờ
        candidate.setStatus(CandidateStatus.MERGED);
        candidate.setReviewedByAccountId(reviewerAccountId);
        candidate.setReviewedAt(LocalDateTime.now());
        candidate.setReviewNote(note);
        duplicateCandidateRepository.save(candidate);

        // 3. Ghi lịch sử gộp
        MergeHistory history = new MergeHistory();
        history.setAction(MergeAction.MERGE);
        history.setSourcePatientId(sourcePatient.getId());
        history.setTargetPatientId(targetPatient.getId());
        history.setSourcePreviousStatus(sourcePreviousStatus);
        history.setDuplicateCandidateId(candidate.getId());
        history.setRelatedMergeId(null);
        history.setEventId(UUID.randomUUID().toString());
        history.setPerformedByAccountId(reviewerAccountId);
        history.setPerformedAt(LocalDateTime.now());
        history.setNote(note);
        MergeHistory savedHistory = mergeHistoryRepository.save(history);

        // 4. Phát sự kiện sau khi lưu DB thành công
        PatientEvent event = new PatientEvent(
            savedHistory.getEventId(),
            PatientEvent.PatientEventType.PATIENT_MERGED,
            savedHistory.getPerformedAt(),
            sourcePatient.getId(),
            targetPatient.getId(),
            savedHistory.getId(),
            null,
            reviewerAccountId
        );
        eventPublisher.publish(event);

        return MergeHistoryResponse.from(savedHistory);
    }

    @Transactional
    public MergeHistoryResponse unmerge(Long patientId, String note, Long reviewerAccountId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> BusinessException.notFound("Hồ sơ bệnh nhân", patientId));

        if (patient.getStatus() != PatientStatus.MERGED) {
            throw new BusinessException(HttpStatus.CONFLICT, "NOT_MERGED",
                "Chỉ tách hồ sơ đang ở trạng thái MERGED");
        }

        MergeHistory lastMerge = mergeHistoryRepository.findFirstBySourcePatientIdAndActionOrderByPerformedAtDesc(
            patient.getId(), MergeAction.MERGE)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MERGE_HISTORY_NOT_FOUND",
                "Không tìm thấy lịch sử gộp của hồ sơ này"));

        // 1. Khôi phục trạng thái cũ của hồ sơ
        patient.setStatus(lastMerge.getSourcePreviousStatus());
        patient.setMergedIntoId(null);
        patientRepository.save(patient);

        // 2. Cặp trong hàng chờ chuyển sang NOT_DUPLICATE (CS24)
        if (lastMerge.getDuplicateCandidateId() != null) {
            duplicateCandidateRepository.findById(lastMerge.getDuplicateCandidateId()).ifPresent(candidate -> {
                candidate.setStatus(CandidateStatus.NOT_DUPLICATE);
                candidate.setReviewNote(note);
                candidate.setReviewedAt(LocalDateTime.now());
                candidate.setReviewedByAccountId(reviewerAccountId);
                duplicateCandidateRepository.save(candidate);
            });
        }

        // 3. Ghi lịch sử tách (UNMERGE)
        MergeHistory unmergeHistory = new MergeHistory();
        unmergeHistory.setAction(MergeAction.UNMERGE);
        unmergeHistory.setSourcePatientId(patient.getId());
        unmergeHistory.setTargetPatientId(lastMerge.getTargetPatientId());
        unmergeHistory.setSourcePreviousStatus(lastMerge.getSourcePreviousStatus());
        unmergeHistory.setDuplicateCandidateId(lastMerge.getDuplicateCandidateId());
        unmergeHistory.setRelatedMergeId(lastMerge.getId());
        unmergeHistory.setEventId(UUID.randomUUID().toString());
        unmergeHistory.setPerformedByAccountId(reviewerAccountId);
        unmergeHistory.setPerformedAt(LocalDateTime.now());
        unmergeHistory.setNote(note);
        MergeHistory savedHistory = mergeHistoryRepository.save(unmergeHistory);

        // 4. Phát sự kiện sau khi lưu DB thành công
        PatientEvent event = new PatientEvent(
            savedHistory.getEventId(),
            PatientEvent.PatientEventType.PATIENT_UNMERGED,
            savedHistory.getPerformedAt(),
            patient.getId(),
            lastMerge.getTargetPatientId(),
            savedHistory.getId(),
            lastMerge.getEventId(),
            reviewerAccountId
        );
        eventPublisher.publish(event);

        return MergeHistoryResponse.from(savedHistory);
    }

    @Transactional(readOnly = true)
    public List<MergeHistoryResponse> history(Long patientId) {
        return mergeHistoryRepository.findBySourcePatientIdOrTargetPatientIdOrderByPerformedAtDesc(patientId, patientId)
            .stream()
            .map(MergeHistoryResponse::from)
            .toList();
    }
}
