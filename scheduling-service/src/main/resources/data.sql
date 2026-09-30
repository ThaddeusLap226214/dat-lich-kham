-- Ngày tính theo ngày chạy lần đầu, để lịch luôn nằm quanh "hôm nay" và khớp ca làm việc của bác sĩ (provider-service/data.sql):
--   thứ Hai tuần trước : CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY
--   thứ Hai tuần tới   : CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY
--   ngày làm việc gần nhất của bác sĩ 2 (T2–T7): ngày mai, hoặc thứ Hai nếu ngày mai là Chủ nhật
-- patient_id: bên Patient; doctor_id, room_id, work_schedule_id: bên Provider; booked_by_account_id: bên Auth
INSERT IGNORE INTO appointments (id, patient_id, doctor_id, room_id, work_schedule_id, booked_by_account_id,
                                 start_time, end_time, status, hold_expires_at, reason, cancel_reason,
                                 cancelled_by_account_id, confirmed_at, cancelled_at, completed_at)
VALUES
    -- Tuần trước: đã khám / đã hủy
    (1, 1, 1, 1, 1, 5,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:30:00'),
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:50:00'),
     'COMPLETED', NULL, 'Khám sức khỏe định kỳ', NULL, NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 10) DAY, '20:15:00'), NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:48:00')),
    (2, 4, 1, 1, 1, 10,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:50:00'),
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:10:00'),
     'COMPLETED', NULL, 'Đau đầu, chóng mặt', NULL, NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 9) DAY, '09:00:00'), NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:09:00')),
    (3, 5, 2, 2, 8, 10,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:30:00'),
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:45:00'),
     'COMPLETED', NULL, 'Sốt, ho 3 ngày', NULL, NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '21:30:00'), NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '07:44:00')),
    (4, 6, 4, 4, 16, 11,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:00:00'),
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:30:00'),
     'CANCELLED', NULL, 'Đau răng hàm', 'Bận việc đột xuất', 11,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 11) DAY, '10:00:00'),
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 8) DAY, '18:00:00'), NULL),
    -- Lịch của hồ sơ 10, đã được chuyển sang hồ sơ 9 khi gộp (xem patient_reassignments)
    (5, 9, 1, 1, 1, 2,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:10:00'),
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:30:00'),
     'COMPLETED', NULL, 'Khám tổng quát', NULL, NULL,
     NOW() - INTERVAL 16 DAY, NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:28:00')),
    -- Người cao tuổi không có email: nhật ký nhắc lịch ghi SKIPPED
    (14, 7, 1, 1, 1, 2,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:30:00'),
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:50:00'),
     'COMPLETED', NULL, 'Tăng huyết áp', NULL, NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 9) DAY, '15:00:00'), NULL,
     TIMESTAMP(CURDATE() - INTERVAL (WEEKDAY(CURDATE()) + 7) DAY, '08:47:00')),

    -- Tuần tới: đã đặt
    (6, 1, 1, 1, 1, 5,
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:10:00'),
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:30:00'),
     'CONFIRMED', NULL, 'Tái khám huyết áp', NULL, NULL, NOW() - INTERVAL 1 DAY, NULL, NULL),
    (7, 2, 2, 2, 8, 5,
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '07:45:00'),
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:00:00'),
     'CONFIRMED', NULL, 'Khám nhi định kỳ', NULL, NULL, NOW() - INTERVAL 1 DAY, NULL, NULL),
    (8, 4, 1, 1, 1, 10,
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:30:00'),
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:50:00'),
     'CONFIRMED', NULL, 'Tái khám', NULL, NULL, NOW() - INTERVAL 2 DAY, NULL, NULL),
    (9, 6, 4, 4, 16, 11,
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:30:00'),
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '09:00:00'),
     'CONFIRMED', NULL, 'Đau răng hàm', NULL, NULL, NOW() - INTERVAL 1 DAY, NULL, NULL),
    -- Lễ tân đặt hộ người cao tuổi
    (10, 7, 1, 1, 6, 2,
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '13:30:00'),
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '13:50:00'),
     'CONFIRMED', NULL, 'Khám tim mạch', NULL, NULL, NOW() - INTERVAL 3 DAY, NULL, NULL),
    -- Tình huống 2: hồ sơ tạm (đang chờ duyệt trùng) vẫn đặt lịch được; thứ Ba tuần tới, Tai Mũi Họng
    (11, 8, 3, 3, 14, 2,
     TIMESTAMP(CURDATE() + INTERVAL (8 - WEEKDAY(CURDATE())) DAY, '13:30:00'),
     TIMESTAMP(CURDATE() + INTERVAL (8 - WEEKDAY(CURDATE())) DAY, '13:50:00'),
     'CONFIRMED', NULL, 'Ù tai', NULL, NULL, NOW() - INTERVAL 1 DAY, NULL, NULL),
    -- Giữ chỗ nhưng không xác nhận kịp → hết hạn, slot đã được nhả
    (12, 5, 2, 2, 8, 10,
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:00:00'),
     TIMESTAMP(CURDATE() + INTERVAL (7 - WEEKDAY(CURDATE())) DAY, '08:15:00'),
     'EXPIRED', NOW() - INTERVAL 1 DAY, 'Tiêm nhắc lại', NULL, NULL, NULL, NULL, NULL),
    -- Lịch gần nhất (1–2 ngày tới) để thử nhắc lịch
    (13, 2, 2, 2, 8 + WEEKDAY(CURDATE() + INTERVAL IF(WEEKDAY(CURDATE()) = 5, 2, 1) DAY), 5,
     TIMESTAMP(CURDATE() + INTERVAL IF(WEEKDAY(CURDATE()) = 5, 2, 1) DAY, '09:00:00'),
     TIMESTAMP(CURDATE() + INTERVAL IF(WEEKDAY(CURDATE()) = 5, 2, 1) DAY, '09:15:00'),
     'CONFIRMED', NULL, 'Ho, sổ mũi', NULL, NULL, NOW() - INTERVAL 1 DAY, NULL, NULL);

-- Lịch 5 được chuyển từ hồ sơ 10 sang hồ sơ 9 khi xử lý sự kiện PATIENT_MERGED (event_id khớp merge_history bên Patient)
INSERT IGNORE INTO patient_reassignments (id, appointment_id, from_patient_id, to_patient_id, merge_event_id,
                                          reassigned_at, reverted_at)
VALUES (1, 5, 10, 9, '7c1e5a2e-3b1f-4d6a-9c1e-000000000001', NOW() - INTERVAL 14 DAY, NULL);

INSERT IGNORE INTO processed_events (event_id, event_type, processed_at)
VALUES ('7c1e5a2e-3b1f-4d6a-9c1e-000000000001', 'PATIENT_MERGED', NOW() - INTERVAL 14 DAY);
