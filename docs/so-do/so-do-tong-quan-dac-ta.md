# Đặc tả sơ đồ kiến trúc tổng thể — Nền tảng đặt lịch khám đa khoa

**Dùng để làm gì:** mô tả *cái được vẽ* trên `so-do-tong-quan.png` / `.svg`: mỗi khối là gì, ai phụ trách, nối với
khối nào, truyền gì. Hình được vẽ lại bằng `python docs/so-do/ve_so_do_tong_quan.py`.

**Căn cứ:** `docker-compose.yml`, `docker-compose.dev.yml`, `docker/mysql/init/01-create-databases.sh`, các
`application.yml`, lớp client gọi `/internal/v1/...`, cấu hình RabbitMQ trong code; `docs/02` (vai trò, luồng mục 5,
phân công mục 7), `docs/06` (mục A5, A7, B6); `README.md` mục "Nguồn gốc mã nguồn".

> **Quyết định mới (19/10/2026) thay thế:** xác thực dùng **Keycloak** (phần mềm quản lý danh tính mã nguồn mở) thay cho
> tự viết. Keycloak giữ tài khoản, vai trò, cấp JWT; Gateway xác minh JWT rồi gắn header danh tính chuyển vào service.
> Người phụ trách P4 chốt cùng ngày (`docs/07` mục *Quyết định và câu còn mở*): Auth Service **thu hẹp** — đăng ký, quản lý tài khoản, cấp `accountId`, gọi Keycloak Admin API; Keycloak lưu ở `keycloak_db` trong MySQL chung, cổng 8180. Keycloak **chưa có** trong `docker-compose.yml`.

Cách đọc hình (giống sơ đồ tổng quan môn Cơ sở HTTT): **vị trí trên/dưới = tầng; màu = nguồn gốc**.

---

## 1. Ký hiệu

| Ký hiệu | Nghĩa |
|---|---|
| Xám | Cửa vào: người dùng theo vai trò |
| Xanh dương | **Có sẵn từ khung Spring Petclinic Microservices** (giữ lại, chỉ sửa cấu hình) |
| Cam | **Nhóm tự viết** (service nghiệp vụ, web, phần lọc JWT và API Composition ở Gateway) |
| Xanh lá | **Phần mềm có sẵn, mượn ngoài** (chạy bằng image Docker có sẵn) |
| Vàng | Hạ tầng |
| Mũi tên liền | Gọi HTTP đồng bộ (REST), mũi tên từ bên gọi → bên được gọi; nhãn = dữ liệu đi qua |
| Mũi tên nét đứt | Bất đồng bộ qua RabbitMQ |

Khối nào thuộc Petclinic, khối nào nhóm viết — kiểm theo code:
- `discovery-server/.../DiscoveryServerApplication.java`, `api-gateway/.../ApiGatewayApplication.java` còn thông báo bản
  quyền Petclinic (Apache-2.0) ở đầu file; `README.md` dòng 68: giữ Eureka, cấu trúc Gateway (`WebClient` gọi theo tên +
  Circuit Breaker Resilience4j), cách bố trí Docker Compose.
- `api-gateway/.../composite/*`, `web/CorsConfig.java`, `web/FallbackController.java` và 5 service nghiệp vụ không có
  thông báo bản quyền Petclinic → nhóm viết. Web Angular và phần MPI của Patient Service lấy từ repo MPI của nhóm (`docs/05`).

## 2. Bố cục (từ trên xuống)

```
Tiêu đề
Clients (C1: các vai trò)
Keycloak (N1, góc trái, ngay trên Auth) ◄── Web Angular (C2)                     Eureka (N2, góc phải)
  ▲ khóa công khai      → API Gateway (C3 = C3a định tuyến | C3b xác minh JWT + vai trò | C3c API Composition)
  ▲ tạo tài khoản       → thanh nhánh xuống 5 service: Auth (S5) | Provider (S1) | Scheduling (S3) | Patient (S2) | Reminder (S4)
  → giữa thanh nhánh và hàng service: RabbitMQ (N3) | Email — Mailpit (N4), đặt tránh các nhánh xuống
  → hàng service đặt sát MySQL; đường service ↔ service nằm giữa hàng service và MySQL
Dữ liệu: MySQL (D1) chứa 6 database; mỗi service và Keycloak có một đường xuống database của mình
Hạ tầng: Docker Compose (H1) | mạng nội bộ (H2) | cổng mở ra máy (H3)
Chú thích màu + kiểu đường
```

