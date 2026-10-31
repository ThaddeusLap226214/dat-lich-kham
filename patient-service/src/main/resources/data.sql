-- owner_account_id / created_by_account_id là mã tài khoản bên Auth Service
-- (2 = letan, 5 = benhnhan, 10 = nam.tran, 11 = lan.pham)
INSERT IGNORE INTO patients (id, owner_account_id, created_by_account_id, full_name, normalized_name, date_of_birth,
                             gender, phone, email, address, id_type, id_number, guardian_name, guardian_phone,
                             guardian_relation, status, merged_into_id)
VALUES
    -- Tài khoản benhnhan: bản thân (có CCCD) và con (trẻ em chưa có giấy tờ)
    (1, 5, 5, 'Lê Thị Hoa', 'le thi hoa', '1990-05-12', 'FEMALE', '0905123456', 'hoa.le@example.com',
     '12 Nguyễn Văn Linh, Đà Nẵng', 'CCCD', '048190001234', NULL, NULL, NULL, 'ACTIVE', NULL),
    (2, 5, 5, 'Lê Minh Khôi', 'le minh khoi', '2018-03-20', 'MALE', NULL, 'hoa.le@example.com',
     '12 Nguyễn Văn Linh, Đà Nẵng', NULL, NULL, 'Lê Thị Hoa', '0905123456', 'Mẹ', 'TEMPORARY', NULL),
    -- Tình huống 3: lễ tân tạo tại quầy, không giấy tờ, trùng tên + ngày sinh + SĐT với hồ sơ 1
    (3, NULL, 2, 'Le Thi Hoa', 'le thi hoa', '1990-05-12', 'FEMALE', '0905123456', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'TEMPORARY', NULL),
    -- Tài khoản nam.tran: bản thân và con
    (4, 10, 10, 'Trần Văn Nam', 'tran van nam', '1985-11-02', 'MALE', '0912345678', 'nam.tran@example.com',
     '45 Lê Duẩn, Đà Nẵng', 'CCCD', '048085004567', NULL, NULL, NULL, 'ACTIVE', NULL),
    (5, 10, 10, 'Trần Gia Bảo', 'tran gia bao', '2016-07-15', 'MALE', NULL, 'nam.tran@example.com',
     '45 Lê Duẩn, Đà Nẵng', NULL, NULL, 'Trần Văn Nam', '0912345678', 'Bố', 'TEMPORARY', NULL),
    -- Tài khoản lan.pham: dùng thẻ BHYT làm giấy tờ
    (6, 11, 11, 'Phạm Thị Lan', 'pham thi lan', '1972-01-25', 'FEMALE', '0978123456', 'lan.pham@example.com',
     '8 Hùng Vương, Đà Nẵng', 'BHYT', 'DN4480123456789', NULL, NULL, NULL, 'ACTIVE', NULL),
    -- Người cao tuổi đến quầy, lễ tân tạo hồ sơ
    (7, NULL, 2, 'Nguyễn Văn Hùng', 'nguyen van hung', '1958-09-09', 'MALE', '0935111222', NULL,
     '102 Trần Phú, Đà Nẵng', 'CCCD', '048058007890', NULL, NULL, NULL, 'ACTIVE', NULL),
    -- Tình huống 2: cùng CCCD với hồ sơ 7 nhưng ngày sinh gõ nhầm → hồ sơ tạm, chờ duyệt
    (8, NULL, 2, 'Nguyễn Văn Hùng', 'nguyen van hung', '1958-09-19', 'MALE', '0935111222', NULL,
     '102 Trần Phú, Đà Nẵng', 'CCCD', '048058007890', NULL, NULL, NULL, 'TEMPORARY', NULL),
    -- Một vụ gộp đã hoàn tất: hồ sơ 10 (tạo tại quầy, không giấy tờ) đã gộp vào hồ sơ 9
    (9, NULL, 2, 'Lê Văn Tâm', 'le van tam', '1990-02-02', 'MALE', '0905777888', 'tam.le@example.com',
     '20 Bạch Đằng, Đà Nẵng', 'CCCD', '048090002222', NULL, NULL, NULL, 'ACTIVE', NULL),
    (10, NULL, 2, 'Le Van Tam', 'le van tam', '1990-02-02', 'MALE', '0905777888', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'MERGED', 9),
    -- Khách lễ tân tạo tại quầy (11–17: hồ sơ giữ lại, có CCCD)
    (11, NULL, 2, 'Trần Thị Mỹ', 'tran thi my', '1995-04-10', 'FEMALE', '0905200011', 'my.tran@example.com',
     '15 Hoàng Diệu, Đà Nẵng', 'CCCD', '048195000011', NULL, NULL, NULL, 'ACTIVE', NULL),
    (12, NULL, 2, 'Phan Văn Đức', 'phan van duc', '1979-08-21', 'MALE', '0905200012', NULL,
     '30 Ông Ích Khiêm, Đà Nẵng', 'CCCD', '048079000012', NULL, NULL, NULL, 'ACTIVE', NULL),
    (13, NULL, 2, 'Đỗ Thị Thu', 'do thi thu', '1988-12-03', 'FEMALE', '0905200013', 'thu.do@example.com',
     '7 Phan Châu Trinh, Đà Nẵng', 'CCCD', '048188000013', NULL, NULL, NULL, 'ACTIVE', NULL),
    (14, NULL, 2, 'Huỳnh Văn Lộc', 'huynh van loc', '1965-06-30', 'MALE', '0905200014', NULL,
     '88 Điện Biên Phủ, Đà Nẵng', 'CCCD', '048065000014', NULL, NULL, NULL, 'ACTIVE', NULL),
    (15, NULL, 2, 'Ngô Thị Hạnh', 'ngo thi hanh', '2001-02-14', 'FEMALE', '0905200015', 'hanh.ngo@example.com',
     '21 Nguyễn Chí Thanh, Đà Nẵng', 'CCCD', '048201000015', NULL, NULL, NULL, 'ACTIVE', NULL),
    (16, NULL, 2, 'Võ Minh Quân', 'vo minh quan', '1992-10-05', 'MALE', '0905200016', 'quan.vo@example.com',
     '5 Lý Thường Kiệt, Đà Nẵng', 'CCCD', '048092000016', NULL, NULL, NULL, 'ACTIVE', NULL),
    (17, NULL, 2, 'Bùi Thị Ngọc', 'bui thi ngoc', '1983-03-27', 'FEMALE', '0905200017', NULL,
     '64 Hải Phòng, Đà Nẵng', 'CCCD', '048183000017', NULL, NULL, NULL, 'ACTIVE', NULL),
    -- Hồ sơ phát sinh ở quầy (không email): 18–22 đã gộp vào 11–15; 23, 24 đã gộp rồi tách lại nên về TEMPORARY
    (18, NULL, 2, 'Tran Thi My', 'tran thi my', '1995-04-10', 'FEMALE', '0905200011', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'MERGED', 11),
    (19, NULL, 2, 'Phan Van Duc', 'phan van duc', '1979-08-21', 'MALE', '0905200012', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'MERGED', 12),
    -- Cùng CCCD với hồ sơ 13, ngày sinh gõ nhầm (tình huống 2)
    (20, NULL, 2, 'Đỗ Thị Thu', 'do thi thu', '1988-12-13', 'FEMALE', '0905200013', NULL,
     NULL, 'CCCD', '048188000013', NULL, NULL, NULL, 'MERGED', 13),
    (21, NULL, 2, 'Huynh Van Loc', 'huynh van loc', '1965-06-30', 'MALE', '0905200014', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'MERGED', 14),
    (22, NULL, 2, 'Ngô Thị Hạnh', 'ngo thi hanh', '2001-02-14', 'FEMALE', '0905200015', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'MERGED', 15),
    (23, NULL, 2, 'Vo Minh Quan', 'vo minh quan', '1992-10-05', 'MALE', '0905200016', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'TEMPORARY', NULL),
    (24, NULL, 2, 'Bui Thi Ngoc', 'bui thi ngoc', '1983-03-27', 'FEMALE', '0905200017', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL, 'TEMPORARY', NULL);

