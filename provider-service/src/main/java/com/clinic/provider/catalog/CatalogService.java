package com.clinic.provider.catalog;

import com.clinic.provider.catalog.dto.DoctorRequest;
import com.clinic.provider.catalog.dto.DoctorResponse;
import com.clinic.provider.catalog.dto.RoomRequest;
import com.clinic.provider.catalog.dto.RoomResponse;
import com.clinic.provider.catalog.dto.SpecialtyRequest;
import com.clinic.provider.catalog.dto.SpecialtyResponse;
import com.clinic.provider.common.NotImplementedYetException;
import org.springframework.stereotype.Service;

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

    public List<SpecialtyResponse> listSpecialties() {
        throw new NotImplementedYetException("danh sách chuyên khoa đang hoạt động");
    }

    public SpecialtyResponse createSpecialty(SpecialtyRequest request) {
        throw new NotImplementedYetException("thêm chuyên khoa, 409 CODE_TAKEN nếu trùng mã");
    }

    public SpecialtyResponse updateSpecialty(Long id, SpecialtyRequest request) {
        throw new NotImplementedYetException("sửa chuyên khoa");
    }

    public List<RoomResponse> listRooms() {
        throw new NotImplementedYetException("danh sách phòng khám");
    }

    public List<RoomResponse> findRoomsByIds(Collection<Long> ids) {
        throw new NotImplementedYetException("lấy nhiều phòng theo danh sách mã (API nội bộ cho Gateway)");
    }

    public RoomResponse createRoom(RoomRequest request) {
        throw new NotImplementedYetException("thêm phòng khám");
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        throw new NotImplementedYetException("sửa phòng khám");
    }

    public List<DoctorResponse> listDoctors(Long specialtyId) {
        throw new NotImplementedYetException("danh sách bác sĩ, lọc theo chuyên khoa nếu có");
    }

    public DoctorResponse getDoctor(Long id) {
        throw new NotImplementedYetException("chi tiết bác sĩ, 404 nếu không có");
    }

    public List<DoctorResponse> findDoctorsByIds(Collection<Long> ids) {
        throw new NotImplementedYetException("lấy nhiều bác sĩ theo danh sách mã (API nội bộ cho Gateway)");
    }

    public DoctorResponse createDoctor(DoctorRequest request) {
        throw new NotImplementedYetException("thêm bác sĩ");
    }

    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        throw new NotImplementedYetException("sửa bác sĩ");
    }
}
