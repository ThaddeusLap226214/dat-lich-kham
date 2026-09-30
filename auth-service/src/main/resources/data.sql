-- Tài khoản demo, mật khẩu chung là 123456 (đã băm BCrypt). Chỉ dùng khi chạy thử trên máy.
-- id cố định vì service khác dùng các mã này trong dữ liệu mẫu (Patient: 2 = lễ tân, 5 = bệnh nhân)
-- doctor_id trỏ tới bác sĩ bên Provider Service (1 = Nguyễn Văn An, 2 = Trần Thị Bình)
INSERT IGNORE INTO accounts (id, username, password_hash, full_name, email, phone, role, doctor_id) VALUES
    (1, 'admin', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Quản trị viên', 'admin@datlichkham.local', NULL, 'ADMIN', NULL),
    (2, 'letan', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Nguyễn Thị Lễ Tân', 'letan@datlichkham.local', '0905000100', 'RECEPTIONIST', NULL),
    (3, 'bs.an', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Nguyễn Văn An', 'bs.an@datlichkham.local', '0905000001', 'DOCTOR', 1),
    (4, 'bs.binh', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Trần Thị Bình', 'bs.binh@datlichkham.local', '0905000002', 'DOCTOR', 2),
    (5, 'benhnhan', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Lê Thị Hoa', 'hoa.le@example.com', '0905123456', 'PATIENT', NULL),
    (6, 'bs.cuong', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Lê Minh Cường', 'bs.cuong@datlichkham.local', '0905000003', 'DOCTOR', 3),
    (7, 'bs.dung', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Phạm Thu Dung', 'bs.dung@datlichkham.local', '0905000004', 'DOCTOR', 4),
    (8, 'bs.em', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Hoàng Văn Em', 'bs.em@datlichkham.local', '0905000005', 'DOCTOR', 5),
    (9, 'bs.giang', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Võ Thị Giang', 'bs.giang@datlichkham.local', '0905000006', 'DOCTOR', 6),
    (10, 'nam.tran', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Trần Văn Nam', 'nam.tran@example.com', '0912345678', 'PATIENT', NULL),
    (11, 'lan.pham', '$2a$10$lLLmpRErrAkR8HDiWYdMBOO47AYJ4SDaCm11Apfd1.w.L8VvhQVWy', 'Phạm Thị Lan', 'lan.pham@example.com', '0978123456', 'PATIENT', NULL);
