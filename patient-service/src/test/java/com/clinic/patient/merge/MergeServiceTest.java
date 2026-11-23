package com.clinic.patient.merge;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.duplicate.CandidateStatus;
import com.clinic.patient.duplicate.DuplicateCandidate;
import com.clinic.patient.duplicate.DuplicateCandidateRepository;
import com.clinic.patient.duplicate.DuplicateReason;
import com.clinic.patient.event.PatientEvent;
import com.clinic.patient.event.PatientEventPublisher;
import com.clinic.patient.merge.dto.MergeHistoryResponse;
import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.Patient;
import com.clinic.patient.patient.PatientRepository;
import com.clinic.patient.patient.PatientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DuplicateCandidateRepository duplicateCandidateRepository;

    @Mock
    private MergeHistoryRepository mergeHistoryRepository;

    @Mock
    private PatientEventPublisher eventPublisher;

    private MergeService mergeService;

    @BeforeEach
    void setUp() {
        mergeService = new MergeService(patientRepository, duplicateCandidateRepository, mergeHistoryRepository, eventPublisher);
    }

    @Test
    void gopHoSoThanhCongVaPhatSuKien() {
        Patient source = new Patient();
        source.setId(3L);
        source.setStatus(PatientStatus.TEMPORARY);
        source.setFullName("Le Thi Hoa");

        Patient target = new Patient();
        target.setId(1L);
        target.setStatus(PatientStatus.ACTIVE);
        target.setFullName("Lê Thị Hoa");

        DuplicateCandidate candidate = new DuplicateCandidate();
        candidate.setId(1L);
        candidate.setPatient(source);
        candidate.setCandidatePatient(target);
        candidate.setStatus(CandidateStatus.PENDING);
        candidate.setReason(DuplicateReason.SAME_NAME_DOB_PHONE);
        candidate.setScore(new BigDecimal("60.00"));

        when(duplicateCandidateRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(mergeHistoryRepository.save(any())).thenAnswer(i -> {
            MergeHistory h = i.getArgument(0);
            h.setId(100L);
            return h;
        });

        MergeHistoryResponse response = mergeService.merge(1L, 1L, "Gộp hồ sơ 3 vào 1", 2L);

        assertThat(source.getStatus()).isEqualTo(PatientStatus.MERGED);
        assertThat(source.getMergedIntoId()).isEqualTo(1L);
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.MERGED);
        assertThat(response.action()).isEqualTo(MergeAction.MERGE);
        assertThat(response.sourcePatientId()).isEqualTo(3L);
        assertThat(response.targetPatientId()).isEqualTo(1L);

        ArgumentCaptor<PatientEvent> eventCaptor = ArgumentCaptor.forClass(PatientEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        PatientEvent event = eventCaptor.getValue();
        assertThat(event.eventType()).isEqualTo(PatientEvent.PatientEventType.PATIENT_MERGED);
        assertThat(event.sourcePatientId()).isEqualTo(3L);
        assertThat(event.targetPatientId()).isEqualTo(1L);
        assertThat(event.performedByAccountId()).isEqualTo(2L);
    }

    @Test
    void gopHoSo_KeepPatientNotInPair_BaoLoi400() {
        Patient source = new Patient();
        source.setId(3L);
        Patient target = new Patient();
        target.setId(1L);

        DuplicateCandidate candidate = new DuplicateCandidate();
        candidate.setId(1L);
        candidate.setPatient(source);
        candidate.setCandidatePatient(target);
        candidate.setStatus(CandidateStatus.PENDING);

        when(duplicateCandidateRepository.findById(1L)).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> mergeService.merge(1L, 999L, "Gộp vào hồ sơ không liên quan", 2L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
            .hasFieldOrPropertyWithValue("code", "KEEP_PATIENT_NOT_IN_PAIR");
    }

    @Test
    void tachHoSoGopNhamThanhCongVaPhatSuKien() {
        Patient source = new Patient();
        source.setId(3L);
        source.setStatus(PatientStatus.MERGED);
        source.setMergedIntoId(1L);

        MergeHistory lastMerge = new MergeHistory();
        lastMerge.setId(10L);
        lastMerge.setAction(MergeAction.MERGE);
        lastMerge.setSourcePatientId(3L);
        lastMerge.setTargetPatientId(1L);
        lastMerge.setSourcePreviousStatus(PatientStatus.TEMPORARY);
        lastMerge.setDuplicateCandidateId(1L);
        lastMerge.setEventId("event-123");

        DuplicateCandidate candidate = new DuplicateCandidate();
        candidate.setId(1L);
        candidate.setStatus(CandidateStatus.MERGED);

        when(patientRepository.findById(3L)).thenReturn(Optional.of(source));
        when(mergeHistoryRepository.findFirstBySourcePatientIdAndActionOrderByPerformedAtDesc(3L, MergeAction.MERGE))
            .thenReturn(Optional.of(lastMerge));
        when(duplicateCandidateRepository.findById(1L)).thenReturn(Optional.of(candidate));
        when(mergeHistoryRepository.save(any())).thenAnswer(i -> {
            MergeHistory h = i.getArgument(0);
            h.setId(11L);
            return h;
        });

        MergeHistoryResponse response = mergeService.unmerge(3L, "Lễ tân gộp nhầm, tách lại", 2L);

        assertThat(source.getStatus()).isEqualTo(PatientStatus.TEMPORARY);
        assertThat(source.getMergedIntoId()).isNull();
        assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.NOT_DUPLICATE);
        assertThat(response.action()).isEqualTo(MergeAction.UNMERGE);

        ArgumentCaptor<PatientEvent> eventCaptor = ArgumentCaptor.forClass(PatientEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        PatientEvent event = eventCaptor.getValue();
        assertThat(event.eventType()).isEqualTo(PatientEvent.PatientEventType.PATIENT_UNMERGED);
        assertThat(event.relatedMergeEventId()).isEqualTo("event-123");
    }
}