## 3. Thành phần

### 3.1. Cửa vào (C)

| Mã | Tên trên hình | Vai trò một dòng | Phụ trách | Căn cứ |
|---|---|---|---|---|
| C1 | Clients: Bệnh nhân / người nhà · Lễ tân · Bác sĩ · Quản trị phòng khám · Khách chưa đăng nhập | Người dùng theo vai trò `PATIENT`, `RECEPTIONIST`, `DOCTOR`, `ADMIN` (+ `GUEST` chưa đăng nhập) | — | `docs/02` mục 2.2; `frontend/src/app/core/identity.service.ts` |
| C2 | **Web Angular :4200** | Giao diện một trang, menu theo vai trò; nginx chỉ phục vụ file tĩnh, trình duyệt gọi thẳng Gateway | Mỗi phần P1–P5 làm trang của mình | `docker-compose.yml` (`frontend`, 4200:80); `frontend/nginx.conf`; `environment.ts` (`apiUrl = http://localhost:8080/api/v1`); `core/app-pages.ts` |
| C3 | **API Gateway :8080** | Cửa vào duy nhất của backend | P4 | `api-gateway/src/main/resources/application.yml` |
| C3a | Định tuyến + Circuit Breaker | Chuyển `/api/v1/**` tới service theo tên (`lb://`), Circuit Breaker riêng từng route (đã chốt), chờ tối đa 5 giây + Fallback, CORS | Khung Petclinic (route, CORS do nhóm cấu hình) | `application.yml` (5 route), `ApiGatewayApplication.java`, `CorsConfig.java` |
| C3b | Xác minh JWT + vai trò (P4) | Xác minh JWT bằng khóa công khai Keycloak (không gọi Keycloak cho từng request), chặn thô theo vai trò, xóa header giả rồi gắn `X-User-Id`, `X-User-Role`, `X-Doctor-Id`, `X-Correlation-Id` | P4 — **chưa có code** | `docs/06` mục A5; quyết định 19/10 |
| C3c | API Composition (P4) | `/api/v1/composite/my-appointments`, `/doctor-agenda`: ghép lịch hẹn (Scheduling) + tên bác sĩ, phòng (Provider) + tên bệnh nhân (Patient) | P4 — đã có 3 lớp client, phần ghép đang trả 501 | `composite/CompositeController.java`, `SchedulingClient`, `ProviderClient`, `PatientClient`; `docs/06` B6 |

### 3.2. Service nghiệp vụ (S) — màu cam

| Mã | Tên trên hình | Vai trò một dòng | Phụ trách | Căn cứ |
|---|---|---|---|---|
| S1 | **Provider Service :8082** | Chuyên khoa, bác sĩ, phòng, ca làm việc hằng tuần | P3 | `provider-service/.../application.yml`; `ProviderInternalController` |
| S2 | **Patient Service (MPI) :8083** | Hồ sơ bệnh nhân, phát hiện nghi trùng, gộp / tách; phát sự kiện | P1 | `patient-service/.../application.yml`; `event/PatientEventPublisher.java` |
| S3 | **Scheduling Service :8084** | Tìm slot trống, giữ chỗ, xác nhận, hủy, chống đặt trùng; nhận sự kiện gộp / tách | P2 | `scheduling-service/.../application.yml`; `client/ProviderClient.java`; `event/PatientEventListener.java` |
| S4 | **Reminder Service :8085** | Cron mỗi 5 phút (`0 */5 * * * *`) quét lịch 24 giờ tới, gửi email nhắc | P5 | `reminder-service/.../application.yml`; `client/SchedulingClient.java`, `client/PatientClient.java` |
| S5 | **Auth Service :8081** | Thu hẹp (đã chốt 19/10): đăng ký, quản lý tài khoản, cấp `accountId` dạng số, gọi Keycloak Admin API; không kiểm mật khẩu, không cấp JWT | P4 | `docker-compose.yml` (`auth-service`); route `/api/v1/auth/**`, `/api/v1/accounts/**` |

