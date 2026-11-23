package com.clinic.patient.patient;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.duplicate.CandidateStatus;
import com.clinic.patient.duplicate.DuplicateCandidate;
import com.clinic.patient.duplicate.DuplicateCandidateRepository;
import com.clinic.patient.duplicate.DuplicateReason;
import com.clinic.patient.duplicate.MatchScorer;
import com.clinic.patient.patient.dto.AddIdentityRequest;
import com.clinic.patient.patient.dto.CreatePatientResponse;
import com.clinic.patient.patient.dto.PatientRequest;
import com.clinic.patient.patient.dto.PatientResponse;
import com.clinic.patient.patient.dto.PatientSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DuplicateCandidateRepository duplicateCandidateRepository;

    @Mock
    private MatchScorer matchScorer;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientService(patientRepository, duplicateCandidateRepository, matchScorer, 14);
    }

    @Test
    void tinhHuong1_DungLaiHoSoCoSanKhiKhopGiayToVaHoTenNgaySinh() {
        Patient existing = new Patient();
        existing.setId(1L);
        existing.setFullName("Lê Thị Hoa");
        existing.setNormalizedName("le thi hoa");
        existing.setDateOfBirth(LocalDate.of(1990, 5, 12));
        existing.setStatus(PatientStatus.ACTIVE);
        existing.setIdType(IdType.CCCD);
        existing.setIdNumber("048190001234");

        when(patientRepository.findByIdTypeAndIdNumberAndStatus(IdType.CCCD, "048190001234", PatientStatus.ACTIVE))
            .thenReturn(Optional.of(existing));

        PatientRequest request = new PatientRequest(
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

        CreatePatientResponse response = patientService.create(request, 5L, "PATIENT");

        assertThat(response.outcome()).isEqualTo(CreatePatientResponse.Outcome.REUSED_EXISTING);
        assertThat(response.duplicateQueued()).isFalse();
        assertThat(response.patient().id()).isEqualTo(1L);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void tinhHuong2_TaoHoSoTamVaGhiHangChoKhiGiayToKhacTen() {
        Patient existing = new Patient();
        existing.setId(1L);
        existing.setFullName("Lê Thị Hoa");
        existing.setNormalizedName("le thi hoa");
        existing.setDateOfBirth(LocalDate.of(1990, 5, 12));
        existing.setStatus(PatientStatus.ACTIVE);
        existing.setIdType(IdType.CCCD);
        existing.setIdNumber("048190001234");

        when(patientRepository.findByIdTypeAndIdNumberAndStatus(IdType.CCCD, "048190001234", PatientStatus.ACTIVE))
            .thenReturn(Optional.of(existing));
        when(matchScorer.score(any(), any())).thenReturn(new BigDecimal("80.00"));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });

        PatientRequest request = new PatientRequest(
            "Lê Văn Nam",
            LocalDate.of(1995, 3, 20),
            Gender.MALE,
            "0905999888",
            "nam@gmail.com",
            "Đà Nẵng",
            IdType.CCCD,
            "048190001234",
            null,
            null,
            null
        );

        CreatePatientResponse response = patientService.create(request, 5L, "PATIENT");

        assertThat(response.outcome()).isEqualTo(CreatePatientResponse.Outcome.CREATED_TEMPORARY);
        assertThat(response.duplicateQueued()).isTrue();
        assertThat(response.patient().id()).isEqualTo(2L);
        assertThat(response.patient().status()).isEqualTo(PatientStatus.TEMPORARY);

        ArgumentCaptor<DuplicateCandidate> candidateCaptor = ArgumentCaptor.forClass(DuplicateCandidate.class);
        verify(duplicateCandidateRepository).save(candidateCaptor.capture());
        DuplicateCandidate candidate = candidateCaptor.getValue();
        assertThat(candidate.getReason()).isEqualTo(DuplicateReason.SAME_ID_DIFFERENT_INFO);
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING);
    }

    @Test
    void treDuoi14TuoiThieuNguoiGiamHo_BaoLoi400() {
        PatientRequest request = new PatientRequest(
            "Em Bé 5 Tuổi",
            LocalDate.now().minusYears(5),
            Gender.MALE,
            "0905123456",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> patientService.create(request, 5L, "PATIENT"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
            .hasFieldOrPropertyWithValue("code", "GUARDIAN_REQUIRED");
    }

    @Test
    void cccdKhongDu12So_BaoLoi400() {
        PatientRequest request = new PatientRequest(
            "Nguyễn Văn A",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            "0905123456",
            null,
            null,
            IdType.CCCD,
            "12345",
            null,
            null,
            null
        );

        assertThatThrownBy(() -> patientService.create(request, 5L, "PATIENT"))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
            .hasFieldOrPropertyWithValue("code", "INVALID_ID_NUMBER");
    }

    @Test
    void tinhHuong4_BoSungGiayToMoiChuyenThanhActive() {
        Patient temporary = new Patient();
        temporary.setId(2L);
        temporary.setStatus(PatientStatus.TEMPORARY);

        when(patientRepository.findById(2L)).thenReturn(Optional.of(temporary));
        when(patientRepository.findByIdTypeAndIdNumberAndStatus(IdType.CCCD, "048190009999", PatientStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

        AddIdentityRequest request = new AddIdentityRequest(IdType.CCCD, "048190009999");
        PatientResponse response = patientService.addIdentity(2L, request, 2L);

        assertThat(response.status()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(response.idNumber()).isEqualTo("048190009999");
    }

    @Test
    void tinhHuong5_BoSungGiayToDaThuocHoSoKhacGiuTemporaryVaVaoHangCho() {
        Patient temporary = new Patient();
        temporary.setId(2L);
        temporary.setStatus(PatientStatus.TEMPORARY);

        Patient active = new Patient();
        active.setId(1L);
        active.setStatus(PatientStatus.ACTIVE);

        when(patientRepository.findById(2L)).thenReturn(Optional.of(temporary));
        when(patientRepository.findByIdTypeAndIdNumberAndStatus(IdType.CCCD, "048190001234", PatientStatus.ACTIVE))
            .thenReturn(Optional.of(active));
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> i.getArgument(0));

        AddIdentityRequest request = new AddIdentityRequest(IdType.CCCD, "048190001234");
        PatientResponse response = patientService.addIdentity(2L, request, 2L);

        assertThat(response.status()).isEqualTo(PatientStatus.TEMPORARY);
        verify(duplicateCandidateRepository).save(any(DuplicateCandidate.class));
    }

    @Test
    void listMineVaFindSummaries() {
        Patient p1 = new Patient();
        p1.setId(1L);
        p1.setOwnerAccountId(5L);
        p1.setStatus(PatientStatus.ACTIVE);
        p1.setFullName("Lê Thị Hoa");
        p1.setEmail("hoa@gmail.com");

        when(patientRepository.findByOwnerAccountIdAndStatusNot(5L, PatientStatus.MERGED))
            .thenReturn(List.of(p1));
        when(patientRepository.findByIdIn(List.of(1L)))
            .thenReturn(List.of(p1));

        List<PatientResponse> mine = patientService.listMine(5L);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).fullName()).isEqualTo("Lê Thị Hoa");

        List<PatientSummary> summaries = patientService.findSummaries(List.of(1L));
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).email()).isEqualTo("hoa@gmail.com");
    }
}
