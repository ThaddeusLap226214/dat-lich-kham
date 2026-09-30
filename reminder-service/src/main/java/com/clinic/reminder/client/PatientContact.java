package com.clinic.reminder.client;

// Các trường cần dùng từ PatientSummary của Patient Service
public record PatientContact(Long id, String fullName, String email, String phone, String guardianPhone) {
}