-- Hàng chờ duyệt: 2 cặp đang chờ, 1 cặp đã gộp. score tính theo MatchScorer (40 giấy tờ, 25 tên, 20 ngày sinh, 10 SĐT, 5 giới tính)
INSERT IGNORE INTO duplicate_candidates (id, patient_id, candidate_patient_id, reason, score, status,
                                         reviewed_by_account_id, reviewed_at, review_note)
VALUES (1, 3, 1, 'SAME_NAME_DOB_PHONE', 60.00, 'PENDING', NULL, NULL, NULL),
       (2, 8, 7, 'SAME_ID_DIFFERENT_INFO', 80.00, 'PENDING', NULL, NULL, NULL),
       (3, 10, 9, 'SAME_NAME_DOB_PHONE', 60.00, 'MERGED', 2, NOW() - INTERVAL 14 DAY,
        'Cùng người, lần trước đến quầy quên CCCD'),
       -- 4–8 đã gộp (reviewed_at = lúc gộp); 9, 10 gộp nhầm rồi tách nên NOT_DUPLICATE (reviewed_at = lúc tách, CS24)
       (4, 18, 11, 'SAME_NAME_DOB_PHONE', 60.00, 'MERGED', 2, NOW() - INTERVAL 13 DAY,
        'Cùng người, lần đầu đến quầy chưa mang CCCD'),
       (5, 19, 12, 'SAME_NAME_DOB_PHONE', 60.00, 'MERGED', 2, NOW() - INTERVAL 12 DAY, NULL),
       (6, 20, 13, 'SAME_ID_DIFFERENT_INFO', 80.00, 'MERGED', 2, NOW() - INTERVAL 12 DAY + INTERVAL 1 HOUR,
        'Cùng CCCD, ngày sinh gõ nhầm'),
       (7, 21, 14, 'SAME_NAME_DOB_PHONE', 60.00, 'MERGED', 2, NOW() - INTERVAL 11 DAY, NULL),
       (8, 22, 15, 'SAME_NAME_DOB_PHONE', 60.00, 'MERGED', 2, NOW() - INTERVAL 10 DAY, NULL),
       (9, 23, 16, 'SAME_NAME_DOB_PHONE', 60.00, 'NOT_DUPLICATE', 2, NOW() - INTERVAL 9 DAY + INTERVAL 3 HOUR,
        'Hai người khác nhau, trùng tên, ngày sinh và dùng chung SĐT công ty'),
       (10, 24, 17, 'SAME_NAME_DOB_PHONE', 60.00, 'NOT_DUPLICATE', 2, NOW() - INTERVAL 8 DAY,
        'Hai người khác nhau, lễ tân gộp nhầm');

