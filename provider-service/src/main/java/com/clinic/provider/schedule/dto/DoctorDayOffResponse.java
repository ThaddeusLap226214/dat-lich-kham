package com.clinic.provider.schedule.dto;

import com.clinic.provider.schedule.DoctorDayOff;

import java.time.LocalDate;

public record DoctorDayOffResponse(Long id, Long doctorId, LocalDate offDate, String reason) {

    public static DoctorDayOffResponse from(DoctorDayOff dayOff) {
        return new DoctorDayOffResponse(dayOff.getId(), dayOff.getDoctor().getId(), dayOff.getOffDate(),
            dayOff.getReason());
    }
}

