package com.clinic.patient.duplicate;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.common.PageResponse;
import com.clinic.patient.duplicate.dto.DuplicateCandidateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DuplicateReviewService {

    private final DuplicateCandidateRepository duplicateCandidateRepository;

    public DuplicateReviewService(DuplicateCandidateRepository duplicateCandidateRepository) {
        this.duplicateCandidateRepository = duplicateCandidateRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<DuplicateCandidateResponse> list(CandidateStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        Page<DuplicateCandidate> pageResult = (status != null)
            ? duplicateCandidateRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            : duplicateCandidateRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.of(pageResult, DuplicateCandidateResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(CandidateStatus status) {
        CandidateStatus targetStatus = status != null ? status : CandidateStatus.PENDING;
        return duplicateCandidateRepository.countByStatus(targetStatus);
    }

    @Transactional
    public DuplicateCandidateResponse reject(Long candidateId, String note, Long reviewerAccountId) {
        DuplicateCandidate candidate = duplicateCandidateRepository.findById(candidateId)
            .orElseThrow(() -> BusinessException.notFound("Cặp hồ sơ nghi trùng", candidateId));

        if (candidate.getStatus() != CandidateStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "CANDIDATE_ALREADY_REVIEWED",
                "Cặp hồ sơ đã được duyệt trước đó");
        }

        candidate.setStatus(CandidateStatus.NOT_DUPLICATE);
        candidate.setReviewNote(note);
        candidate.setReviewedByAccountId(reviewerAccountId);
        candidate.setReviewedAt(LocalDateTime.now());

        return DuplicateCandidateResponse.from(duplicateCandidateRepository.save(candidate));
    }
}
