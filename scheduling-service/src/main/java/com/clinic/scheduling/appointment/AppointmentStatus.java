package com.clinic.scheduling.appointment;

// HELD → CONFIRMED → COMPLETED; CONFIRMED → NO_SHOW (không đến); HELD → EXPIRED (quá giờ giữ chỗ); CONFIRMED → CANCELLED
public enum AppointmentStatus {
    HELD,
    CONFIRMED,
    COMPLETED,
    NO_SHOW,
    EXPIRED,
    CANCELLED
}
