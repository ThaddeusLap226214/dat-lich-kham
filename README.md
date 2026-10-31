# Nền tảng đặt lịch khám đa khoa — Microservices

Đồ án môn Kiến trúc hướng dịch vụ, **Hướng 4: Lập lịch & xử lý ràng buộc thời gian**. Bệnh nhân đặt lịch khám theo chuyên khoa – bác sĩ – khung giờ; hệ thống chống đặt trùng slot, gửi thư xác nhận, báo hủy và nhắc lịch trước giờ khám, và quản lý định danh bệnh nhân (MPI) để phát hiện, gộp hồ sơ trùng khi có lễ tân duyệt.

```
                     Web (Angular) — http://localhost:4200
                                   │
                                   ▼
          API Gateway (8080): định tuyến, xác minh token, API Composition
             │          │          │           │           │
             ▼          ▼          ▼           ▼           ▼
           Auth     Provider    Patient    Scheduling   Reminder
             │                   (MPI)                  (thông báo → email)
          Keycloak
             │          │          │           │           │
          auth_db  provider_db patient_db scheduling_db reminder_db

          Sự kiện qua RabbitMQ: Patient → Scheduling (gộp / tách hồ sơ)
                                Scheduling → Reminder (đã xác nhận / đã hủy lịch)
          discovery-server (Eureka): danh bạ service
```

## Chạy nhanh

Cần Docker Desktop và Git; đặt repo ở **thư mục không dấu**.

```powershell
git clone <địa chỉ repo> D:\SOA\dat-lich-kham
cd D:\SOA\dat-lich-kham
docker compose up -d --build
```

Chờ `docker compose ps` báo đủ 12 container `healthy` (lần đầu khoảng 2,5 phút cộng thời gian tải image), rồi mở **http://localhost:4200**. Tài khoản mẫu: `admin`, `letan`, `bs.an`, `bs.binh`, `benhnhan` (đủ 11 tài khoản ở `docs/02-co-so-du-lieu.md` mục 5) — mật khẩu `123456`. Hướng dẫn đầy đủ: [docs/00-bat-dau-tu-day.md](docs/00-bat-dau-tu-day.md).

## Tài liệu

| File | Đọc khi |
|---|---|
| [00 — Bắt đầu từ đây](docs/00-bat-dau-tu-day.md) | **Đọc trước**: cài, chạy lần đầu, bật lại, cập nhật, bắt đầu việc của mình; phần chung đã sẵn sàng; phụ lục cho báo cáo |
| [01 — Nghiệp vụ và đặc tả chức năng](docs/01-nghiep-vu-va-dac-ta-chuc-nang.md) | Code một chức năng: chạy ra sao, theo chính sách nào |
| [02 — Cơ sở dữ liệu](docs/02-co-so-du-lieu.md) | Cần biết bảng, cột, dữ liệu mẫu, kịch bản kiểm thử |
| [03 — Hợp đồng API](docs/03-hop-dong-api.md) | Viết hoặc gọi một API, phát / nhận sự kiện |
| [04 — Việc của từng người](docs/04-viec-cua-tung-nguoi.md) | Mình làm việc gì, làm gì trước, chờ ai, khi nào là xong |

## Cấu trúc repo

```
dat-lich-kham/
├── discovery-server/        Eureka — danh bạ service
├── api-gateway/             Cổng vào, định tuyến, xác minh token, API Composition
├── auth-service/            Tài khoản (mã số, vai trò), gọi Keycloak Admin API
├── provider-service/        Chuyên khoa, bác sĩ, phòng, ca làm việc
├── patient-service/         Hồ sơ bệnh nhân, MPI, phát sự kiện gộp / tách
├── scheduling-service/      Slot, giữ chỗ, đặt / hủy lịch, nhận và phát sự kiện
├── reminder-service/        Dịch vụ thông báo: nhắc lịch, thư xác nhận, thư báo hủy
├── frontend/                Web Angular 18
├── docker/                  Script tạo database + tài khoản MySQL, realm Keycloak
├── docs/                    5 tài liệu 00–04
├── Dockerfile               Build dùng chung cho các module Java
├── docker-compose.yml       Chạy toàn hệ thống
└── docker-compose.dev.yml   Mở thêm cổng từng service khi phát triển
```

Mỗi service Java có cùng bố cục: `<nghiệp vụ>/` (entity, repository, service, controller, dto), `internal/` (API nội bộ), `client/` (gọi service khác), `event/` (RabbitMQ), `common/` (lỗi chung); `resources/` gồm `application.yml`, `schema.sql`, `data.sql`.

## Nguồn gốc mã nguồn

- Hạ tầng dựa trên [Spring Petclinic Microservices](https://github.com/spring-petclinic/spring-petclinic-microservices) (Apache-2.0, xem `LICENSE`), commit `aefaf7f` — phần nào áp dụng ở đâu: `docs/00-bat-dau-tu-day.md` mục 7.4.
- Phần chuẩn hóa tên, tính điểm giống nhau, tìm kiếm hồ sơ của `patient-service` và nền giao diện `frontend/` lấy từ repo [MPI-Master-Patient-Index](https://github.com/nhugray/MPI-Master-Patient-Index) (commit `6362a13`).
- Còn lại (5 service nghiệp vụ, bảng dữ liệu, API, khung web, cấu hình Docker, tài liệu) viết mới.