INSERT IGNORE INTO merge_history (id, action, source_patient_id, target_patient_id, source_previous_status,
                                  duplicate_candidate_id, related_merge_id, event_id, performed_by_account_id,
                                  performed_at, note)
VALUES (1, 'MERGE', 10, 9, 'TEMPORARY', 3, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000001', 2,
        NOW() - INTERVAL 14 DAY, 'Cùng người, lần trước đến quầy quên CCCD'),
       -- event_id có đuôi = id; Scheduling ghi cùng event_id vào processed_events, patient_reassignments
       (2, 'MERGE', 18, 11, 'TEMPORARY', 4, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000002', 2,
        NOW() - INTERVAL 13 DAY, 'Cùng người, lần đầu đến quầy chưa mang CCCD'),
       (3, 'MERGE', 19, 12, 'TEMPORARY', 5, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000003', 2,
        NOW() - INTERVAL 12 DAY, NULL),
       (4, 'MERGE', 20, 13, 'TEMPORARY', 6, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000004', 2,
        NOW() - INTERVAL 12 DAY + INTERVAL 1 HOUR, 'Cùng CCCD, ngày sinh gõ nhầm'),
       (5, 'MERGE', 21, 14, 'TEMPORARY', 7, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000005', 2,
        NOW() - INTERVAL 11 DAY, NULL),
       (6, 'MERGE', 22, 15, 'TEMPORARY', 8, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000006', 2,
        NOW() - INTERVAL 10 DAY, NULL),
       (7, 'MERGE', 23, 16, 'TEMPORARY', 9, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000007', 2,
        NOW() - INTERVAL 10 DAY + INTERVAL 2 HOUR, NULL),
       (8, 'MERGE', 24, 17, 'TEMPORARY', 10, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000008', 2,
        NOW() - INTERVAL 9 DAY, NULL),
       -- Tách hai vụ gộp nhầm: related_merge_id trỏ về lần gộp bị hoàn tác
       (9, 'UNMERGE', 23, 16, 'TEMPORARY', 9, 7, '7c1e5a2e-3b1f-4d6a-9c1e-000000000009', 2,
        NOW() - INTERVAL 9 DAY + INTERVAL 3 HOUR, 'Hai người khác nhau, trùng tên, ngày sinh và dùng chung SĐT công ty'),
       (10, 'UNMERGE', 24, 17, 'TEMPORARY', 10, 8, '7c1e5a2e-3b1f-4d6a-9c1e-000000000010', 2,
        NOW() - INTERVAL 8 DAY, 'Hai người khác nhau, lễ tân gộp nhầm');
