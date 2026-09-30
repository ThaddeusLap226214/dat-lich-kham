package com.clinic.scheduling.slot;

import com.clinic.scheduling.appointment.AppointmentRepository;
import com.clinic.scheduling.client.ProviderClient;
import com.clinic.scheduling.common.NotImplementedYetException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SlotService {

    private final ProviderClient providerClient;
    private final AppointmentRepository appointmentRepository;

    public SlotService(ProviderClient providerClient, AppointmentRepository appointmentRepository) {
        this.providerClient = providerClient;
        this.appointmentRepository = appointmentRepository;
    }

    public List<SlotResponse> findSlots(Long doctorId, LocalDate date) {
        throw new NotImplementedYetException(
            "lấy ca làm việc từ ProviderClient, cắt thành slot theo slotMinutes, đánh dấu slot đã có lịch HELD/CONFIRMED/COMPLETED "
                + "và slot đã qua giờ là không trống");
    }
}
