package com.clinic.patient.patient;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.common.GlobalExceptionHandler;
import com.clinic.patient.merge.MergeAction;
import com.clinic.patient.merge.MergeService;
import com.clinic.patient.merge.dto.MergeHistoryResponse;
import com.clinic.patient.patient.dto.AddIdentityRequest;
import com.clinic.patient.patient.dto.CreatePatientResponse;
import com.clinic.patient.patient.dto.PatientRequest;
import com.clinic.patient.patient.dto.PatientResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private PatientService patientService;

    @Mock
    private MergeService mergeService;

    @InjectMocks
    private PatientController patientController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void postPatients_TaoMoiTraVe201() throws Exception {
        PatientResponse patientRes = samplePatient(1L, PatientStatus.ACTIVE);
        CreatePatientResponse res = new CreatePatientResponse(patientRes, CreatePatientResponse.Outcome.CREATED_ACTIVE, false);

        when(patientService.create(any(), eq(5L), eq("PATIENT"))).thenReturn(res);

        PatientRequest req = new PatientRequest(
            "Lê Thị Hoa",
            LocalDate.of(1990, 5, 12),
            Gender.FEMALE,
            "0905123456",
            "hoa@gmail.com",
            "Đà Nẵng",
            IdType.CCCD,
            "048190001234",
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/patients")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "PATIENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.outcome").value("CREATED_ACTIVE"))
            .andExpect(jsonPath("$.patient.fullName").value("Lê Thị Hoa"));
    }

    @Test
    void postPatients_DungLaiTraVe200() throws Exception {
        PatientResponse patientRes = samplePatient(1L, PatientStatus.ACTIVE);
        CreatePatientResponse res = new CreatePatientResponse(patientRes, CreatePatientResponse.Outcome.REUSED_EXISTING, false);

        when(patientService.create(any(), eq(5L), eq("PATIENT"))).thenReturn(res);

        PatientRequest req = new PatientRequest(
            "Lê Thị Hoa",
            LocalDate.of(1990, 5, 12),
            Gender.FEMALE,
            "0905123456",
            "hoa@gmail.com",
            "Đà Nẵng",
            IdType.CCCD,
            "048190001234",
            null,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/patients")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "PATIENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("REUSED_EXISTING"));
    }

    @Test
    void getMine_TraVeDanhSachHoSoCuaTaiKhoan() throws Exception {
        PatientResponse p = samplePatient(1L, PatientStatus.ACTIVE);
        when(patientService.listMine(5L)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/v1/patients/mine")
                .header("X-User-Id", 5L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].fullName").value("Lê Thị Hoa"));
    }

    @Test
    void getPatientById_ThanhCong() throws Exception {
        PatientResponse p = samplePatient(1L, PatientStatus.ACTIVE);
        when(patientService.get(1L, 5L, "PATIENT")).thenReturn(p);

        mockMvc.perform(get("/api/v1/patients/1")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "PATIENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getPatientById_KhongCoQuyen_TraVe403() throws Exception {
        when(patientService.get(1L, 99L, "PATIENT"))
            .thenThrow(new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Không có quyền với hồ sơ này"));

        mockMvc.perform(get("/api/v1/patients/1")
                .header("X-User-Id", 99L)
                .header("X-User-Role", "PATIENT"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void postIdentity_BoSungGiayToThanhCong() throws Exception {
        PatientResponse updated = samplePatient(2L, PatientStatus.ACTIVE);
        when(patientService.addIdentity(eq(2L), any(), eq(2L))).thenReturn(updated);

        AddIdentityRequest req = new AddIdentityRequest(IdType.CCCD, "048190001234");

        mockMvc.perform(post("/api/v1/patients/2/identity")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void postUnmerge_TachHoSoThanhCong() throws Exception {
        MergeHistoryResponse res = new MergeHistoryResponse(
            1L,
            MergeAction.UNMERGE,
            3L,
            1L,
            PatientStatus.TEMPORARY,
            1L,
            10L,
            "event-123",
            2L,
            LocalDateTime.now(),
            "Lễ tân tách hồ sơ gộp nhầm"
        );

        when(mergeService.unmerge(eq(3L), any(), eq(2L))).thenReturn(res);

        mockMvc.perform(post("/api/v1/patients/3/unmerge")
                .header("X-User-Id", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Lễ tân tách hồ sơ gộp nhầm\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.action").value("UNMERGE"))
            .andExpect(jsonPath("$.sourcePatientId").value(3L));
    }

    private static PatientResponse samplePatient(Long id, PatientStatus status) {
        return new PatientResponse(
            id,
            5L,
            "Lê Thị Hoa",
            LocalDate.of(1990, 5, 12),
            Gender.FEMALE,
            "0905123456",
            "hoa@gmail.com",
            "Đà Nẵng",
            IdType.CCCD,
            "048190001234",
            null,
            null,
            null,
            status,
            null,
            LocalDateTime.now()
        );
    }
}