### 3.3. Dịch vụ nền (N)

| Mã | Tên trên hình | Vai trò một dòng | Phụ trách | Căn cứ |
|---|---|---|---|---|
| N1 | **Keycloak** (xanh lá) | Giữ tài khoản, vai trò; cấp JWT; công bố khóa công khai để Gateway xác minh | P4 | Quyết định 19/10/2026; `docs/02` mục 3.3 (phương án Keycloak). **Chưa có trong compose** |
| N2 | **Discovery Server (Eureka) :8761** (xanh dương) | Danh bạ: mọi service tự đăng ký tên; Gateway, Scheduling, Reminder tra địa chỉ để gọi `http://ten-service/...` | Khung có sẵn, không ai phải viết thêm | `discovery-server/.../application.yml`; `EUREKA_URL` trong compose |
| N3 | **RabbitMQ :5672** (xanh lá) | Exchange `patient.events` (topic) → hàng đợi `scheduling.patient-events` (nhận `patient.*`); lỗi → `patient.events.dlx` → `scheduling.patient-events.dlq` | P1 phát, P2 nhận | `rabbitmq:4-management`; `patient/event/Messaging.java`, `scheduling/event/RabbitConfig.java` |
| N4 | **Email — Mailpit (SMTP :1025)** (xanh lá) | Hộp thư giả lập: nhận email nhắc lịch, xem thư ở web `:8025`; không gửi thư ra Internet | P5 dùng | `axllent/mailpit`; `MAIL_HOST: mailpit`, `MAIL_PORT: 1025` |

Không có thành phần quan sát (Zipkin, Prometheus, Grafana, Admin server): `README.md` ghi đã bỏ "bộ giám sát" của Petclinic;
tìm trong `pom.xml`, `*.yml`, `*.java` không thấy.

### 3.4. Dữ liệu (D) — xanh lá

| Mã | Tên trên hình | Vai trò | Phụ trách | Căn cứ |
|---|---|---|---|---|
| D1 | **MySQL 8.4** | Một container, cổng máy 3307; tạo 6 database + 6 tài khoản (5 của service + `keycloak_db` của Keycloak), mỗi tài khoản chỉ có quyền trên database của mình | Chung | `docker-compose.yml` (`mysql`); `01-create-databases.sh` |
| D2 | `provider_db` | Dữ liệu của S1 | P3 | `provider-service/.../application.yml` |
| D3 | `patient_db` | Dữ liệu của S2 | P1 | `patient-service/.../application.yml` |
| D4 | `scheduling_db` | Dữ liệu của S3 (có `processed_events`) | P2 | `scheduling-service/.../application.yml`; `docs/03` mục 3.4 |
| D5 | `reminder_db` | Nhật ký đã gửi nhắc (`reminder_logs`) | P5 | `reminder-service/.../application.yml` |
| D6 | `auth_db` | Bảng `accounts`: nguồn `accountId`, `doctorId` | P4 | `auth-service/.../application.yml` |
| D7 | `keycloak_db` | Dữ liệu của Keycloak (tài khoản, vai trò, cấu hình realm) | P4 | Đã chốt 19/10; chưa có trong `docker/mysql/init` |

### 3.5. Hạ tầng (H) — vàng

| Mã | Tên trên hình | Nội dung | Căn cứ |
|---|---|---|---|
| H1 | Docker Compose: 11 container (+ Keycloak sẽ thêm) | `mysql`, `rabbitmq`, `mailpit`, `discovery-server`, `api-gateway`, `auth-service`, `provider-service`, `patient-service`, `scheduling-service`, `reminder-service`, `frontend`; bật theo thứ tự nhờ `depends_on` + `healthcheck` | `docker-compose.yml` |
| H2 | Mạng nội bộ của Compose | Container gọi nhau bằng tên (`mysql`, `rabbitmq`, `mailpit`, `discovery-server`); service gọi nhau qua tên Eureka | `DB_HOST`, `RABBITMQ_HOST`, `MAIL_HOST`, `EUREKA_URL` trong compose |
| H3 | Cổng mở ra máy | 4200 web · 8080 gateway · 8761 Eureka · 15672 RabbitMQ quản trị · 8025 Mailpit · 3307 MySQL. Bản dev mở thêm 8081–8085, 5672, 1025 | `docker-compose.yml`, `docker-compose.dev.yml` |

