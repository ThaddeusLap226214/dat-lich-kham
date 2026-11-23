package com.clinic.patient.patient;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.common.PageResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private static final String ROLE_PATIENT = "PATIENT";

    private final PatientRepository patientRepository;
    private final DuplicateCandidateRepository duplicateCandidateRepository;
    private final MatchScorer matchScorer;
    private final int guardianRequiredUnderAge;

    public PatientService(PatientRepository patientRepository,
                          DuplicateCandidateRepository duplicateCandidateRepository,
                          MatchScorer matchScorer,
                          @Value("${patient.guardian-required-under-age:14}") int guardianRequiredUnderAge) {
        this.patientRepository = patientRepository;
        this.duplicateCandidateRepository = duplicateCandidateRepository;
        this.matchScorer = matchScorer;
        this.guardianRequiredUnderAge = guardianRequiredUnderAge;
    }

    @Transactional
    public CreatePatientResponse create(PatientRequest request, Long accountId, String role) {
        validateGuardian(request.dateOfBirth(), request.guardianName(), request.guardianPhone());
        validateIdentityNumber(request.idType(), request.idNumber());

        String phone = PhoneNormalizer.normalize(request.phone());
        String guardianPhone = PhoneNormalizer.normalize(request.guardianPhone());
        String normalizedName = NameNormalizer.normalize(request.fullName());
        String idNumber = request.idNumber() != null && !request.idNumber().isBlank()
            ? request.idNumber().trim()
            : null;

        Long ownerAccountId = ROLE_PATIENT.equalsIgnoreCase(role) ? accountId : null;

        if (request.idType() != null && idNumber != null) {
            Optional<Patient> activeExistingOpt = patientRepository.findByIdTypeAndIdNumberAndStatus(
                request.idType(), idNumber, PatientStatus.ACTIVE);

            if (activeExistingOpt.isPresent()) {
                Patient existing = activeExistingOpt.get();
                boolean sameName = existing.getNormalizedName().equals(normalizedName);
                boolean sameDob = existing.getDateOfBirth().equals(request.dateOfBirth());

                if (sameName && sameDob) {
                    // Tình huống 1: Dùng lại hồ sơ có sẵn
                    return new CreatePatientResponse(
                        PatientResponse.from(existing),
                        CreatePatientResponse.Outcome.REUSED_EXISTING,
                        false
                    );
                } else {
                    // Tình huống 2: Giấy tờ đã thuộc hồ sơ ACTIVE khác thông tin -> Tạo hồ sơ TEMPORARY và ghi hàng chờ
                    Patient patient = buildPatient(request, normalizedName, phone, guardianPhone, idNumber,
                        ownerAccountId, accountId, PatientStatus.TEMPORARY);
                    Patient saved = patientRepository.save(patient);

                    DuplicateCandidate candidate = new DuplicateCandidate();
                    candidate.setPatient(saved);
                    candidate.setCandidatePatient(existing);
                    candidate.setReason(DuplicateReason.SAME_ID_DIFFERENT_INFO);
                    candidate.setScore(matchScorer.score(saved, existing));
                    candidate.setStatus(CandidateStatus.PENDING);
                    duplicateCandidateRepository.save(candidate);

                    return new CreatePatientResponse(
                        PatientResponse.from(saved),
                        CreatePatientResponse.Outcome.CREATED_TEMPORARY,
                        true
                    );
                }
            } else {
                // Có giấy tờ và chưa thuộc hồ sơ ACTIVE nào -> Tạo hồ sơ ACTIVE
                Patient patient = buildPatient(request, normalizedName, phone, guardianPhone, idNumber,
                    ownerAccountId, accountId, PatientStatus.ACTIVE);
                Patient saved = patientRepository.save(patient);

                return new CreatePatientResponse(
                    PatientResponse.from(saved),
                    CreatePatientResponse.Outcome.CREATED_ACTIVE,
                    false
                );
            }
        }

        // Tình huống 3: Không có giấy tờ -> Tạo hồ sơ TEMPORARY, kiểm tra trùng họ tên + ngày sinh + SĐT
        Patient patient = buildPatient(request, normalizedName, phone, guardianPhone, null,
            ownerAccountId, accountId, PatientStatus.TEMPORARY);
        Patient saved = patientRepository.save(patient);

        boolean duplicateQueued = false;
        List<Patient> sameNameDobList = patientRepository.findByNormalizedNameAndDateOfBirthAndStatusNot(
            normalizedName, request.dateOfBirth(), PatientStatus.MERGED);

        for (Patient existing : sameNameDobList) {
            if (!existing.getId().equals(saved.getId())) {
                if (phone != null && phone.equals(existing.getPhone())) {
                    DuplicateCandidate candidate = new DuplicateCandidate();
                    candidate.setPatient(saved);
                    candidate.setCandidatePatient(existing);
                    candidate.setReason(DuplicateReason.SAME_NAME_DOB_PHONE);
                    candidate.setScore(matchScorer.score(saved, existing));
                    candidate.setStatus(CandidateStatus.PENDING);
                    duplicateCandidateRepository.save(candidate);
                    duplicateQueued = true;
                }
            }
        }

        return new CreatePatientResponse(
            PatientResponse.from(saved),
            CreatePatientResponse.Outcome.CREATED_TEMPORARY,
            duplicateQueued
        );
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listMine(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        return patientRepository.findByOwnerAccountIdAndStatusNot(accountId, PatientStatus.MERGED)
            .stream()
            .map(PatientResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> search(String keyword, PatientStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(patientRepository.findAll(PatientSpecification.search(keyword, status), pageable),
            PatientResponse::from);
    }

    @Transactional(readOnly = true)
    public PatientResponse get(Long id, Long accountId, String role) {
        Patient patient = findOrThrow(id);
        checkOwner(patient, accountId, role);
        return PatientResponse.from(patient);
    }

    // Giấy tờ (idType, idNumber) trong request bị bỏ qua: đổi giấy tờ phải qua addIdentity để áp quy tắc chống trùng
    @Transactional
    public PatientResponse update(Long id, PatientRequest request, Long accountId, String role) {
        Patient patient = findOrThrow(id);
        checkOwner(patient, accountId, role);
        if (patient.getStatus() == PatientStatus.MERGED) {
            throw new BusinessException(HttpStatus.CONFLICT, "PATIENT_MERGED",
                "Hồ sơ đã gộp vào hồ sơ " + patient.getMergedIntoId() + ", sửa trên hồ sơ đó");
        }

        validateGuardian(request.dateOfBirth(), request.guardianName(), request.guardianPhone());

        patient.setFullName(request.fullName().trim());
        patient.setNormalizedName(NameNormalizer.normalize(request.fullName()));
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setPhone(PhoneNormalizer.normalize(request.phone()));
        patient.setEmail(request.email() != null && !request.email().isBlank() ? request.email().trim() : null);
        patient.setAddress(request.address() != null && !request.address().isBlank() ? request.address().trim() : null);
        patient.setGuardianName(request.guardianName() != null && !request.guardianName().isBlank() ? request.guardianName().trim() : null);
        patient.setGuardianPhone(PhoneNormalizer.normalize(request.guardianPhone()));
        patient.setGuardianRelation(request.guardianRelation() != null && !request.guardianRelation().isBlank() ? request.guardianRelation().trim() : null);
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse addIdentity(Long id, AddIdentityRequest request, Long accountId) {
        Patient patient = findOrThrow(id);
        if (patient.getStatus() != PatientStatus.TEMPORARY) {
            throw new BusinessException(HttpStatus.CONFLICT, "NOT_TEMPORARY", "Chỉ bổ sung giấy tờ cho hồ sơ tạm");
        }

        validateIdentityNumber(request.idType(), request.idNumber());
        String idNumber = request.idNumber().trim();

        Optional<Patient> activeExistingOpt = patientRepository.findByIdTypeAndIdNumberAndStatus(
            request.idType(), idNumber, PatientStatus.ACTIVE);

        if (activeExistingOpt.isEmpty()) {
            // Tình huống 4: Giấy tờ chưa thuộc ai -> Thành ACTIVE
            patient.setIdType(request.idType());
            patient.setIdNumber(idNumber);
            patient.setStatus(PatientStatus.ACTIVE);
            return PatientResponse.from(patientRepository.save(patient));
        } else {
            // Tình huống 5: Giấy tờ đã thuộc hồ sơ ACTIVE khác -> Giữ TEMPORARY, đưa vào hàng chờ
            Patient existing = activeExistingOpt.get();
            patient.setIdType(request.idType());
            patient.setIdNumber(idNumber);
            Patient saved = patientRepository.save(patient);

            if (!duplicateCandidateRepository.existsByPatientIdAndCandidatePatientId(saved.getId(), existing.getId())) {
                DuplicateCandidate candidate = new DuplicateCandidate();
                candidate.setPatient(saved);
                candidate.setCandidatePatient(existing);
                candidate.setReason(DuplicateReason.ID_ADDED_BELONGS_TO_OTHER);
                candidate.setScore(matchScorer.score(saved, existing));
                candidate.setStatus(CandidateStatus.PENDING);
                duplicateCandidateRepository.save(candidate);
            }

            return PatientResponse.from(saved);
        }
    }

    @Transactional(readOnly = true)
    public List<PatientSummary> findSummaries(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return patientRepository.findByIdIn(ids)
            .stream()
            .map(PatientSummary::from)
            .toList();
    }

    private Patient findOrThrow(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Hồ sơ bệnh nhân", id));
    }

    private void validateGuardian(LocalDate dateOfBirth, String guardianName, String guardianPhone) {
        if (dateOfBirth != null) {
            int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
            if (age < guardianRequiredUnderAge) {
                if (guardianName == null || guardianName.isBlank()
                    || guardianPhone == null || guardianPhone.isBlank()) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "GUARDIAN_REQUIRED",
                        "Bệnh nhân dưới " + guardianRequiredUnderAge + " tuổi bắt buộc có thông tin người giám hộ");
                }
            }
        }
    }

    private void validateIdentityNumber(IdType idType, String idNumber) {
        if (idType == IdType.CCCD) {
            if (idNumber == null || !idNumber.trim().matches("^\\d{12}$")) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ID_NUMBER", "Số CCCD phải gồm đúng 12 chữ số");
            }
        }
    }

    private Patient buildPatient(PatientRequest request, String normalizedName, String phone, String guardianPhone,
                                 String idNumber, Long ownerAccountId, Long createdByAccountId, PatientStatus status) {
        Patient p = new Patient();
        p.setOwnerAccountId(ownerAccountId);
        p.setCreatedByAccountId(createdByAccountId);
        p.setFullName(request.fullName().trim());
        p.setNormalizedName(normalizedName);
        p.setDateOfBirth(request.dateOfBirth());
        p.setGender(request.gender());
        p.setPhone(phone);
        p.setEmail(request.email() != null && !request.email().isBlank() ? request.email().trim() : null);
        p.setAddress(request.address() != null && !request.address().isBlank() ? request.address().trim() : null);
        p.setIdType(request.idType());
        p.setIdNumber(idNumber);
        p.setGuardianName(request.guardianName() != null && !request.guardianName().isBlank() ? request.guardianName().trim() : null);
        p.setGuardianPhone(guardianPhone);
        p.setGuardianRelation(request.guardianRelation() != null && !request.guardianRelation().isBlank() ? request.guardianRelation().trim() : null);
        p.setStatus(status);
        return p;
    }

    // PATIENT chỉ đụng được hồ sơ mình quản lý; lễ tân, bác sĩ được xem mọi hồ sơ
    private static void checkOwner(Patient patient, Long accountId, String role) {
        if (ROLE_PATIENT.equalsIgnoreCase(role) && (accountId == null || !accountId.equals(patient.getOwnerAccountId()))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Không có quyền với hồ sơ này");
        }
    }
}
