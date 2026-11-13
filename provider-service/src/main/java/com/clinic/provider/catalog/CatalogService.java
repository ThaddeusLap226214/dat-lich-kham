package com.clinic.provider.catalog;

import com.clinic.provider.catalog.dto.DoctorRequest;
import com.clinic.provider.catalog.dto.DoctorResponse;
import com.clinic.provider.catalog.dto.RoomRequest;
import com.clinic.provider.catalog.dto.RoomResponse;
import com.clinic.provider.catalog.dto.SpecialtyRequest;
import com.clinic.provider.catalog.dto.SpecialtyResponse;
import com.clinic.provider.common.BusinessException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class CatalogService {

    private final SpecialtyRepository specialtyRepository;
    private final RoomRepository roomRepository;
    private final DoctorRepository doctorRepository;

    public CatalogService(SpecialtyRepository specialtyRepository, RoomRepository roomRepository,
                          DoctorRepository doctorRepository) {
        this.specialtyRepository = specialtyRepository;
        this.roomRepository = roomRepository;
        this.doctorRepository = doctorRepository;
    }

    @Cacheable("specialties")
    @Transactional(readOnly = true)
    public List<SpecialtyResponse> listSpecialties() {
        return specialtyRepository.findByActiveTrueOrderByNameAsc().stream()
            .map(SpecialtyResponse::from)
            .toList();
    }

    @CacheEvict(cacheNames = {"specialties", "doctors"}, allEntries = true)
    @Transactional
    public SpecialtyResponse createSpecialty(SpecialtyRequest request) {
        String code = request.code().trim();
        String name = request.name().trim();
        if (specialtyRepository.existsByCode(code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CODE_TAKEN", "Mã chuyên khoa " + code + " đã tồn tại");
        }
        if (specialtyRepository.existsByName(name)) {
            throw new BusinessException(HttpStatus.CONFLICT, "NAME_TAKEN", "Tên chuyên khoa " + name + " đã tồn tại");
        }
        Specialty specialty = new Specialty();
        specialty.setCode(code);
        specialty.setName(name);
        specialty.setDescription(trimOrNull(request.description()));
        specialty.setActive(request.active() != null ? request.active() : true);
        return SpecialtyResponse.from(specialtyRepository.save(specialty));
    }

    @CacheEvict(cacheNames = {"specialties", "doctors"}, allEntries = true)
    @Transactional
    public SpecialtyResponse updateSpecialty(Long id, SpecialtyRequest request) {
        Specialty specialty = specialtyRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Chuyên khoa", id));
        String code = request.code().trim();
        String name = request.name().trim();
        if (specialtyRepository.existsByCodeAndIdNot(code, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CODE_TAKEN", "Mã chuyên khoa " + code + " đã tồn tại");
        }
        if (specialtyRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "NAME_TAKEN", "Tên chuyên khoa " + name + " đã tồn tại");
        }
        specialty.setCode(code);
        specialty.setName(name);
        specialty.setDescription(trimOrNull(request.description()));
        if (request.active() != null) {
            specialty.setActive(request.active());
        }
        return SpecialtyResponse.from(specialtyRepository.save(specialty));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms() {
        return roomRepository.findAll(Sort.by("code")).stream()
            .map(RoomResponse::from)
            .toList();
    }

    public List<RoomResponse> findRoomsByIds(Collection<Long> ids) {
        return roomRepository.findByIdIn(ids).stream().map(RoomResponse::from).toList();
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        String code = request.code().trim();
        if (roomRepository.existsByCode(code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CODE_TAKEN", "Mã phòng khám " + code + " đã tồn tại");
        }
        Room room = new Room();
        room.setCode(code);
        room.setName(request.name().trim());
        room.setLocation(trimOrNull(request.location()));
        room.setActive(request.active() != null ? request.active() : true);
        return RoomResponse.from(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Phòng khám", id));
        String code = request.code().trim();
        if (roomRepository.existsByCodeAndIdNot(code, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "CODE_TAKEN", "Mã phòng khám " + code + " đã tồn tại");
        }
        room.setCode(code);
        room.setName(request.name().trim());
        room.setLocation(trimOrNull(request.location()));
        if (request.active() != null) {
            room.setActive(request.active());
        }
        return RoomResponse.from(roomRepository.save(room));
    }

    @Cacheable(cacheNames = "doctors", key = "#specialtyId == null ? 'ALL' : #specialtyId")
    @Transactional(readOnly = true)
    public List<DoctorResponse> listDoctors(Long specialtyId) {
        List<Doctor> doctors = specialtyId == null
            ? doctorRepository.findByActiveTrue()
            : doctorRepository.findBySpecialtyIdAndActiveTrue(specialtyId);
        return doctors.stream().map(DoctorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Bác sĩ", id));
        return DoctorResponse.from(doctor);
    }

    public List<DoctorResponse> findDoctorsByIds(Collection<Long> ids) {
        return doctorRepository.findByIdIn(ids).stream().map(DoctorResponse::from).toList();
    }

    @CacheEvict(cacheNames = "doctors", allEntries = true)
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        Specialty specialty = specialtyRepository.findById(request.specialtyId())
            .orElseThrow(() -> BusinessException.notFound("Chuyên khoa", request.specialtyId()));
        Doctor doctor = new Doctor();
        applyDoctorFields(doctor, request, specialty);
        doctor.setActive(request.active() != null ? request.active() : true);
        return DoctorResponse.from(doctorRepository.save(doctor));
    }

    @CacheEvict(cacheNames = "doctors", allEntries = true)
    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Bác sĩ", id));
        Specialty specialty = specialtyRepository.findById(request.specialtyId())
            .orElseThrow(() -> BusinessException.notFound("Chuyên khoa", request.specialtyId()));
        applyDoctorFields(doctor, request, specialty);
        if (request.active() != null) {
            doctor.setActive(request.active());
        }
        return DoctorResponse.from(doctorRepository.save(doctor));
    }

    private void applyDoctorFields(Doctor doctor, DoctorRequest request, Specialty specialty) {
        doctor.setFullName(request.fullName().trim());
        doctor.setTitle(trimOrNull(request.title()));
        doctor.setSpecialty(specialty);
        doctor.setPhone(trimOrNull(request.phone()));
        doctor.setEmail(trimOrNull(request.email()));
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
