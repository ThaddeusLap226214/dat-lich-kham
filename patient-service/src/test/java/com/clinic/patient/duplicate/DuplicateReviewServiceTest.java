package com.clinic.patient.duplicate;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.duplicate.dto.DuplicateCandidateResponse;
import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.IdType;
import com.clinic.patient.patient.Patient;
import com.clinic.patient.patient.PatientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateReviewServiceTest {

    @Mock
    private DuplicateCandidateRepository duplicateCandidateRepository;

    private DuplicateReviewService duplicateReviewService;

    @BeforeEach
    void setUp() {
        duplicateReviewService = new DuplicateReviewService(duplicateCandidateRepository);
    }

    @Test
    void listVaCount() {
        DuplicateCandidate c = createCandidate(1L, CandidateStatus.PENDING);
        when(duplicateCandidateRepository.findByStatusOrderByCreatedAtDesc(eq(CandidateStatus.PENDING), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(c)));
        when(duplicateCandidateRepository.countByStatus(CandidateStatus.PENDING)).thenReturn(1L);

        var page = duplicateReviewService.list(CandidateStatus.PENDING, 0, 10);
        assertThat(page.content()).hasSize(1);
        assertThat(duplicateReviewService.count(CandidateStatus.PENDING)).isEqualTo(1L);
    }

    @Test
    void rejectThanhCong() {
        DuplicateCandidate c = createCandidate(1L, CandidateStatus.PENDING);
        when(duplicateCandidateRepository.findById(1L)).thenReturn(Optional.of(c));
        when(duplicateCandidateRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DuplicateCandidateResponse res = duplicateReviewService.reject(1L, "Không trùng, hai người khác nhau", 2L);
        assertThat(res.status()).isEqualTo(CandidateStatus.NOT_DUPLICATE);
        assertThat(res.reviewNote()).isEqualTo("Không trùng, hai người khác nhau");
        assertThat(res.reviewedByAccountId()).isEqualTo(2L);
    }

    @Test
    void rejectCapDaDuyet_BaoLoi409() {
        DuplicateCandidate c = createCandidate(1L, CandidateStatus.NOT_DUPLICATE);
        when(duplicateCandidateRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> duplicateReviewService.reject(1L, "note", 2L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
            .hasFieldOrPropertyWithValue("code", "CANDIDATE_ALREADY_REVIEWED");
    }

    private static DuplicateCandidate createCandidate(Long id, CandidateStatus status) {
        DuplicateCandidate c = new DuplicateCandidate();
        c.setId(id);
        c.setStatus(status);
        c.setReason(DuplicateReason.SAME_NAME_DOB_PHONE);
        c.setScore(new BigDecimal("60.00"));

        Patient p1 = new Patient();
        p1.setId(3L);
        p1.setFullName("Lê Thị Hoa");
        p1.setNormalizedName("le thi hoa");
        p1.setDateOfBirth(LocalDate.of(1990, 5, 12));
        p1.setGender(Gender.FEMALE);
        p1.setStatus(PatientStatus.TEMPORARY);

        Patient p2 = new Patient();
        p2.setId(1L);
        p2.setFullName("Lê Thị Hoa");
        p2.setNormalizedName("le thi hoa");
        p2.setDateOfBirth(LocalDate.of(1990, 5, 12));
        p2.setGender(Gender.FEMALE);
        p2.setStatus(PatientStatus.ACTIVE);
        p2.setIdType(IdType.CCCD);
        p2.setIdNumber("048190001234");

        c.setPatient(p1);
        c.setCandidatePatient(p2);
        return c;
    }
}
