package com.clinic.provider.schedule;

import com.clinic.provider.catalog.Doctor;
import com.clinic.provider.catalog.DoctorRepository;
import com.clinic.provider.catalog.Room;
import com.clinic.provider.catalog.RoomRepository;
import com.clinic.provider.common.BusinessException;
import com.clinic.provider.schedule.dto.DoctorDayOffRequest;
import com.clinic.provider.schedule.dto.DoctorDayOffResponse;
import com.clinic.provider.schedule.dto.WorkScheduleRequest;
import com.clinic.provider.schedule.dto.WorkScheduleResponse;
import com.clinic.provider.schedule.dto.WorkingHoursResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final DoctorRepository doctorRepository;
    private final RoomRepository roomRepository;
    private final DoctorDayOffRepository doctorDayOffRepository;

    public WorkScheduleService(WorkScheduleRepository workScheduleRepository, DoctorRepository doctorRepository,
                               RoomRepository roomRepository, DoctorDayOffRepository doctorDayOffRepository) {
        this.workScheduleRepository = workScheduleRepository;
        this.doctorRepository = doctorRepository;
        this.roomRepository = roomRepository;
        this.doctorDayOffRepository = doctorDayOffRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkScheduleResponse> listByDoctor(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw BusinessException.notFound("Bác sĩ", doctorId);
        }
        return workScheduleRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId).stream()
            .map(WorkScheduleResponse::from)
            .toList();
    }

    @Transactional
    public WorkScheduleResponse create(Long doctorId, WorkScheduleRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> BusinessException.notFound("Bác sĩ", doctorId));
        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> BusinessException.notFound("Phòng khám", request.roomId()));
        validateScheduleInput(request);

        boolean active = request.active() != null ? request.active() : true;
        if (active) {
            checkNoOverlap(doctorId, request, null);
        }

        WorkSchedule schedule = new WorkSchedule();
        schedule.setDoctor(doctor);
        applyScheduleFields(schedule, room, request, active);
        return WorkScheduleResponse.from(workScheduleRepository.save(schedule));
    }

    @Transactional
    public WorkScheduleResponse update(Long id, WorkScheduleRequest request) {
        WorkSchedule schedule = workScheduleRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Ca làm việc", id));
        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> BusinessException.notFound("Phòng khám", request.roomId()));
        validateScheduleInput(request);

        boolean active = request.active() != null ? request.active() : schedule.isActive();
        if (active) {
            checkNoOverlap(schedule.getDoctor().getId(), request, id);
        }

        applyScheduleFields(schedule, room, request, active);
        return WorkScheduleResponse.from(workScheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long id) {
        WorkSchedule schedule = workScheduleRepository.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Ca làm việc", id));
        schedule.setActive(false);
        workScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public WorkingHoursResponse getWorkingHours(Long doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw BusinessException.notFound("Bác sĩ", doctorId);
        }
        if (doctorDayOffRepository.existsByDoctorIdAndOffDate(doctorId, date)) {
            return new WorkingHoursResponse(doctorId, date, List.of());
        }
        // getDayOfWeek().getValue(): 1 = Thứ Hai ... 7 = Chủ nhật, cùng quy ước với cột day_of_week
        List<WorkingHoursResponse.Shift> shifts = workScheduleRepository
            .findEffective(doctorId, date.getDayOfWeek().getValue(), date).stream()
            .map(w -> new WorkingHoursResponse.Shift(w.getId(), w.getRoom().getId(), w.getStartTime(), w.getEndTime(),
                w.getSlotMinutes()))
            .toList();
        return new WorkingHoursResponse(doctorId, date, shifts);
    }

    @Transactional(readOnly = true)
    public List<DoctorDayOffResponse> listDayOffs(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw BusinessException.notFound("Bác sĩ", doctorId);
        }
        return doctorDayOffRepository.findByDoctorIdOrderByOffDateAsc(doctorId).stream()
            .map(DoctorDayOffResponse::from)
            .toList();
    }

    @Transactional
    public DoctorDayOffResponse addDayOff(Long doctorId, DoctorDayOffRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> BusinessException.notFound("Bác sĩ", doctorId));
        if (doctorDayOffRepository.existsByDoctorIdAndOffDate(doctorId, request.offDate())) {
            throw new BusinessException(HttpStatus.CONFLICT, "DAY_OFF_EXISTS",
                "Bác sĩ đã có ngày nghỉ vào ngày " + request.offDate());
        }
        DoctorDayOff dayOff = new DoctorDayOff();
        dayOff.setDoctor(doctor);
        dayOff.setOffDate(request.offDate());
        dayOff.setReason(request.reason() != null && !request.reason().isBlank() ? request.reason().trim() : null);
        return DoctorDayOffResponse.from(doctorDayOffRepository.save(dayOff));
    }

    @Transactional
    public void deleteDayOff(Long doctorId, Long dayOffId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw BusinessException.notFound("Bác sĩ", doctorId);
        }
        DoctorDayOff dayOff = doctorDayOffRepository.findByIdAndDoctorId(dayOffId, doctorId)
            .orElseThrow(() -> BusinessException.notFound("Ngày nghỉ", dayOffId));
        doctorDayOffRepository.delete(dayOff);
    }

    private void validateScheduleInput(WorkScheduleRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE",
                "Giờ bắt đầu phải trước giờ kết thúc");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                "Ngày hết hiệu lực không được trước ngày bắt đầu hiệu lực");
        }
    }

    private void checkNoOverlap(Long doctorId, WorkScheduleRequest request, Long excludeScheduleId) {
        List<WorkSchedule> activeSameDay = workScheduleRepository
            .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, request.dayOfWeek());

        for (WorkSchedule existing : activeSameDay) {
            if (excludeScheduleId != null && excludeScheduleId.equals(existing.getId())) {
                continue;
            }
            boolean datesOverlap =
                (existing.getEffectiveTo() == null || !request.effectiveFrom().isAfter(existing.getEffectiveTo()))
                    && (request.effectiveTo() == null || !existing.getEffectiveFrom().isAfter(request.effectiveTo()));
            boolean timesOverlap =
                request.startTime().isBefore(existing.getEndTime())
                    && existing.getStartTime().isBefore(request.endTime());
            if (datesOverlap && timesOverlap) {
                throw new BusinessException(HttpStatus.CONFLICT, "SCHEDULE_OVERLAP",
                    "Ca làm việc bị chồng giờ với ca đã có trong cùng ngày");
            }
        }
    }

    private void applyScheduleFields(WorkSchedule schedule, Room room, WorkScheduleRequest request, boolean active) {
        schedule.setRoom(room);
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setSlotMinutes(request.slotMinutes());
        schedule.setEffectiveFrom(request.effectiveFrom());
        schedule.setEffectiveTo(request.effectiveTo());
        schedule.setActive(active);
    }
}
