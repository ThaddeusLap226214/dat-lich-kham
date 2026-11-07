package com.clinic.scheduling.slot;

import com.clinic.scheduling.appointment.Appointment;
import com.clinic.scheduling.appointment.AppointmentRepository;
import com.clinic.scheduling.appointment.AppointmentStatus;
import com.clinic.scheduling.client.ProviderClient;
import com.clinic.scheduling.client.WorkingHours;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SlotService {

    private final ProviderClient providerClient;
    private final AppointmentRepository appointmentRepository;
    private final int bookAheadDays;
    private final int bookBeforeHours;

    public SlotService(ProviderClient providerClient,
                       AppointmentRepository appointmentRepository,
                       @Value("${scheduling.book-ahead-days:30}") int bookAheadDays,
                       @Value("${scheduling.book-before-hours:2}") int bookBeforeHours) {
        this.providerClient = providerClient;
        this.appointmentRepository = appointmentRepository;
        this.bookAheadDays = bookAheadDays;
        this.bookBeforeHours = bookBeforeHours;
    }

    public List<SlotResponse> findSlots(Long doctorId, LocalDate date, String role) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Lấy giờ làm việc của bác sĩ từ Provider Service
        WorkingHours workingHours = providerClient.getWorkingHours(doctorId, date);
        if (workingHours == null || workingHours.shifts() == null || workingHours.shifts().isEmpty()) {
            return List.of();
        }

        // Lấy các lịch hẹn chiếm slot của bác sĩ trong ngày (HELD, CONFIRMED, COMPLETED, NO_SHOW)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Appointment> occupiedAppointments = appointmentRepository.findByDoctorIdAndStatusInAndStartTimeBetween(
            doctorId,
            List.of(AppointmentStatus.HELD, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW),
            startOfDay,
            endOfDay
        );

        // Tập hợp các mốc startTime đã bị chiếm
        Set<LocalDateTime> occupiedTimes = occupiedAppointments.stream()
            .map(Appointment::getStartTime)
            .collect(Collectors.toSet());

        // Kiểm tra CS05: Ngày quá 30 ngày tính từ hôm nay -> mọi slot available = false
        boolean isBeyondBookAhead = date.isAfter(today.plusDays(bookAheadDays));

        List<SlotResponse> result = new ArrayList<>();

        for (WorkingHours.Shift shift : workingHours.shifts()) {
            LocalTime shiftStart = shift.startTime();
            LocalTime shiftEnd = shift.endTime();
            int slotMinutes = shift.slotMinutes() > 0 ? shift.slotMinutes() : 20;

            LocalTime current = shiftStart;
            // CS11: Cắt ca theo slotMinutes, bỏ phần lẻ cuối ca (current.plusMinutes(slotMinutes) <= shiftEnd)
            while (!current.plusMinutes(slotMinutes).isAfter(shiftEnd)) {
                LocalTime next = current.plusMinutes(slotMinutes);
                LocalDateTime slotStartTime = LocalDateTime.of(date, current);
                LocalDateTime slotEndTime = LocalDateTime.of(date, next);

                boolean available = true;

                // 1. Quá 30 ngày (CS05)
                if (isBeyondBookAhead) {
                    available = false;
                }
                // 2. Slot đã nằm trong quá khứ
                else if (slotStartTime.isBefore(now)) {
                    available = false;
                }
                // 3. Slot đã có người giữ / đặt
                else if (occupiedTimes.contains(slotStartTime)) {
                    available = false;
                }
                // 4. CS06: Bệnh nhân (PATIENT) không đặt được khi còn dưới bookBeforeHours (2 giờ)
                else if ("PATIENT".equals(role) && slotStartTime.isBefore(now.plusHours(bookBeforeHours))) {
                    available = false;
                }

                result.add(new SlotResponse(slotStartTime, slotEndTime, shift.roomId(), shift.workScheduleId(), available));

                current = next;
            }
        }

        return result;
    }
}
