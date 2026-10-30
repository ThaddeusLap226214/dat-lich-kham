package com.clinic.provider.schedule;

import com.clinic.provider.catalog.DoctorRepository;
import com.clinic.provider.catalog.RoomRepository;
import com.clinic.provider.common.BusinessException;
import com.clinic.provider.common.NotImplementedYetException;
import com.clinic.provider.schedule.dto.WorkScheduleRequest;
import com.clinic.provider.schedule.dto.WorkScheduleResponse;
import com.clinic.provider.schedule.dto.WorkingHoursResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final DoctorRepository doctorRepository;
    private final RoomRepository roomRepository;

    public WorkScheduleService(WorkScheduleRepository workScheduleRepository, DoctorRepository doctorRepository,
                               RoomRepository roomRepository) {
        this.workScheduleRepository = workScheduleRepository;
        this.doctorRepository = doctorRepository;
        this.roomRepository = roomRepository;
    }

    public List<WorkScheduleResponse> listByDoctor(Long doctorId) {
        throw new NotImplementedYetException("danh sách ca làm việc của bác sĩ");
    }

    public WorkScheduleResponse create(Long doctorId, WorkScheduleRequest request) {
        throw new NotImplementedYetException("thêm ca làm việc, 409 SCHEDULE_OVERLAP nếu chồng giờ với ca khác cùng ngày");
    }

    public WorkScheduleResponse update(Long id, WorkScheduleRequest request) {
        throw new NotImplementedYetException("sửa ca làm việc");
    }

    public void delete(Long id) {
        throw new NotImplementedYetException("ngừng ca làm việc (đặt active = false, không xóa hẳn)");
    }

    public WorkingHoursResponse getWorkingHours(Long doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw BusinessException.notFound("Bác sĩ", doctorId);
        }
        // getDayOfWeek().getValue(): 1 = Thứ Hai ... 7 = Chủ nhật, cùng quy ước với cột day_of_week
        List<WorkingHoursResponse.Shift> shifts = workScheduleRepository
            .findEffective(doctorId, date.getDayOfWeek().getValue(), date).stream()
            .map(w -> new WorkingHoursResponse.Shift(w.getId(), w.getRoom().getId(), w.getStartTime(), w.getEndTime(),
                w.getSlotMinutes()))
            .toList();
        return new WorkingHoursResponse(doctorId, date, shifts);
    }
}
