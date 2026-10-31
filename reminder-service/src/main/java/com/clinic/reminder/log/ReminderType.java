package com.clinic.reminder.log;

// Khớp CHECK ck_reminder_logs_type và dữ liệu mẫu: thiếu giá trị thì JPA lỗi khi đọc các dòng nhật ký đó
public enum ReminderType {
    BEFORE_APPOINTMENT,
    // Nhắc lần hai trước giờ khám (việc mở rộng P5-7); mẫu thư của loại này do người làm P5-7 thêm
    BEFORE_APPOINTMENT_2H,
    CONFIRMATION,
    CANCELLATION
}
