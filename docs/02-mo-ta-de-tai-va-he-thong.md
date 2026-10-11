# 02. Mô tả đề tài và hệ thống

**Tên đề tài:** Nền tảng đặt lịch khám đa khoa dựa trên kiến trúc Microservices
**Hướng đề tài:** Hướng 4 — Hệ thống Lập lịch & Xử lý ràng buộc thời gian

Tài liệu mô tả bài toán, yêu cầu của đề tài, kiến trúc hệ thống, trách nhiệm của từng service, các luồng nghiệp vụ chính, hiện trạng của mã nguồn và cách chia hệ thống thành 5 phần việc.

## Mục lục theo nhu cầu

| Nhu cầu | Đọc mục |
|---|---|
| Hiểu đề tài yêu cầu gì | [1. Yêu cầu của đề tài](#1-yêu-cầu-của-đề-tài) · [2. Bài toán và người dùng](#2-bài-toán-và-người-dùng) · [9. Đối chiếu yêu cầu](#9-đối-chiếu-yêu-cầu-của-đề-tài) |
| Hiểu kiến trúc | [3. Kiến trúc](#3-kiến-trúc) · [4. Các service](#4-các-service) |
| Hiểu nghiệp vụ | [4.1. Quy tắc MPI](#41-patient-service--quy-tắc-định-danh-bệnh-nhân-mpi) · [4.2. Trạng thái lịch hẹn](#42-scheduling-service--trạng-thái-lịch-hẹn) · [5. Các luồng chính](#5-các-luồng-chính) |
| Biết hệ thống đang có gì, chia việc ra sao | [6. Hiện trạng hệ thống](#6-hiện-trạng-hệ-thống) · [7. Năm phần việc](#7-năm-phần-việc) |
| Biết giới hạn và nguồn tham chiếu | [8. Ngoài phạm vi](#8-ngoài-phạm-vi) · [10. Tài liệu tham chiếu](#10-tài-liệu-tham-chiếu) |

---

## 1. Yêu cầu của đề tài

### 1.1. Yêu cầu riêng của Hướng 4

| Yêu cầu | Nội dung |
|---|---|
| Trọng tâm kỹ thuật | Thuật toán tìm khoảng thời gian trống, giải quyết đụng độ lịch (conflict resolution), tác vụ chạy ngầm theo thời gian (cron job) |
| Provider/Resource Service | Quản lý bên cung cấp dịch vụ (bác sĩ) và cấu hình khung giờ khả dụng |
| Consumer Service | Quản lý thông tin người đặt lịch (bệnh nhân) |
| Scheduling Service | Logic lõi kiểm tra slot trống và giữ chỗ |
| Reminder/Cron Service | Dịch vụ độc lập, định kỳ quét cơ sở dữ liệu để gửi nhắc trước X giờ |
| Trọng tâm báo cáo | Cách thiết kế API tại API Gateway để **tổng hợp dữ liệu (API Composition)**: khi hiển thị lịch trình, Gateway gọi Provider Service lấy tên bác sĩ, gọi Scheduling Service lấy thời gian, rồi ghép lại trả cho frontend |

### 1.2. Tiêu chí đánh giá chung

1. Có **1 API Gateway** đứng trước toàn bộ hệ thống.
2. Triển khai tối thiểu **4 Core Microservices**.
3. Dùng **Docker Compose** để khởi chạy cục bộ toàn bộ hệ thống.

---

## 2. Bài toán và người dùng

### 2.1. Bài toán

Một phòng khám đa khoa có nhiều chuyên khoa, mỗi chuyên khoa có nhiều bác sĩ, mỗi bác sĩ có lịch làm việc riêng.

| Vấn đề | Cách hệ thống giải quyết |
|---|---|
| Bệnh nhân phải đến xếp hàng lấy số | Đặt lịch trực tuyến theo chuyên khoa, bác sĩ, khung giờ |
| Hai người đặt trùng một khung giờ của cùng một bác sĩ | Giữ chỗ tạm và chặn đặt trùng bằng ràng buộc của cơ sở dữ liệu |
| Bệnh nhân quên lịch hẹn | Tác vụ định kỳ tự gửi email nhắc trước giờ khám |
| Một người có nhiều hồ sơ rời rạc (đặt hộ, trẻ em chưa có CCCD, đến quầy không mang giấy tờ, gõ nhầm) | Quản lý định danh bệnh nhân — **MPI** (Master Patient Index): phát hiện hồ sơ nghi trùng và gộp hồ sơ **khi có lễ tân duyệt** |

### 2.2. Người dùng

| Vai trò | Ai | Chức năng |
|---|---|---|
| `PATIENT` | Bệnh nhân hoặc người nhà đặt hộ | Đăng ký, khai hồ sơ bệnh nhân, đặt / hủy lịch, xem lịch của mình. Một tài khoản quản lý được nhiều hồ sơ (bản thân, con, người nhà) |
| `RECEPTIONIST` | Lễ tân | Tạo hồ sơ cho bệnh nhân đến quầy, bổ sung giấy tờ, duyệt hồ sơ nghi trùng, gộp / tách hồ sơ, đặt / hủy lịch hộ |
| `DOCTOR` | Bác sĩ | Xem lịch khám trong ngày của mình, đánh dấu đã khám |
| `ADMIN` | Quản lý phòng khám | Quản lý chuyên khoa, bác sĩ, phòng, lịch làm việc, tài khoản |

---

## 3. Kiến trúc

### 3.1. Sơ đồ tổng thể

```
                           Web (Angular) — http://localhost:4200
                                        │
                                        ▼
        ┌──────────────────────────────────────────────────────────────┐
        │ API Gateway (8080): định tuyến, xác minh JWT, gắn danh tính, │
        │ Circuit Breaker, API Composition (/api/v1/composite/...)     │
        └──────────────────────────────────────────────────────────────┘
            │            │            │             │             │
            ▼            ▼            ▼             ▼             ▼
      Auth Service  Provider     Patient       Scheduling     Reminder
                    Service      Service       Service        Service
                                 (MPI)            ▲            (cron)
            │            │            │   sự kiện │               │
            │            │            └─ RabbitMQ ┘               │ email → Mailpit
            ▼            ▼            ▼             ▼             ▼
         auth_db    provider_db   patient_db   scheduling_db  reminder_db

     discovery-server (Eureka): danh bạ — mỗi service tự đăng ký, gọi nhau theo tên
```

### 3.2. Nguyên tắc thiết kế

| Nguyên tắc | Thể hiện trong hệ thống |
|---|---|
| **Một cổng vào** | Web chỉ gọi Gateway. API nội bộ giữa các service có dạng `/internal/v1/...` và không có route trên Gateway, nên không gọi được từ bên ngoài |
| **Database per Service** | Mỗi service có cơ sở dữ liệu riêng và tài khoản MySQL riêng chỉ có quyền trên cơ sở dữ liệu đó. Bảng chỉ lưu **mã** của thực thể thuộc service khác (`patient_id`, `doctor_id`), không có khóa ngoại chéo |
| **Hợp đồng trước (contract-first)** | Toàn bộ đường dẫn, dữ liệu vào/ra, mã lỗi, sự kiện được định nghĩa trong `docs/06` trước khi viết phần xử lý |
| **Giao tiếp đồng bộ khi cần kết quả ngay** | REST qua `RestClient` / `WebClient` gọi theo tên service (Eureka + LoadBalancer): Gateway ghép dữ liệu, Scheduling hỏi Provider giờ làm việc, Reminder hỏi Scheduling và Patient |
| **Giao tiếp bất đồng bộ khi chỉ cần báo tin** | Patient Service phát sự kiện "đã gộp / đã tách hồ sơ" qua RabbitMQ; Scheduling Service tự lắng nghe và cập nhật lịch hẹn |
| **Chống lỗi lan truyền** | Circuit Breaker + Fallback tại Gateway; Reminder tắt thì việc đặt lịch không bị ảnh hưởng; tin nhắn xử lý lỗi vào Dead Letter Queue |

### 3.3. Đối chiếu với giáo trình

Giáo trình: `LectureNotesSOA-new.pdf`.

| Khái niệm trong giáo trình | Trang | Thể hiện trong hệ thống |
|---|---|---|
| Standardized Service Contract, Service Versioning | 11, 21–24 | `docs/06`; đường dẫn có phiên bản `/api/v1/`; Swagger mỗi service |
| Loose Coupling, Service Autonomy | 12–13 | Không dùng chung cơ sở dữ liệu, không dùng chung code giữa các service; mỗi service tự build, tự chạy |
| Service Statelessness | 14, 55 | Service không giữ phiên đăng nhập; danh tính đi theo từng request (JWT → header) |
| Service Discoverability, Service Registry / Discovery | 14, 34–35 | Eureka (`discovery-server`), gọi nhau bằng `lb://ten-service` |
| Service Composability, API Composition | 14, 89–91, 108 | `/api/v1/composite/my-appointments`, `/api/v1/composite/doctor-agenda` tại Gateway |
| Phân rã theo Business Capability, tránh Nano Service | 17–18, 87 | 5 service ứng với 5 năng lực nghiệp vụ; MPI nằm gọn trong Patient Service thay vì tách thành nhiều service nhỏ |
| Database per Service, ERD phân mảnh, entity chỉ giữ mã | 88–89 | `docs/03` |
| Saga (Choreography) và giao dịch bù trừ | 48, 92–93 | Gộp hồ sơ → Scheduling chuyển lịch hẹn; tách hồ sơ → hoàn lại (bù trừ) |
| Eventual Consistency (nhất quán sau cùng) | 48, 86 | Lịch hẹn được chuyển sang hồ sơ mới sau khi sự kiện gộp tới Scheduling |
| Pub/Sub qua Message Broker, Dead Letter Queue | 103–107 | Exchange `patient.events`, hàng đợi `scheduling.patient-events` và DLQ |
| Idempotency (lũy đẳng) | 46, 56 | Bảng `processed_events` (không xử lý một sự kiện hai lần), `reminder_logs` (không nhắc hai lần) |
| Timeout, Retry, Circuit Breaker, Fallback | 45–46, 109, 111 | Resilience4j tại Gateway; Reminder thử lại lần quét sau |
| JWT, API Gateway làm lớp bảo mật đầu tiên, 401 / 403 | 41–43, 73–77 | Auth Service cấp JWT; Gateway xác minh, gắn danh tính; service kiểm tra quyền |
| Kiểm tra quyền trên từng bản ghi (lỗi "không kiểm tra Authorization") | 43 | Bệnh nhân chỉ xem được hồ sơ, lịch hẹn của chính mình (403) |
| CORS | 79–80 | Bộ lọc CORS tại Gateway cho web ở cổng 4200 |
| Correlation ID (truy vết request qua nhiều service) | 85 | Việc cần làm của Gateway (`docs/04`) |
| Containerization, Dockerfile | 112–115 | `Dockerfile` build nhiều giai đoạn, `docker-compose.yml` |

**Ghi chú về Auth Service.** Hệ thống cần một thành phần xác thực đóng vai trò Identity Provider (giáo trình trang 40–41); cách xây dựng do phần P4 lựa chọn. Hai phương án:

| Phương án | Mô tả | Căn cứ |
|---|---|---|
| Tự viết ở mức tối thiểu | Đăng ký, đăng nhập, cấp JWT, một vai trò mỗi tài khoản; không OAuth, không quên mật khẩu. Khung hiện tại đã có sẵn bảng `accounts`, băm BCrypt, thư viện JJWT, 5 tài khoản mẫu | Luồng Identity Provider → JWT → API Gateway → service (trang 41–43, 73–77) |
| Dùng giải pháp có sẵn (Keycloak) | Chạy Keycloak trong Docker Compose làm Identity Provider; tạo realm, vai trò, tài khoản mẫu; Gateway xác minh JWT bằng khóa công khai của Keycloak | Giáo trình xếp xác thực vào **Generic Domain** và khuyến nghị không tự xây (trang 87–88); OAuth 2.0 (trang 75–76) |

Dù chọn phương án nào, phần giao tiếp với các service khác giữ nguyên: Gateway xác minh JWT rồi gắn `X-User-Id`, `X-User-Role`, `X-Doctor-Id` (`docs/06` mục A5), nên các phần P1, P2, P3, P5 không bị ảnh hưởng.

---

## 4. Các service

| Service | Ứng với yêu cầu Hướng 4 | Trách nhiệm | Dữ liệu sở hữu |
|---|---|---|---|
| **API Gateway** | Tiêu chí 1 | Định tuyến, xác minh JWT, gắn danh tính, chặn theo vai trò, API Composition | Không có |
| **Auth Service** | (bổ sung) | Tài khoản, đăng nhập, cấp JWT | Tài khoản, mật khẩu đã băm, vai trò |
| **Provider Service** | Provider/Resource Service | Chuyên khoa, bác sĩ, phòng khám, ca làm việc hằng tuần | Chuyên khoa, bác sĩ, phòng, ca làm việc |
| **Patient Service (MPI)** | Consumer Service | Hồ sơ bệnh nhân, chống tạo trùng, hàng chờ duyệt, gộp / tách hồ sơ | Hồ sơ, cặp nghi trùng, lịch sử gộp / tách |
| **Scheduling Service** | Scheduling Service | Tìm slot trống, giữ chỗ, xác nhận, hủy, chống đặt trùng | Lịch hẹn, lịch hẹn đã chuyển hồ sơ, sự kiện đã xử lý |
| **Reminder Service** | Reminder/Cron Service | Định kỳ quét lịch hẹn sắp tới, gửi email nhắc | Nhật ký đã gửi nhắc |

### 4.1. Patient Service — quy tắc định danh bệnh nhân (MPI)

**Trạng thái hồ sơ:**

```
TEMPORARY (hồ sơ tạm) ──bổ sung giấy tờ, không trùng ai──► ACTIVE (đang dùng)
ACTIVE / TEMPORARY ──lễ tân duyệt gộp vào hồ sơ khác──► MERGED (đã gộp)
MERGED ──lễ tân tách do gộp nhầm──► trạng thái trước khi gộp
```

**Nguyên tắc cốt lõi: hệ thống không bao giờ tự gộp hai hồ sơ.** Khi phát hiện trùng, hệ thống chỉ đưa cặp hồ sơ vào hàng chờ duyệt; lễ tân quyết định.

| # | Tình huống | Hệ thống xử lý |
|---|---|---|
| 1 | Có CCCD, CCCD đã thuộc hồ sơ ACTIVE, họ tên và ngày sinh khớp | Dùng lại hồ sơ có sẵn |
| 2 | Có CCCD, CCCD đã thuộc hồ sơ ACTIVE nhưng họ tên hoặc ngày sinh không khớp | Tạo hồ sơ TEMPORARY, đưa cặp vào hàng chờ; vẫn đặt lịch được bằng hồ sơ tạm |
| 3 | Không có giấy tờ (trẻ em dưới 14 tuổi, người cao tuổi được đặt hộ, cấp cứu, quên giấy tờ) | Tạo hồ sơ TEMPORARY; nếu trùng họ tên (bỏ dấu, viết thường) + ngày sinh + số điện thoại với hồ sơ có sẵn thì đưa vào hàng chờ |
| 4 | Hồ sơ tạm được bổ sung CCCD chưa thuộc ai | Chuyển sang ACTIVE |
| 5 | Hồ sơ tạm được bổ sung CCCD đã thuộc hồ sơ ACTIVE khác | Không tự gộp; đưa cặp vào hàng chờ |

### 4.2. Scheduling Service — trạng thái lịch hẹn

```
HELD (đang giữ chỗ) ──bệnh nhân xác nhận──► CONFIRMED (đã đặt) ──đã khám──► COMPLETED
HELD ──quá thời gian giữ chỗ──► EXPIRED
CONFIRMED ──bệnh nhân / lễ tân hủy──► CANCELLED
```

Các con số nghiệp vụ (cấu hình trong `application.yml`, đổi được):

| Thông số | Giá trị | Cấu hình |
|---|---|---|
| Thời lượng một lượt khám | Theo từng ca làm việc (dữ liệu mẫu 15–30 phút) | Cột `slot_minutes` |
| Thời gian giữ chỗ | 5 phút | `scheduling.hold-minutes` |
| Được hủy trước giờ khám ít nhất | 2 giờ | `scheduling.cancel-before-hours` |
| Nhắc trước giờ khám | 24 giờ | `reminder.hours-before` |
| Chu kỳ quét nhắc lịch | 5 phút | `reminder.cron` |

---

## 5. Các luồng chính

**Luồng 1 — Đặt lịch khám**
```
Bệnh nhân chọn chuyên khoa → bác sĩ → ngày
  → Gateway → Scheduling: danh sách slot (Scheduling hỏi Provider giờ làm việc)
Bệnh nhân chọn slot, chọn hoặc khai hồ sơ
  → Gateway → Patient: tìm hoặc tạo hồ sơ theo bảng tình huống → patient_id
  → Gateway → Scheduling: giữ chỗ (HELD, hết hạn sau 5 phút)
Bệnh nhân xác nhận → CONFIRMED
```

**Luồng 2 — Hai người tranh cùng một slot**
```
A và B cùng giữ slot 08:00 của bác sĩ 1
  → Scheduling ghi lịch hẹn: ràng buộc UNIQUE trên (bác sĩ, giờ bắt đầu) chỉ cho một bản ghi thành công
  → A: HELD;  B: 409 SLOT_ALREADY_HELD → web tải lại danh sách slot
```

**Luồng 3 — Xem lịch khám của tôi (API Composition)**
```
Web: GET /api/v1/composite/my-appointments
  → Gateway gọi Scheduling: lịch hẹn (chỉ có doctor_id, room_id, patient_id, giờ)
  → Gateway gọi song song Provider (tên bác sĩ, chuyên khoa, phòng) và Patient (tên bệnh nhân)
  → Gateway ghép → một kết quả hoàn chỉnh; Provider hoặc Patient lỗi thì phần đó để trống
```

**Luồng 4 — Duyệt và gộp hồ sơ trùng (Saga kiểu Choreography)**
```
Patient phát hiện trùng → cặp hồ sơ vào hàng chờ
Lễ tân chọn "gộp hồ sơ B vào A"
  → Patient: B → MERGED, ghi lịch sử, phát sự kiện PATIENT_MERGED
  → RabbitMQ → Scheduling: chuyển lịch hẹn của B sang A, ghi lại để hoàn tác
Lễ tân tách do gộp nhầm → PATIENT_UNMERGED → Scheduling trả lịch hẹn về B (giao dịch bù trừ)
```

**Luồng 5 — Nhắc lịch**
```
Cron trong Reminder chạy mỗi 5 phút
  → Scheduling: lịch CONFIRMED trong 24 giờ tới
  → bỏ các lịch đã nhắc (nhật ký của chính Reminder)
  → Patient: email của hồ sơ → gửi email → ghi nhật ký
```

Chọn giao tiếp bất đồng bộ cho luồng 4 vì Patient không cần biết service nào đang dùng `patient_id`: nếu Scheduling tạm tắt, sự kiện nằm chờ trong hàng đợi và được xử lý khi Scheduling bật lại, thao tác gộp của lễ tân không bị ảnh hưởng.

---

## 6. Hiện trạng hệ thống

Mã nguồn hiện là **bộ khung chạy được**: mọi thành phần đã khởi động và nối với nhau, mọi bảng dữ liệu và API đã có; phần xử lý nghiệp vụ bên trong được đánh dấu để các thành viên viết tiếp.

| Thành phần | Đã có | Còn phải làm |
|---|---|---|
| Hạ tầng | Eureka, Docker Compose 11 container, MySQL 5 cơ sở dữ liệu + dữ liệu mẫu, RabbitMQ (exchange, hàng đợi, DLQ), Mailpit | — |
| API Gateway | Định tuyến 5 service, Circuit Breaker + Fallback, CORS, 3 lớp client gọi service khác | Bộ lọc JWT, chặn vai trò, API Composition, Correlation ID |
| Auth Service | Bảng, entity, API, băm BCrypt, 5 tài khoản mẫu | Đăng ký, đăng nhập, cấp JWT, quản lý tài khoản |
| Provider Service | Bảng, entity, API, truy vấn ca làm việc theo ngày, dữ liệu mẫu 6 bác sĩ / 25 ca | Toàn bộ phần xử lý |
| Patient Service | Bảng, entity, API; **tìm kiếm, xem, sửa hồ sơ, chuẩn hóa tên, tính điểm giống nhau đã chạy** (chuyển từ code MPI); bộ phát sự kiện | Tạo hồ sơ theo tình huống, hàng chờ, gộp / tách |
| Scheduling Service | Bảng có ràng buộc chống đặt trùng, entity, API, client gọi Provider, bộ nhận sự kiện | Tính slot, giữ chỗ, xác nhận, hủy, xử lý sự kiện |
| Reminder Service | Bảng có ràng buộc chống nhắc lặp, entity, cron, client gọi Scheduling / Patient | Phần quét và gửi email |
| Web | Khung chung: 5 vai trò, menu theo vai trò, trang chủ kiểm tra tình trạng, 14 trang khung có nút gọi thử API; trang hồ sơ bệnh nhân từ code MPI (đã khớp hợp đồng API: danh sách, tìm, lọc, sửa) | Giao diện thật của từng trang |

Chỗ chờ viết trong backend ném `NotImplementedYetException("Chưa làm: …")`; gọi vào sẽ nhận HTTP **501** kèm mô tả việc cần làm.

---

## 7. Năm phần việc

Hệ thống được chia thành 5 phần việc, mỗi phần gồm một service backend và các trang web tương ứng, để mỗi thành viên làm trọn một lát từ cơ sở dữ liệu tới giao diện.

| Phần | Nội dung | Người nhận | Mức độ | Đọc thêm trong tài liệu này |
|---|---|---|---|---|
| **P1** | Patient Service (MPI) + trang hồ sơ, hàng chờ duyệt | Như | Nặng — đã có một phần từ code MPI | [4.1. Quy tắc MPI](#41-patient-service--quy-tắc-định-danh-bệnh-nhân-mpi); [mục 5](#5-các-luồng-chính) luồng 1, 4 |
| **P2** | Scheduling Service + trang đặt lịch, lịch hẹn theo bác sĩ | Vũ | Nặng nhất — trọng tâm Hướng 4 (slot trống, đụng độ lịch) | [4.2. Trạng thái lịch hẹn](#42-scheduling-service--trạng-thái-lịch-hẹn); [mục 5](#5-các-luồng-chính) luồng 1, 2, 4 |
| **P3** | Provider Service + trang chuyên khoa / bác sĩ, quản trị danh mục, lịch làm việc, lịch khám của bác sĩ | Thành viên tự chọn | Vừa — backend nhẹ nên nhận nhiều trang web hơn | [Mục 5](#5-các-luồng-chính) luồng 1 (giờ làm việc), 3 (tên bác sĩ, phòng) |
| **P4** | Auth Service + API Gateway + trang đăng nhập, đăng ký, lịch khám của tôi, quản lý tài khoản | Lập | Nặng — trọng tâm báo cáo Hướng 4 (API Composition) | [3.3. Ghi chú về Auth Service](#33-đối-chiếu-với-giáo-trình); [mục 5](#5-các-luồng-chính) luồng 3 |
| **P5** | Reminder Service + trang nhật ký nhắc lịch + kiểm thử toàn hệ thống | Thành viên tự chọn | Vừa | [Mục 5](#5-các-luồng-chính) luồng 5 |

**Phụ thuộc giữa các phần:**

```
P3 Provider ──giờ làm việc──► P2 Scheduling ──lịch hẹn sắp tới──► P5 Reminder
     │                              ▲                                  │
     └──bác sĩ, phòng──► P4 Gateway (ghép dữ liệu) ◄──bệnh nhân── P1 Patient ◄──┘ (email)

P1 Patient ──sự kiện gộp / tách (RabbitMQ)──► P2 Scheduling
P4 Auth ──JWT──► mọi phần (trước khi hoàn thành: dùng Chế độ thử / header gửi tay)
```

Danh sách việc cụ thể của từng phần: `docs/04`.

---

## 8. Ngoài phạm vi

- Thanh toán, bảo hiểm y tế; hồ sơ bệnh án, đơn thuốc, kết quả xét nghiệm.
- Ứng dụng di động, SMS thật (email gửi vào hộp thư giả lập Mailpit).
- Triển khai lên máy chủ thật (chỉ chạy cục bộ bằng Docker Compose).
- So khớp hồ sơ gần đúng / học máy; trao đổi dữ liệu với bệnh viện khác theo chuẩn HL7 FHIR.
- Tự động dời lịch khi bác sĩ nghỉ đột xuất; danh sách chờ (waitlist).

---

## 9. Đối chiếu yêu cầu của đề tài

| Yêu cầu | Đáp ứng |
|---|---|
| 1 API Gateway trước toàn hệ thống | `api-gateway` — mục 3, 4 |
| Tối thiểu 4 Core Microservices | 5 service: Auth, Provider, Patient, Scheduling, Reminder |
| Docker Compose chạy toàn hệ thống | `docker-compose.yml`: 11 container — `docs/01` |
| Hướng 4: Provider, Consumer, Scheduling, Reminder | Provider Service, Patient Service, Scheduling Service, Reminder Service |
| Hướng 4: tìm slot trống, đụng độ lịch | Scheduling Service — luồng 1, 2 |
| Hướng 4: tác vụ chạy ngầm định kỳ | Reminder Service — luồng 5 |
| Hướng 4: API Composition tại Gateway | Luồng 3 |

---

## 10. Tài liệu tham chiếu

| Nguồn | Nội dung |
|---|---|
| Đề tài và tiêu chí chấm (`detai.pdf`) | Mục 1 |
| Giáo trình `LectureNotesSOA-new.pdf` | Mục 3.3 (số trang ghi trong bảng) |
| Luật Căn cước số 26/2023/QH15, hiệu lực 01/07/2024 | Tuổi bắt buộc làm thẻ căn cước (14 tuổi) — tình huống 3 |
| Spring Petclinic Microservices (Apache-2.0), commit `aefaf7f` | Mã nguồn nền của hạ tầng |
| Repo MPI-Master-Patient-Index, commit `6362a13` | Nguồn phần MPI của Patient Service và giao diện ban đầu — `docs/05` |
