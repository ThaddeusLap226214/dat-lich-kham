-- Nhật ký nhắc cho các lịch hẹn tuần trước (appointment_id khớp scheduling-service/data.sql, gửi trước giờ khám 1 ngày)
INSERT IGNORE INTO reminder_logs (id, appointment_id, patient_id, appointment_start, reminder_type, recipient_email,
                                  status, attempt_count, last_error, sent_at)
VALUES
    (1, 1, 1, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:30:00'), 'BEFORE_APPOINTMENT',
     'hoa.le@example.com', 'SENT', 1, NULL, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '07:35:00')),
    (2, 2, 4, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:50:00'), 'BEFORE_APPOINTMENT',
     'nam.tran@example.com', 'SENT', 1, NULL, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '07:55:00')),
    (3, 3, 5, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:30:00'), 'BEFORE_APPOINTMENT',
     'nam.tran@example.com', 'SENT', 2, 'Lần 1: không kết nối được máy chủ mail',
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '07:40:00')),
    (4, 5, 9, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:10:00'), 'BEFORE_APPOINTMENT',
     'tam.le@example.com', 'SENT', 1, NULL, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '08:15:00')),
    (5, 14, 7, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:30:00'), 'BEFORE_APPOINTMENT',
     NULL, 'SKIPPED', 1, 'Hồ sơ không có email', NULL);
