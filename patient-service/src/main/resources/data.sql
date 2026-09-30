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
     NULL, NULL, NULL, NULL, NULL, NULL, 'MERGED', 9);

-- Hàng chờ duyệt: 2 cặp đang chờ, 1 cặp đã gộp. score tính theo MatchScorer (40 giấy tờ, 25 tên, 20 ngày sinh, 10 SĐT, 5 giới tính)
INSERT IGNORE INTO duplicate_candidates (id, patient_id, candidate_patient_id, reason, score, status,
                                         reviewed_by_account_id, reviewed_at, review_note)
VALUES (1, 3, 1, 'SAME_NAME_DOB_PHONE', 60.00, 'PENDING', NULL, NULL, NULL),
       (2, 8, 7, 'SAME_ID_DIFFERENT_INFO', 80.00, 'PENDING', NULL, NULL, NULL),
       (3, 10, 9, 'SAME_NAME_DOB_PHONE', 60.00, 'MERGED', 2, NOW() - INTERVAL 14 DAY,
        'Cùng người, lần trước đến quầy quên CCCD');

INSERT IGNORE INTO merge_history (id, action, source_patient_id, target_patient_id, source_previous_status,
                                  duplicate_candidate_id, related_merge_id, event_id, performed_by_account_id,
                                  performed_at, note)
VALUES (1, 'MERGE', 10, 9, 'TEMPORARY', 3, NULL, '7c1e5a2e-3b1f-4d6a-9c1e-000000000001', 2,
        NOW() - INTERVAL 14 DAY, 'Cùng người, lần trước đến quầy quên CCCD');
