# Nền tảng đặt lịch khám đa khoa — Microservices

Đồ án môn Kiến trúc hướng dịch vụ, **Hướng 4: Lập lịch & xử lý ràng buộc thời gian**. Bệnh nhân đặt lịch khám theo chuyên khoa – bác sĩ – khung giờ; hệ thống chống đặt trùng slot, tự nhắc lịch trước giờ khám, và quản lý định danh bệnh nhân (MPI) để phát hiện, gộp hồ sơ trùng khi có lễ tân duyệt.

```
                     Web (Angular) — http://localhost:4200
                                   │
                                   ▼
          API Gateway (8080): định tuyến, JWT, API Composition
             │          │          │           │           │
             ▼          ▼          ▼           ▼           ▼
           Auth     Provider    Patient    Scheduling   Reminder
                                 (MPI) ──RabbitMQ──►       (cron → email)
             │          │          │           │           │
          auth_db  provider_db patient_db scheduling_db reminder_db

          discovery-server (Eureka): danh bạ service
```

## Chạy nhanh

Cần Docker Desktop và Git; đặt repo ở **thư mục không dấu**.

```powershell
git clone <địa chỉ repo> D:\SOA\dat-lich-kham
cd D:\SOA\dat-lich-kham
docker compose up -d --build
```

Chờ `docker compose ps` báo đủ 11 container `healthy` (lần đầu khoảng 2,5 phút cộng thời gian tải image), rồi mở **http://localhost:4200**. Tài khoản mẫu: `admin`, `letan`, `bs.an`, `bs.binh`, `benhnhan` — mật khẩu `123456`.

Hướng dẫn đầy đủ (yêu cầu máy, giới hạn RAM cho Docker, kết nối cơ sở dữ liệu, chế độ phát triển, xử lý sự cố): [docs/01](docs/01-huong-dan-cai-dat-va-chay.md).

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [01 — Hướng dẫn cài đặt và chạy](docs/01-huong-dan-cai-dat-va-chay.md) | Yêu cầu phần cứng / phần mềm, các bước khởi chạy, địa chỉ truy cập, kết nối CSDL, xử lý sự cố |
| [02 — Mô tả đề tài và hệ thống](docs/02-mo-ta-de-tai-va-he-thong.md) | Yêu cầu đề tài, bài toán, kiến trúc, đối chiếu giáo trình, các service, luồng chính, hiện trạng, 5 phần việc |
| [03 — Thiết kế cơ sở dữ liệu](docs/03-thiet-ke-csdl.md) | Bảng của 5 service, ràng buộc chống trùng, dữ liệu mẫu |
| [04 — Các việc cần triển khai](docs/04-cac-viec-can-trien-khai.md) | Việc của từng phần, tiêu chí hoàn thành, mục giáo trình liên quan |
| [05 — Gộp mã nguồn MPI vào Patient Service](docs/05-gop-code-mpi-vao-patient-service.md) | Phần đã gộp, phần không gộp, đối chiếu dữ liệu, tình trạng giao diện |
| [06 — Danh sách API, hợp đồng giữa các service](docs/06-danh-sach-api-hop-dong-giua-cac-service-can-tuan-thu.md) | Quy tắc chung, JWT, sự kiện RabbitMQ, API từng service |

## Cấu trúc repo

```
dat-lich-kham/
├── discovery-server/        Eureka — danh bạ service
├── api-gateway/             Cổng vào, định tuyến, API Composition
├── auth-service/            Tài khoản, JWT
├── provider-service/        Chuyên khoa, bác sĩ, phòng, ca làm việc
├── patient-service/         Hồ sơ bệnh nhân, MPI, phát sự kiện gộp / tách
├── scheduling-service/      Slot, giữ chỗ, đặt / hủy lịch, nhận sự kiện
├── reminder-service/        Cron gửi nhắc lịch
├── frontend/                Web Angular 18
├── docker/mysql/init/       Script tạo 5 cơ sở dữ liệu + 5 tài khoản MySQL
├── docs/                    Tài liệu 01–06
├── Dockerfile               Build dùng chung cho các module Java
├── docker-compose.yml       Chạy toàn hệ thống
└── docker-compose.dev.yml   Mở thêm cổng từng service khi phát triển
```

Mỗi service Java có cùng bố cục: `<nghiệp vụ>/` (entity, repository, service, controller, dto), `internal/` (API nội bộ), `client/` (gọi service khác), `event/` (RabbitMQ), `common/` (lỗi chung); `resources/` gồm `application.yml`, `schema.sql`, `data.sql`.

## Nguồn gốc mã nguồn

- Hạ tầng dựa trên [Spring Petclinic Microservices](https://github.com/spring-petclinic/spring-petclinic-microservices) (Apache-2.0, xem `LICENSE`), commit `aefaf7f`. Giữ lại: Eureka, cấu trúc Gateway dùng `WebClient` gọi theo tên service + Circuit Breaker Resilience4j, Maven Wrapper, cách bố trí Docker Compose có healthcheck. Đã sửa đổi: `DiscoveryServerApplication`, `ApiGatewayApplication` (giữ nguyên thông báo bản quyền đầu file). Bỏ: Config Server, chatbot AI, bộ giám sát, giao diện AngularJS, nghiệp vụ thú y.
- Phần MPI của `patient-service` và nền giao diện `frontend/` lấy từ repo [MPI-Master-Patient-Index](https://github.com/nhugray/MPI-Master-Patient-Index) (commit `6362a13`) — `docs/05`.
- Còn lại (5 service nghiệp vụ, bảng dữ liệu, API, khung web, cấu hình Docker, tài liệu) viết mới.
