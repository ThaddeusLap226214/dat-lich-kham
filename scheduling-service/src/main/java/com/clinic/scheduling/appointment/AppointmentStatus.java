package com.clinic.scheduling.appointment;

// HELD → CONFIRMED → COMPLETED; HELD → EXPIRED (quá giờ giữ chỗ); CONFIRMED → CANCELLED
public enum AppointmentStatus {
    HELD,
    CONFIRMED,
    COMPLETED,
    EXPIRED,
    CANCELLED
}
