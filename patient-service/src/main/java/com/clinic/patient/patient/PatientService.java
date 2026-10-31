package com.clinic.patient.patient;

import com.clinic.patient.common.BusinessException;
import com.clinic.patient.common.NotImplementedYetException;
import com.clinic.patient.common.PageResponse;
import com.clinic.patient.duplicate.DuplicateCandidateRepository;
import com.clinic.patient.patient.dto.AddIdentityRequest;
import com.clinic.patient.patient.dto.CreatePatientResponse;
import com.clinic.patient.patient.dto.PatientRequest;
import com.clinic.patient.patient.dto.PatientResponse;
import com.clinic.patient.patient.dto.PatientSummary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class PatientService {

    private static final String ROLE_PATIENT = "PATIENT";

    private final PatientRepository patientRepository;
    private final DuplicateCandidateRepository duplicateCandidateRepository;

    public PatientService(PatientRepository patientRepository, DuplicateCandidateRepository duplicateCandidateRepository) {
        this.patientRepository = patientRepository;
        this.duplicateCandidateRepository = duplicateCandidateRepository;
    }

    public CreatePatientResponse create(PatientRequest request, Long accountId, String role) {
        throw new NotImplementedYetException("tạo hồ sơ theo bảng tình huống 1-3 (docs/01-nghiep-vu-va-dac-ta-chuc-nang.md mục 4); không bao giờ tự gộp");
    }

    public List<PatientResponse> listMine(Long accountId) {
        throw new NotImplementedYetException("các hồ sơ (chưa MERGED) mà tài khoản đang quản lý");
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
        patient.setFullName(request.fullName().trim());
        patient.setNormalizedName(NameNormalizer.normalize(request.fullName()));
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        patient.setAddress(request.address());
        patient.setGuardianName(request.guardianName());
        patient.setGuardianPhone(request.guardianPhone());
        patient.setGuardianRelation(request.guardianRelation());
        return PatientResponse.from(patientRepository.save(patient));
    }

    public PatientResponse addIdentity(Long id, AddIdentityRequest request, Long accountId) {
        throw new NotImplementedYetException("bổ sung giấy tờ cho hồ sơ tạm: tình huống 4 (thành ACTIVE) và 5 (vào hàng chờ)");
    }

    public List<PatientSummary> findSummaries(Collection<Long> ids) {
        throw new NotImplementedYetException("lấy nhiều hồ sơ rút gọn theo danh sách mã (API nội bộ)");
    }

    private Patient findOrThrow(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Hồ sơ bệnh nhân", id));
    }

    // PATIENT chỉ đụng được hồ sơ mình quản lý; lễ tân, bác sĩ được xem mọi hồ sơ
    private static void checkOwner(Patient patient, Long accountId, String role) {
        if (ROLE_PATIENT.equals(role) && !accountId.equals(patient.getOwnerAccountId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Không có quyền với hồ sơ này");
        }
    }
}
