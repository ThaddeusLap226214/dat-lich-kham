package com.clinic.patient.duplicate;

import com.clinic.patient.common.NotImplementedYetException;
import com.clinic.patient.common.PageResponse;
import com.clinic.patient.duplicate.dto.DuplicateCandidateResponse;
import org.springframework.stereotype.Service;

@Service
public class DuplicateReviewService {

    private final DuplicateCandidateRepository duplicateCandidateRepository;

    public DuplicateReviewService(DuplicateCandidateRepository duplicateCandidateRepository) {
        this.duplicateCandidateRepository = duplicateCandidateRepository;
    }

    public PageResponse<DuplicateCandidateResponse> list(CandidateStatus status, int page, int size) {
        throw new NotImplementedYetException("hàng chờ duyệt, mới nhất lên đầu");
    }

    public long count(CandidateStatus status) {
        throw new NotImplementedYetException("đếm số cặp đang chờ duyệt (hiện số trên trang lễ tân)");
    }

    public DuplicateCandidateResponse reject(Long candidateId, String note, Long reviewerAccountId) {
        throw new NotImplementedYetException("lễ tân xác nhận không trùng → NOT_DUPLICATE");
    }
}
