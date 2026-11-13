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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkScheduleServiceTest {

    @Mock
    private WorkScheduleRepository workScheduleRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private DoctorDayOffRepository doctorDayOffRepository;

    private WorkScheduleService workScheduleService;

    @BeforeEach
    void setUp() {
        workScheduleService = new WorkScheduleService(workScheduleRepository, doctorRepository, roomRepository,
            doctorDayOffRepository);
    }

    @Test
    void listByDoctor_returnsSchedulesOrThrows404() {
        when(doctorRepository.existsById(99L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> workScheduleService.listByDoctor(99L));

        Doctor doc = doctor(1L);
        Room room = room(1L);
        WorkSchedule ws = schedule(10L, doc, room, 1, LocalTime.of(7, 30), LocalTime.of(11, 30));
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(workScheduleRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(1L)).thenReturn(List.of(ws));

        List<WorkScheduleResponse> result = workScheduleService.listByDoctor(1L);
        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().id());
    }

    @Test
    void create_allowsAdjacentShiftAndRejectsOverlappingShift() {
        Doctor doc = doctor(1L);
        Room room = room(1L);
        WorkSchedule existing = schedule(10L, doc, room, 1, LocalTime.of(7, 30), LocalTime.of(11, 30));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(workScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(1L, 1)).thenReturn(List.of(existing));
        when(workScheduleRepository.save(any(WorkSchedule.class))).thenAnswer(inv -> {
            WorkSchedule saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        // Liền kề 11:30 - 13:30 được phép
        WorkScheduleResponse adjacent = workScheduleService.create(1L, new WorkScheduleRequest(
            1L, 1, LocalTime.of(11, 30), LocalTime.of(13, 30), 20,
            LocalDate.of(2026, 1, 1), null, true));
        assertEquals(11L, adjacent.id());

        // Chồng giờ 10:00 - 12:00 bị từ chối 409 SCHEDULE_OVERLAP
        BusinessException overlap = assertThrows(BusinessException.class, () -> workScheduleService.create(1L,
            new WorkScheduleRequest(1L, 1, LocalTime.of(10, 0), LocalTime.of(12, 0), 20,
                LocalDate.of(2026, 1, 1), null, true)));
        assertEquals(HttpStatus.CONFLICT, overlap.getStatus());
        assertEquals("SCHEDULE_OVERLAP", overlap.getCode());
    }

    @Test
    void create_validatesTimeAndDateRange() {
        Doctor doc = doctor(1L);
        Room room = room(1L);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        BusinessException badTime = assertThrows(BusinessException.class, () -> workScheduleService.create(1L,
            new WorkScheduleRequest(1L, 1, LocalTime.of(11, 30), LocalTime.of(11, 30), 20,
                LocalDate.of(2026, 1, 1), null, true)));
        assertEquals("INVALID_TIME_RANGE", badTime.getCode());

        BusinessException badDate = assertThrows(BusinessException.class, () -> workScheduleService.create(1L,
            new WorkScheduleRequest(1L, 1, LocalTime.of(7, 30), LocalTime.of(11, 30), 20,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), true)));
        assertEquals("INVALID_DATE_RANGE", badDate.getCode());
    }

    @Test
    void update_allowsUpdatingSelfWithoutOverlapConflict() {
        Doctor doc = doctor(1L);
        Room room = room(1L);
        WorkSchedule existing = schedule(10L, doc, room, 1, LocalTime.of(7, 30), LocalTime.of(11, 30));

        when(workScheduleRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(workScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(1L, 1)).thenReturn(List.of(existing));
        when(workScheduleRepository.save(any(WorkSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkScheduleResponse updated = workScheduleService.update(10L, new WorkScheduleRequest(
            1L, 1, LocalTime.of(8, 0), LocalTime.of(11, 30), 15,
            LocalDate.of(2026, 1, 1), null, true));
        assertEquals(15, updated.slotMinutes());
        assertTrue(updated.active());
    }

    @Test
    void delete_setsActiveFalseInsteadOfDeleting() {
        Doctor doc = doctor(1L);
        Room room = room(1L);
        WorkSchedule existing = schedule(10L, doc, room, 1, LocalTime.of(7, 30), LocalTime.of(11, 30));
        when(workScheduleRepository.findById(10L)).thenReturn(Optional.of(existing));

        workScheduleService.delete(10L);

        assertFalse(existing.isActive());
        verify(workScheduleRepository).save(existing);
    }

    @Test
    void getWorkingHours_keepsF14Behavior() {
        Doctor doc = doctor(1L);
        Room room = room(1L);
        LocalDate monday = LocalDate.of(2026, 11, 16);
        WorkSchedule ws = schedule(10L, doc, room, 1, LocalTime.of(7, 30), LocalTime.of(11, 30));

        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(doctorDayOffRepository.existsByDoctorIdAndOffDate(1L, monday)).thenReturn(false);
        when(workScheduleRepository.findEffective(1L, 1, monday)).thenReturn(List.of(ws));

        WorkingHoursResponse res = workScheduleService.getWorkingHours(1L, monday);
        assertEquals(1L, res.doctorId());
        assertEquals(1, res.shifts().size());

        when(doctorDayOffRepository.existsByDoctorIdAndOffDate(1L, monday)).thenReturn(true);
        WorkingHoursResponse dayOffRes = workScheduleService.getWorkingHours(1L, monday);
        assertTrue(dayOffRes.shifts().isEmpty());
    }

    @Test
    void dayOffs_addDuplicateAndDelete() {
        Doctor doc = doctor(1L);
        LocalDate offDate = LocalDate.of(2026, 11, 20);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doc));
        when(doctorDayOffRepository.existsByDoctorIdAndOffDate(1L, offDate)).thenReturn(false);
        when(doctorDayOffRepository.save(any(DoctorDayOff.class))).thenAnswer(inv -> {
            DoctorDayOff d = inv.getArgument(0);
            d.setId(5L);
            return d;
        });

        DoctorDayOffResponse created = workScheduleService.addDayOff(1L, new DoctorDayOffRequest(offDate, "Nghỉ phép"));
        assertEquals(5L, created.id());
        assertEquals(offDate, created.offDate());

        when(doctorDayOffRepository.existsByDoctorIdAndOffDate(1L, offDate)).thenReturn(true);
        BusinessException dup = assertThrows(BusinessException.class, () ->
            workScheduleService.addDayOff(1L, new DoctorDayOffRequest(offDate, "Nghỉ nữa")));
        assertEquals("DAY_OFF_EXISTS", dup.getCode());

        DoctorDayOff saved = new DoctorDayOff();
        saved.setId(5L);
        saved.setDoctor(doc);
        saved.setOffDate(offDate);
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(doctorDayOffRepository.findByIdAndDoctorId(5L, 1L)).thenReturn(Optional.of(saved));
        workScheduleService.deleteDayOff(1L, 5L);
        verify(doctorDayOffRepository).delete(saved);
    }

    private Doctor doctor(Long id) {
        Doctor d = new Doctor();
        d.setId(id);
        d.setFullName("Nguyễn Văn An");
        return d;
    }

    private Room room(Long id) {
        Room r = new Room();
        r.setId(id);
        r.setCode("P101");
        r.setName("Phòng 101");
        return r;
    }

    private WorkSchedule schedule(Long id, Doctor doctor, Room room, int dayOfWeek, LocalTime start, LocalTime end) {
        WorkSchedule ws = new WorkSchedule();
        ws.setId(id);
        ws.setDoctor(doctor);
        ws.setRoom(room);
        ws.setDayOfWeek(dayOfWeek);
        ws.setStartTime(start);
        ws.setEndTime(end);
        ws.setSlotMinutes(20);
        ws.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        ws.setActive(true);
        return ws;
    }
}