## 4. Đường nối

| Mã | Từ → Đến | Nhãn trên hình | Kiểu | Căn cứ / ghi chú |
|---|---|---|---|---|
| L1 | C1 → C2 | trình duyệt | Đồng bộ (HTTP :4200) | |
| L2 | C2 → N1 | đăng nhập → nhận JWT | Đồng bộ | Theo quyết định 19/10; **chưa có code**. Web chuyển người dùng sang trang đăng nhập Keycloak là cách làm phổ biến — đã chốt 19/10 |
| L3 | C2 → C3 | REST /api/v1/** + JWT | Đồng bộ | `environment.ts`; hiện "Chế độ thử" gửi tay `X-User-*` thay JWT (`identity.interceptor.ts`) |
| L4 | C3 → N1 | khóa công khai | Đồng bộ | Gateway lấy khóa công khai để xác minh chữ ký JWT; **chưa có code** |
| L5 | C3 → S5 | /auth · /accounts | Đồng bộ | Route `auth-service` trong Gateway; `POST /api/v1/auth/register` là đường dẫn công khai |
| L6 | C3 → S1 | bác sĩ · phòng · ca | Đồng bộ | Route `/specialties`, `/doctors`, `/rooms`, `/work-schedules`; ghép: `/internal/v1/doctors?ids=`, `/internal/v1/rooms?ids=` |
| L7 | C3 → S3 | slot · lịch hẹn | Đồng bộ | Route `/slots`, `/appointments`; ghép: `/internal/v1/appointments?bookedByAccountId=` / `?doctorId=&from=&to=` |
| L8 | C3 → S2 | hồ sơ · hàng chờ trùng | Đồng bộ | Route `/patients`, `/duplicate-candidates`; ghép: `/internal/v1/patients?ids=` |
| L9 | C3 → S4 | nhật ký nhắc | Đồng bộ | Route `/reminders` — trang "Nhật ký nhắc lịch" của ADMIN |
| L10 | S3 → S1 | giờ làm việc theo ngày | Đồng bộ | `GET /internal/v1/doctors/{id}/working-hours?date=` |
| L11 | S4 → S3 | lịch CONFIRMED 24 giờ tới | Đồng bộ | `GET /internal/v1/appointments?status=CONFIRMED&from=&to=` |
| L12 | S4 → S2 | email của hồ sơ | Đồng bộ | `GET /internal/v1/patients?ids=` |
| L13 | S2 → N3 | PATIENT_MERGED / UNMERGED | **Bất đồng bộ** | Exchange `patient.events`, khóa `patient.merged` / `patient.unmerged` |
| L14 | N3 → S3 | chuyển lịch hẹn | **Bất đồng bộ** | Hàng đợi `scheduling.patient-events`; lỗi → DLQ |
| L15 | S4 → N4 | email nhắc lịch | Đồng bộ (SMTP) | `mailpit:1025` |
| L16 | C3, S1–S5 → N2 | *(không vẽ)* | Đồng bộ | Mọi service đăng ký với Eureka; vẽ ra thì rối — ghi thành dòng chú thích dưới hình |
| L18 | S5 → N1 | tạo tài khoản (Admin API) | Đồng bộ | Chỉ Auth Service giữ tài khoản quản trị Keycloak (`docs/07` R23) |
| L17 | S1–S5 → D2–D6 | *(không nhãn)* | Đồng bộ (JDBC) | Mỗi service một đường xuống database của mình; không service nào đọc database của service khác. Đường xuống `patient_db` cắt L11 — không tránh được vì L11 chạy ngang qua cả cột Patient |
| L19 | N1 → D7 | *(không nhãn)* | Đồng bộ (JDBC) | Keycloak lưu dữ liệu ở `keycloak_db` trong MySQL chung |

Không có đường Gateway → Patient → Scheduling theo kiểu "điều phối": ở luồng đặt lịch, web gọi lần lượt từng API,
Gateway chỉ chuyển tiếp. Gateway chỉ tự gọi nhiều service ở API Composition (C3c).

## 5. Kịch bản đi thử trên hình

| # | Kịch bản | Chuỗi mã trên hình | Phát hiện → đã vá |
|---|---|---|---|
| K1 | **Bệnh nhân đặt lịch** (`docs/02` luồng 1, 2) | C1 → C2 → N1 đăng nhập (L2) → C2 → C3 (L3; C3b xác minh với khóa N1 qua L4, gắn `X-User-*`) → S1 danh sách bác sĩ (L6) → S3 danh sách slot (L7) → S3 hỏi S1 giờ làm việc (L10) → S2 tìm / tạo hồ sơ (L8) → S3 giữ chỗ HELD, ràng buộc UNIQUE ở D4 chặn người thứ hai → xác nhận CONFIRMED | Gateway và S3 phải biết địa chỉ S1 → cần Eureka (N2) mà không có đường → **ghi "tra tên qua Eureka" vào C3a** và dòng chú thích L16 |
| K2 | **Lễ tân gộp hồ sơ trùng, lan sang lịch hẹn** (luồng 4) | C1 (lễ tân) → C2 → C3 (L3, vai trò RECEPTIONIST) → S2 gộp B vào A (L8), ghi D3 → S2 phát `PATIENT_MERGED` lên N3 (L13) → N3 giao vào hàng đợi của S3 (L14) → S3 chuyển lịch hẹn B sang A, ghi `processed_events` ở D4. Tách nhầm → `PATIENT_UNMERGED` đi cùng đường | Tin nhắn xử lý lỗi đi đâu không thấy trên hình → **thêm "hàng đợi + DLQ" vào khối N3**. S3 tắt thì sự kiện nằm chờ ở N3, thao tác gộp của lễ tân vẫn xong |
| K3 | **Nhắc lịch qua email** (luồng 5) | Cron trong S4 (5 phút) → S3 lịch CONFIRMED 24 giờ tới (L11) → S4 bỏ lịch đã nhắc theo D5 → S2 email của hồ sơ (L12) → N4 (L15) → ghi `reminder_logs` ở D5. Quản trị xem kết quả: C1 → C2 → C3 → S4 (L9) | Trang "Nhật ký nhắc lịch" (`admin/reminders`, P5) cần đường vào S4 → **thêm nhánh L9**. Người kiểm thử xem thư ở đâu → **ghi "xem thư tại :8025" vào N4** |
| K4 | **Xem lịch khám của tôi** (API Composition, luồng 3) | C1 → C2 → C3 (L3) → C3c gọi S3 lấy lịch chỉ có mã (L7) → gọi song song S1 tên bác sĩ, phòng (L6) và S2 tên bệnh nhân (L8) → ghép → C2 | Nhánh Gateway vừa chuyển tiếp vừa phục vụ ghép → **nhãn các nhánh L6, L7, L8 ghi dữ liệu được ghép** thay cho đường dẫn |

## 6. Chỗ repo lệch với `docs/02` (phát hiện khi thu thập)

| # | Chỗ lệch | Ở đâu |
|---|---|---|
| 1 | `docs/02` mục 3.3, 4, 7 và `docs/06` mục A5 vẫn mô tả Auth Service cấp JWT (HS256, khóa chung `JWT_SECRET`); quyết định 19/10 chọn Keycloak, compose chưa có Keycloak | `docs/02`, `docs/06`, `docker-compose.yml` |
| 2 | `docs/02` mục 2.2 liệt kê 4 vai trò; mục 6 và web có 5 (thêm `GUEST` — khách chưa đăng nhập) | `docs/02` mục 2.2 vs mục 6; `identity.service.ts` |
| 3 | Sơ đồ `docs/02` mục 3.1 vẽ 5 database như 5 khối rời; thực tế là **một** container MySQL chứa 5 database | `docker-compose.yml`, `01-create-databases.sh` |
| 4 | Web còn các trang lấy từ repo MPI (`facilities`, `source-systems`, `patient-import`, `patient-masters`, `dashboard`) gọi `/api/v1/facilities`, `/imports`, `/source-systems` — Gateway không có route cho các đường này, `docs/02` không nhắc | `frontend/src/app/app.routes.ts`, `features/*/services` |
