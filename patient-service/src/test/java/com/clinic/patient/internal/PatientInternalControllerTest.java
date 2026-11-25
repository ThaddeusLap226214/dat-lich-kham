package com.clinic.patient.internal;

import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.PatientService;
import com.clinic.patient.patient.PatientStatus;
import com.clinic.patient.patient.dto.PatientSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PatientInternalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientInternalController patientInternalController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientInternalController).build();
    }

    @Test
    void patientsByIds_TraVeDanhSachSummary() throws Exception {
        PatientSummary s1 = new PatientSummary(
            1L,
            "Lê Thị Hoa",
            LocalDate.of(1990, 5, 12),
            Gender.FEMALE,
            "0905123456",
            "hoa@gmail.com",
            null,
            PatientStatus.ACTIVE,
            null
        );
        PatientSummary s2 = new PatientSummary(
            2L,
            "Trần Văn Bình",
            LocalDate.of(1985, 8, 20),
            Gender.MALE,
            "0912345678",
            "binh@gmail.com",
            null,
            PatientStatus.ACTIVE,
            null
        );

        when(patientService.findSummaries(List.of(1L, 2L))).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/internal/v1/patients?ids=1,2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].fullName").value("Lê Thị Hoa"))
            .andExpect(jsonPath("$[0].phone").value("0905123456"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].fullName").value("Trần Văn Bình"));
    }
}
