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

INSERT IGNORE INTO message_templates (type, subject, body, updated_by_account_id)
VALUES
    ('BEFORE_APPOINTMENT', 'Nhắc lịch khám lúc {startTime}',
     'Kính gửi {patientName},\n\nPhòng khám nhắc bạn có lịch khám lúc {startTime} (mã lịch hẹn {appointmentId}).\nVui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân.\n\nTrân trọng.',
     NULL),
    ('CONFIRMATION', 'Xác nhận lịch khám số {appointmentId}',
     'Kính gửi {patientName},\n\nLịch khám của bạn lúc {startTime} đã được xác nhận (mã lịch hẹn {appointmentId}).\nVui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân.\n\nTrân trọng.',
     NULL),
    ('CANCELLATION', 'Lịch khám số {appointmentId} đã được hủy',
     'Kính gửi {patientName},\n\nLịch khám lúc {startTime} (mã lịch hẹn {appointmentId}) đã được hủy.\nLý do: {cancelReason}\nNếu cần khám, vui lòng đặt lịch mới trên hệ thống.\n\nTrân trọng.',
     NULL);

-- Thư xác nhận / báo hủy: dịch vụ thông báo bắt đầu gửi từ 00:00 Chủ nhật tuần trước,
-- tức CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY. Lịch nào xác nhận / hủy từ mốc đó trở đi
-- (theo confirmed_at / cancelled_at trong scheduling-service/data.sql) có đúng một dòng log và một processed_events.
-- appointment_start chép biểu thức start_time của lịch; sent_at = lúc xác nhận / hủy + 1 phút
INSERT IGNORE INTO reminder_logs (id, appointment_id, patient_id, appointment_start, reminder_type, recipient_email,
                                  status, attempt_count, last_error, sent_at)
VALUES
    (6, 3, 5, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:30:00'), 'CONFIRMATION',
     'nam.tran@example.com', 'SENT', 1, NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '21:30:00') + INTERVAL 1 MINUTE),
    -- Lịch 4 xác nhận trước mốc nên chỉ có thư báo hủy
    (7, 4, 6, TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:00:00'), 'CANCELLATION',
     'lan.pham@example.com', 'SENT', 1, NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '18:00:00') + INTERVAL 1 MINUTE),
    (8, 6, 1, TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:10:00'), 'CONFIRMATION',
     'hoa.le@example.com', 'SENT', 1, NULL, NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),
    (9, 7, 2, TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '07:45:00'), 'CONFIRMATION',
     'hoa.le@example.com', 'SENT', 1, NULL, NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),
    (10, 8, 4, TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:30:00'), 'CONFIRMATION',
     'nam.tran@example.com', 'SENT', 1, NULL, NOW() - INTERVAL 2 DAY + INTERVAL 1 MINUTE),
    (11, 9, 6, TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:30:00'), 'CONFIRMATION',
     'lan.pham@example.com', 'SENT', 1, NULL, NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),
    (12, 10, 7, TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '13:30:00'), 'CONFIRMATION',
     NULL, 'SKIPPED', 1, 'Hồ sơ không có email', NULL),
    (13, 11, 8, TIMESTAMP(CURDATE() + INTERVAL (8 - WEEKDAY(CURDATE())) DAY, '13:30:00'), 'CONFIRMATION',
     NULL, 'SKIPPED', 1, 'Hồ sơ không có email', NULL),
    (14, 13, 2, TIMESTAMP(CURDATE() + INTERVAL IF(WEEKDAY(CURDATE()) = 5, 2, 1) DAY, '09:00:00'), 'CONFIRMATION',
     'hoa.le@example.com', 'SENT', 1, NULL, NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE);

-- Một sự kiện đã xử lý cho mỗi thư xác nhận / báo hủy ở trên (đuôi event_id = id của log); dòng SKIPPED lấy lúc xác nhận + 1 phút
INSERT IGNORE INTO processed_events (event_id, event_type, processed_at)
VALUES
    ('9a3d0e10-5b2c-4f7e-8a61-000000000006', 'APPOINTMENT_CONFIRMED',
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '21:30:00') + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000007', 'APPOINTMENT_CANCELLED',
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '18:00:00') + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000008', 'APPOINTMENT_CONFIRMED', NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000009', 'APPOINTMENT_CONFIRMED', NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000010', 'APPOINTMENT_CONFIRMED', NOW() - INTERVAL 2 DAY + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000011', 'APPOINTMENT_CONFIRMED', NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000012', 'APPOINTMENT_CONFIRMED', NOW() - INTERVAL 3 DAY + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000013', 'APPOINTMENT_CONFIRMED', NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),
    ('9a3d0e10-5b2c-4f7e-8a61-000000000014', 'APPOINTMENT_CONFIRMED', NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE);
