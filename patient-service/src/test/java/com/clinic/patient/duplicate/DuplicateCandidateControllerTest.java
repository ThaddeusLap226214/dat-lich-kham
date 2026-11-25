package com.clinic.patient.duplicate;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.common.GlobalExceptionHandler;
import com.clinic.patient.common.PageResponse;
import com.clinic.patient.duplicate.dto.DuplicateCandidateResponse;
import com.clinic.patient.duplicate.dto.MergeRequest;
import com.clinic.patient.duplicate.dto.ReviewNoteRequest;
import com.clinic.patient.merge.MergeAction;
import com.clinic.patient.merge.MergeService;
import com.clinic.patient.merge.dto.MergeHistoryResponse;
import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.IdType;
import com.clinic.patient.patient.PatientStatus;
import com.clinic.patient.patient.dto.PatientResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DuplicateCandidateControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private DuplicateReviewService duplicateReviewService;

    @Mock
    private MergeService mergeService;

    @InjectMocks
    private DuplicateCandidateController duplicateCandidateController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(duplicateCandidateController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void list_TraVeDanhSachCapNghiTrung() throws Exception {
        DuplicateCandidateResponse res = sampleCandidate(1L, CandidateStatus.PENDING);
        PageResponse<DuplicateCandidateResponse> page = PageResponse.of(
            new PageImpl<>(List.of(res)), r -> r);

        when(duplicateReviewService.list(eq(CandidateStatus.PENDING), eq(0), eq(20)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/duplicate-candidates?status=PENDING&page=0&size=20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].reason").value("SAME_NAME_DOB_PHONE"))
            .andExpect(jsonPath("$.content[0].score").value(60.00));
    }

    @Test
    void count_TraVeSoLuongCapChuaDuyet() throws Exception {
        when(duplicateReviewService.count(CandidateStatus.PENDING)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/duplicate-candidates/count?status=PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void postMerge_GopHoSoThanhCong() throws Exception {
        MergeHistoryResponse res = new MergeHistoryResponse(
            1L,
            MergeAction.MERGE,
            3L,
            1L,
            PatientStatus.TEMPORARY,
            1L,
            null,
            "event-merge-1",
            2L,
            LocalDateTime.now(),
            "Gộp hồ sơ trùng"
        );

        when(mergeService.merge(eq(1L), eq(1L), eq("Gộp hồ sơ trùng"), eq(2L))).thenReturn(res);

        MergeRequest req = new MergeRequest(1L, "Gộp hồ sơ trùng");

        mockMvc.perform(post("/api/v1/duplicate-candidates/1/merge")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action").value("MERGE"))
            .andExpect(jsonPath("$.sourcePatientId").value(3L))
            .andExpect(jsonPath("$.targetPatientId").value(1L));
    }

    @Test
    void postReject_KhongTrungThanhCong() throws Exception {
        DuplicateCandidateResponse res = sampleCandidate(1L, CandidateStatus.NOT_DUPLICATE);
        when(duplicateReviewService.reject(eq(1L), eq("Không trùng"), eq(2L))).thenReturn(res);

        ReviewNoteRequest req = new ReviewNoteRequest("Không trùng");

        mockMvc.perform(post("/api/v1/duplicate-candidates/1/reject")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NOT_DUPLICATE"));
    }

    @Test
    void postMerge_CapDaDuyet_TraVe409() throws Exception {
        when(mergeService.merge(any(), any(), any(), any()))
            .thenThrow(new BusinessException(HttpStatus.CONFLICT, "CANDIDATE_ALREADY_REVIEWED", "Cặp đã duyệt"));

        MergeRequest req = new MergeRequest(1L, "Gộp");

        mockMvc.perform(post("/api/v1/duplicate-candidates/1/merge")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CANDIDATE_ALREADY_REVIEWED"));
    }

    private static DuplicateCandidateResponse sampleCandidate(Long id, CandidateStatus status) {
        PatientResponse p1 = new PatientResponse(
            3L, null, "Lê Thị Hoa", LocalDate.of(1990, 5, 12), Gender.FEMALE,
            "0905123456", null, null, null, null, null, null, null, PatientStatus.TEMPORARY, null, LocalDateTime.now()
        );
        PatientResponse p2 = new PatientResponse(
            1L, 5L, "Lê Thị Hoa", LocalDate.of(1990, 5, 12), Gender.FEMALE,
            "0905123456", "hoa@gmail.com", "Đà Nẵng", IdType.CCCD, "048190001234", null, null, null, PatientStatus.ACTIVE, null, LocalDateTime.now()
        );
        return new DuplicateCandidateResponse(
            id,
            DuplicateReason.SAME_NAME_DOB_PHONE,
            new BigDecimal("60.00"),
            status,
            p1,
            p2,
            null,
            null,
            null,
            LocalDateTime.now()
        );
    }
}
