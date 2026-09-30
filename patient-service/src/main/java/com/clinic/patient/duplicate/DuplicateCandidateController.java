package com.clinic.patient.duplicate;

import com.clinic.patient.common.PageResponse;
import com.clinic.patient.duplicate.dto.DuplicateCandidateResponse;
import com.clinic.patient.duplicate.dto.MergeRequest;
import com.clinic.patient.duplicate.dto.ReviewNoteRequest;
import com.clinic.patient.merge.MergeService;
import com.clinic.patient.merge.dto.MergeHistoryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/duplicate-candidates")
public class DuplicateCandidateController {

    private final DuplicateReviewService duplicateReviewService;
    private final MergeService mergeService;

    public DuplicateCandidateController(DuplicateReviewService duplicateReviewService, MergeService mergeService) {
        this.duplicateReviewService = duplicateReviewService;
        this.mergeService = mergeService;
    }

    @GetMapping
    public PageResponse<DuplicateCandidateResponse> list(@RequestParam(defaultValue = "PENDING") CandidateStatus status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return duplicateReviewService.list(status, page, size);
    }

    @GetMapping("/count")
    public Map<String, Long> count(@RequestParam(defaultValue = "PENDING") CandidateStatus status) {
        return Map.of("count", duplicateReviewService.count(status));
    }

    @PostMapping("/{id}/merge")
    public MergeHistoryResponse merge(@PathVariable Long id, @Valid @RequestBody MergeRequest request,
                                      @RequestHeader("X-User-Id") Long accountId) {
        return mergeService.merge(id, request.keepPatientId(), request.note(), accountId);
    }

    @PostMapping("/{id}/reject")
    public DuplicateCandidateResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewNoteRequest request,
                                             @RequestHeader("X-User-Id") Long accountId) {
        return duplicateReviewService.reject(id, request.note(), accountId);
    }
}
