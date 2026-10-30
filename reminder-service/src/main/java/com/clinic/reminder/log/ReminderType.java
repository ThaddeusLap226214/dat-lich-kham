package com.clinic.reminder.log;

// Khớp CHECK ck_reminder_logs_type và dữ liệu mẫu: thiếu giá trị thì JPA lỗi khi đọc các dòng nhật ký đó
public enum ReminderType {
    BEFORE_APPOINTMENT,
    CONFIRMATION,
    CANCELLATION
}
