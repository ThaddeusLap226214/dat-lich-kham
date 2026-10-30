-- Tài khoản demo. Mật khẩu (chung là 123456) nằm ở Keycloak, xem docker/keycloak/realm-dat-lich-kham.json.
-- id cố định vì service khác dùng các mã này trong dữ liệu mẫu (Patient: 2 = lễ tân, 5 = bệnh nhân)
-- keycloak_user_id khớp "id" của người dùng trong file realm; doctor_id trỏ tới bác sĩ bên Provider (1 = Nguyễn Văn An, 2 = Trần Thị Bình)
INSERT IGNORE INTO accounts (id, username, keycloak_user_id, full_name, email, phone, role, doctor_id) VALUES
    (1, 'admin', '00000000-0000-4000-8000-000000000001', 'Quản trị viên', 'admin@datlichkham.local', NULL, 'ADMIN', NULL),
    (2, 'letan', '00000000-0000-4000-8000-000000000002', 'Nguyễn Thị Lễ Tân', 'letan@datlichkham.local', '0905000100', 'RECEPTIONIST', NULL),
    (3, 'bs.an', '00000000-0000-4000-8000-000000000003', 'Nguyễn Văn An', 'bs.an@datlichkham.local', '0905000001', 'DOCTOR', 1),
    (4, 'bs.binh', '00000000-0000-4000-8000-000000000004', 'Trần Thị Bình', 'bs.binh@datlichkham.local', '0905000002', 'DOCTOR', 2),
    (5, 'benhnhan', '00000000-0000-4000-8000-000000000005', 'Lê Thị Hoa', 'hoa.le@example.com', '0905123456', 'PATIENT', NULL),
    (6, 'bs.cuong', '00000000-0000-4000-8000-000000000006', 'Lê Minh Cường', 'bs.cuong@datlichkham.local', '0905000003', 'DOCTOR', 3),
    (7, 'bs.dung', '00000000-0000-4000-8000-000000000007', 'Phạm Thu Dung', 'bs.dung@datlichkham.local', '0905000004', 'DOCTOR', 4),
    (8, 'bs.em', '00000000-0000-4000-8000-000000000008', 'Hoàng Văn Em', 'bs.em@datlichkham.local', '0905000005', 'DOCTOR', 5),
    (9, 'bs.giang', '00000000-0000-4000-8000-000000000009', 'Võ Thị Giang', 'bs.giang@datlichkham.local', '0905000006', 'DOCTOR', 6),
    (10, 'nam.tran', '00000000-0000-4000-8000-000000000010', 'Trần Văn Nam', 'nam.tran@example.com', '0912345678', 'PATIENT', NULL),
    (11, 'lan.pham', '00000000-0000-4000-8000-000000000011', 'Phạm Thị Lan', 'lan.pham@example.com', '0978123456', 'PATIENT', NULL);
