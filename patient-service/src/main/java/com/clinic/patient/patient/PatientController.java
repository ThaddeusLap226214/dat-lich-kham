package com.clinic.patient.patient;

import com.clinic.patient.common.PageResponse;
import com.clinic.patient.duplicate.dto.ReviewNoteRequest;
import com.clinic.patient.merge.MergeService;
import com.clinic.patient.merge.dto.MergeHistoryResponse;
import com.clinic.patient.patient.dto.AddIdentityRequest;
import com.clinic.patient.patient.dto.CreatePatientResponse;
import com.clinic.patient.patient.dto.PatientRequest;
import com.clinic.patient.patient.dto.PatientResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;
    private final MergeService mergeService;

    public PatientController(PatientService patientService, MergeService mergeService) {
        this.patientService = patientService;
        this.mergeService = mergeService;
    }

    @PostMapping
    public ResponseEntity<CreatePatientResponse> create(@Valid @RequestBody PatientRequest request,
                                                        @RequestHeader("X-User-Id") Long accountId,
                                                        @RequestHeader("X-User-Role") String role) {
        CreatePatientResponse result = patientService.create(request, accountId, role);
        HttpStatus status = result.outcome() == CreatePatientResponse.Outcome.REUSED_EXISTING
            ? HttpStatus.OK
            : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/mine")
    public List<PatientResponse> mine(@RequestHeader("X-User-Id") Long accountId) {
        return patientService.listMine(accountId);
    }

    @GetMapping
    public PageResponse<PatientResponse> search(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) PatientStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return patientService.search(keyword, status, page, size);
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long accountId,
                               @RequestHeader("X-User-Role") String role) {
        return patientService.get(id, accountId, role);
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request,
                                  @RequestHeader("X-User-Id") Long accountId,
                                  @RequestHeader("X-User-Role") String role) {
        return patientService.update(id, request, accountId, role);
    }

    @PostMapping("/{id}/identity")
    public PatientResponse addIdentity(@PathVariable Long id, @Valid @RequestBody AddIdentityRequest request,
                                       @RequestHeader("X-User-Id") Long accountId) {
        return patientService.addIdentity(id, request, accountId);
    }

    @PostMapping("/{id}/unmerge")
    public MergeHistoryResponse unmerge(@PathVariable Long id, @Valid @RequestBody ReviewNoteRequest request,
                                        @RequestHeader("X-User-Id") Long accountId) {
        return mergeService.unmerge(id, request.note(), accountId);
    }

    @GetMapping("/{id}/merge-history")
    public List<MergeHistoryResponse> mergeHistory(@PathVariable Long id) {
        return mergeService.history(id);
    }
}
