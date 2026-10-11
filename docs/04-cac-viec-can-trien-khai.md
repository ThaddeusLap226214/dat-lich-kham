# 04. Các việc cần triển khai theo từng phần

Tài liệu liệt kê các việc còn lại của hệ thống, chia theo 5 phần việc (`docs/02` mục 7). Mỗi việc ghi rõ lớp hoặc API cần viết, tiêu chí **Xong khi** (kiểm chứng được bằng Swagger, web hoặc cơ sở dữ liệu) và mục **giáo trình** liên quan (`LectureNotesSOA-new.pdf`, số trang).

Ký hiệu mức độ:

- **★** — bắt buộc để đáp ứng yêu cầu Hướng 4 và tiêu chí đánh giá.
- **☆** — mở rộng, làm khi các việc ★ đã xong.

## Tổng quan các phần việc

| Phần | Service | Phụ trách | Các việc |
|---|---|---|---|
| [P1](#3-p1--patient-service-mpi) | Patient Service (MPI) | Như | P1-1 Hồ sơ của tôi, hồ sơ rút gọn cho service khác<br>P1-2 Tạo hồ sơ, ghi hàng chờ trùng<br>P1-3 Bổ sung giấy tờ cho hồ sơ tạm<br>P1-4 Hàng chờ duyệt hồ sơ trùng<br>P1-5 Gộp hồ sơ, phát sự kiện<br>P1-6 Tách hồ sơ, xem lịch sử<br>P1-7 Web hồ sơ bệnh nhân, hàng chờ duyệt<br>P1-8 ☆ Lưu vết xem / sửa hồ sơ |
| [P2](#4-p2--scheduling-service) | Scheduling Service | Vũ | P2-1 API đọc lịch hẹn<br>P2-2 Nhả chỗ giữ quá hạn<br>P2-3 Tính slot trống<br>P2-4 Giữ chỗ, chống đặt trùng<br>P2-5 Xác nhận, hủy, đánh dấu đã khám<br>P2-6 Nhận sự kiện gộp / tách hồ sơ<br>P2-7 Web đặt lịch, lịch hẹn theo bác sĩ<br>P2-8 ☆ Cache giờ làm việc |
| [P3](#5-p3--provider-service) | Provider Service | Chưa nhận | P3-1 Danh mục chuyên khoa, phòng, bác sĩ<br>P3-2 API nội bộ bác sĩ, phòng theo mã<br>P3-3 Giờ làm việc của bác sĩ trong ngày<br>P3-4 Quản lý ca làm việc<br>P3-5 Web danh mục, lịch làm việc<br>P3-6 Web lịch khám trong ngày của bác sĩ<br>P3-7 ☆ Cache danh mục |
| [P4](#6-p4--auth-service-và-api-gateway) | Auth Service, API Gateway | Lập | P4-1 Đăng ký<br>P4-2 Đăng nhập cấp JWT<br>P4-3 Quản lý tài khoản<br>P4-4 Bộ lọc JWT ở Gateway<br>P4-5 Chặn theo vai trò<br>P4-6 API Composition<br>P4-7 Correlation ID<br>P4-8 ☆ Giới hạn số request<br>P4-9 Web đăng nhập, đăng ký<br>P4-10 Web lịch khám của tôi, quản trị tài khoản |
| [P5](#7-p5--reminder-service-và-kiểm-thử-toàn-hệ-thống) | Reminder Service, kiểm thử | Chưa nhận | P5-1 Nhật ký nhắc lịch<br>P5-2 Quét và gửi nhắc<br>P5-3 Kiểm chứng độ bền khi tắt / bật service<br>P5-4 Web nhật ký nhắc lịch<br>P5-5 Bộ request mẫu, kịch bản demo<br>P5-6 Kiểm tra đóng gói trên máy sạch<br>P5-7 ☆ Nhắc lần hai |

Các mục chung: [Quy ước chung](#1-quy-ước-chung) · [Việc nên làm trước](#2-việc-nên-làm-trước) · [Việc chung khi hoàn thiện](#8-việc-chung-khi-hoàn-thiện).

---

## 1. Quy ước chung

### 1.1. Tìm chỗ cần viết

Mọi chỗ chờ viết trong backend ném `NotImplementedYetException("Chưa làm: …")`, gọi API sẽ nhận **501** kèm mô tả. Liệt kê theo service:

```powershell
Get-ChildItem .\scheduling-service\src -Recurse -Filter *.java | Select-String "NotImplementedYetException\("
```

| Service | Số chỗ chờ viết |
|---|---|
| auth-service | 6 |
| provider-service | 17 |
| patient-service | 10 |
| scheduling-service | 11 |
| reminder-service | 2 |
| api-gateway | 2 (API ghép dữ liệu) + bộ lọc JWT, Correlation ID viết mới |

### 1.2. Một việc được coi là xong khi

1. API không còn trả 501.
2. Gọi được **qua Gateway** (`http://localhost:8080/...`) khi chạy `docker compose up -d --build`.
3. Dữ liệu trả về và **mã lỗi** đúng `docs/06`.
4. Thỏa cột *Xong khi* của việc đó.

### 1.3. Phạm vi sửa

- Mỗi phần chỉ sửa trong thư mục service của mình và các trang web của mình (`frontend/src/app/features/...`).
- Các file dùng chung — `docker-compose.yml`, `pom.xml` gốc, `docs/03`, `docs/06`, `frontend/src/app/core/app-pages.ts` — chỉ sửa sau khi thống nhất, trong một Pull Request riêng.
- Đổi đường dẫn API, tên trường JSON, mã lỗi hay cấu trúc bảng là **đổi hợp đồng**: phải báo các phần đang dùng nó (sơ đồ phụ thuộc ở `docs/02` mục 7) và sửa tài liệu cùng lúc.

### 1.4. Git

- Nhánh `main` luôn phải chạy được `docker compose up -d --build`.
- Mỗi việc một nhánh `feature/<service>-<việc>`, ví dụ `feature/scheduling-hold-slot`; xong thì mở Pull Request vào `main`, có ít nhất một thành viên khác duyệt.
- Không commit `.env`, `target/`, `node_modules/`.

### 1.5. Dùng AI hỗ trợ viết code

Khung đã cố định công nghệ và hợp đồng; câu lệnh gửi AI cần nêu đủ các ràng buộc:

```
Dự án Spring Boot 4.0.7, Java 25, Jakarta Persistence, Lombok, MySQL 8.4.
Tôi phụ trách <tên service>, chỉ được sửa trong thư mục <tên-service>/.
Bảng dữ liệu: docs/03-thiet-ke-csdl.md. Hợp đồng API: docs/06-danh-sach-api-hop-dong-giua-cac-service-can-tuan-thu.md.
Viết phần thân phương thức <Lớp>.<phương thức> đang ném NotImplementedYetException.
- Giữ nguyên đường dẫn, tên trường DTO, mã lỗi như tài liệu.
- Lỗi nghiệp vụ: throw new BusinessException(HttpStatus.xxx, "MA_LOI", "thông báo tiếng Việt").
- Không sửa schema.sql, không thêm thư viện khi chưa thống nhất.
- Không đọc database của service khác; cần dữ liệu thì gọi API nội bộ qua lớp trong package client/.
```

Sau khi AI sinh code: đọc lại phần thay đổi (`git diff`), build lại service, thử trên Swagger rồi mới commit. Các lỗi hay gặp: viết theo Spring Boot 2/3 cũ (`javax.*` thay vì `jakarta.*`), tự đổi tên trường, tự thêm thư viện.

---

## 2. Việc nên làm trước

Các việc sau là đầu vào của phần khác; làm sớm giúp các phần làm song song mà không phải chờ.

| Việc | Phần | Phần đang chờ |
|---|---|---|
| API nội bộ giờ làm việc của bác sĩ (P3-3) | P3 | P2 tính slot trống |
| API nội bộ bác sĩ, phòng theo danh sách mã (P3-2) | P3 | P4 API Composition |
| API nội bộ hồ sơ rút gọn theo danh sách mã (P1-1) | P1 | P4 API Composition, P5 lấy email |
| API nội bộ tìm lịch hẹn (P2-1) | P2 | P4 API Composition, P5 quét lịch sắp tới |
| Đăng nhập cấp JWT (P4-2) và bộ lọc JWT ở Gateway (P4-4) | P4 | Web bỏ Chế độ thử, dùng đăng nhập thật |

Trong lúc chờ, mỗi phần vẫn thử được service của mình bằng Swagger với header `X-User-Id`, `X-User-Role` gửi tay, hoặc bằng Chế độ thử trên web.

---

## 3. P1 — Patient Service (MPI)

**Phụ trách:** Như. **Nguồn code có sẵn:** `docs/05`.

**Đã có:** tìm kiếm có phân trang, xem, sửa hồ sơ (kiểm tra quyền chủ hồ sơ), chuẩn hóa tên (`NameNormalizer`), tính điểm giống nhau (`MatchScorer`), bộ phát sự kiện (`PatientEventPublisher`), 8 unit test.

| # | Mức | Việc | Lớp / API | Xong khi | Giáo trình |
|---|---|---|---|---|---|
| P1-1 | ★ | Hồ sơ của tài khoản; hồ sơ rút gọn cho service khác | `PatientService.listMine`, `findSummaries` — `GET /api/v1/patients/mine`, `GET /internal/v1/patients?ids=` | `mine` với `X-User-Id: 5` trả 2 hồ sơ mẫu; `internal … ?ids=1,3` trả 2 bản rút gọn có `email` | Database per Service — chia sẻ dữ liệu qua API (88–89) |
| P1-2 | ★ | Tạo hồ sơ theo bảng tình huống 1–3; ghi điểm tham khảo vào hàng chờ | `PatientService.create` — `POST /api/v1/patients` | Tạo hồ sơ CCCD `048190001234` khác tên → `CREATED_TEMPORARY`, `duplicateQueued = true`, bảng `duplicate_candidates` có dòng mới có `score` | Business Capability (17); nghiệp vụ `docs/02` mục 4.1 |
| P1-3 | ★ | Bổ sung giấy tờ cho hồ sơ tạm (tình huống 4–5) | `PatientService.addIdentity` — `POST /api/v1/patients/{id}/identity` | Hồ sơ 2 bổ sung CCCD mới → ACTIVE; bổ sung CCCD của hồ sơ 1 → vẫn TEMPORARY, có cặp mới lý do `ID_ADDED_BELONGS_TO_OTHER` | Ràng buộc `uk_patients_active_identity` (`docs/03` mục 4.1) |
| P1-4 | ★ | Hàng chờ duyệt: danh sách, đếm, xác nhận không trùng | `DuplicateReviewService` — `GET /api/v1/duplicate-candidates`, `/count`, `POST …/{id}/reject` | Lễ tân thấy cặp mẫu 3–1; `reject` → `NOT_DUPLICATE`, gọi lại → 409 `CANDIDATE_ALREADY_REVIEWED` | — |
| P1-5 | ★ | Gộp hồ sơ: đổi trạng thái, ghi lịch sử, **phát sự kiện sau khi giao dịch cơ sở dữ liệu đã commit** | `MergeService.merge` — `POST /api/v1/duplicate-candidates/{id}/merge` | Gộp 3 vào 1 → hồ sơ 3 `MERGED`, `merged_into_id = 1`; RabbitMQ (cổng 15672) thấy tin nhắn trên `patient.events` | Saga Choreography (92–93); Pub/Sub, đặt tên sự kiện (103–104); Eventual Consistency (86) |
| P1-6 | ★ | Tách hồ sơ gộp nhầm và xem lịch sử | `MergeService.unmerge`, `history` — `POST /api/v1/patients/{id}/unmerge`, `GET …/merge-history` | Tách hồ sơ 3 → về trạng thái trước khi gộp; lịch sử có 2 dòng MERGE, UNMERGE; sự kiện `PATIENT_UNMERGED` có `relatedMergeEventId` | Giao dịch bù trừ (48, 92) |
| P1-7 | ★ | Web: trang *Hồ sơ bệnh nhân* đã sửa theo hợp đồng mới (danh sách, tìm, lọc, sửa — `docs/05` mục 4.1); còn trang *Hồ sơ của tôi*; trang *Hàng chờ duyệt* (so sánh hai hồ sơ, gộp / không trùng / tách) | `frontend/src/app/features/patient/…` và trang mới thay trang khung `my-patients`, `duplicate-queue` | Chọn vai trò lễ tân: tìm "le thi hoa" thấy 2 hồ sơ; gộp được từ giao diện | — |
| P1-8 | ☆ | Lưu vết ai xem / sửa hồ sơ (audit log) | Bảng mới, cần thống nhất | — | Audit và Monitoring (43); Event Sourcing — audit trail cho y tế (98) |

**Nội dung báo cáo của phần:** sơ đồ trạng thái hồ sơ; sơ đồ tuần tự luồng gộp hồ sơ qua RabbitMQ (mũi tên nét đứt cho thông điệp bất đồng bộ — giáo trình trang 106–107); ERD `patient_db`.

---

## 4. P2 — Scheduling Service

**Trọng tâm kỹ thuật của Hướng 4:** tìm slot trống, giữ chỗ, giải quyết đụng độ lịch, tác vụ nền.

| # | Mức | Việc | Lớp / API | Xong khi | Giáo trình |
|---|---|---|---|---|---|
| P2-1 | ★ | Các API đọc lịch hẹn, gồm API nội bộ cho Gateway và Reminder | `AppointmentService.get`, `listMine`, `listByDoctor`, `search` — `GET /api/v1/appointments/...`, `GET /internal/v1/appointments` | Với 14 lịch mẫu: `mine` của tài khoản 5 trả 4 lịch (mã 1, 6, 7, 13); API nội bộ lọc `status=CONFIRMED` trả 7 lịch | — |
| P2-2 | ★ | Nhả chỗ giữ quá hạn: tác vụ `@Scheduled` chạy định kỳ | `AppointmentService.expireHolds` + lớp job mới | Lịch `HELD` có `hold_expires_at` đã qua → trong vòng 30 giây thành `EXPIRED` | Tác vụ chạy ngầm (yêu cầu Hướng 4) |
| P2-3 | ★ | Tính slot trống: lấy ca làm việc từ Provider, cắt theo `slotMinutes`, đánh dấu slot đã có người hoặc đã qua giờ | `SlotService.findSlots` — `GET /api/v1/slots?doctorId=&date=` | Bác sĩ 1, thứ Hai tuần tới → 21 slot (12 sáng + 9 chiều), trong đó 08:10, 08:30, 13:30 `available = false` do đã có lịch mẫu; Provider tắt → 503 thay vì treo | Giao tiếp đồng bộ (26, 101); Timeout, Fallback (45–46) |
| P2-4 | ★ | Giữ chỗ và chống đặt trùng | `AppointmentService.hold` — `POST /api/v1/appointments/hold` | Giữ cùng một slot hai lần → lần 2 nhận **409 `SLOT_ALREADY_HELD`** (bắt `DataIntegrityViolationException` của ràng buộc `uk_appointments_slot_lock`); giờ không thuộc ca → 400 `SLOT_NOT_IN_SCHEDULE`; bệnh nhân đã có lịch chồng giờ → 409 `PATIENT_TIME_CONFLICT` | Xử lý tranh chấp đồng thời bằng ràng buộc cơ sở dữ liệu (`docs/03` mục 4.2); `POST` không lũy đẳng (56) |
| P2-5 | ★ | Xác nhận, hủy, đánh dấu đã khám theo trạng thái | `confirm`, `cancel`, `complete` — `POST /api/v1/appointments/{id}/...` | Xác nhận sau 5 phút → 409 `HOLD_EXPIRED`; bệnh nhân hủy khi còn dưới 2 giờ → 409 `TOO_LATE_TO_CANCEL`; bác sĩ khác đánh dấu → 403 | Kiểm tra quyền trên từng bản ghi (43) |
| P2-6 | ★ | Nhận sự kiện gộp / tách hồ sơ: bỏ qua sự kiện đã xử lý, chuyển lịch hẹn, hoàn lại khi tách | `PatientEventHandler.handle` | Gửi cùng một sự kiện 2 lần → lịch chỉ chuyển 1 lần; hồ sơ 3 có lịch → gộp vào 1 → lịch thuộc hồ sơ 1; tách → về hồ sơ 3. Tắt Scheduling lúc gộp, bật lại → sự kiện vẫn được xử lý | Idempotency (46); Saga + bù trừ (92–94); Message Broker làm vùng đệm (103–104); DLQ (107) |
| P2-7 | ★ | Web: trang *Đặt lịch* (chuyên khoa → bác sĩ → ngày → slot → hồ sơ → giữ chỗ có đếm ngược → xác nhận); trang *Lịch hẹn theo bác sĩ* cho lễ tân | Thay trang khung `booking`, `appointments` | Hai cửa sổ trình duyệt cùng giữ một slot → chỉ một bên thành công, bên kia được báo và thấy slot bị khóa | — |
| P2-8 | ☆ | Cache danh sách giờ làm việc của bác sĩ để giảm số lần gọi Provider | — | — | CQRS — tách luồng đọc (94–95) |

**Nội dung báo cáo của phần:** thuật toán tính slot; sơ đồ tuần tự luồng hai người tranh một slot; sơ đồ trạng thái lịch hẹn; ERD `scheduling_db`.

---

## 5. P3 — Provider Service

| # | Mức | Việc | Lớp / API | Xong khi | Giáo trình |
|---|---|---|---|---|---|
| P3-1 | ★ | Danh mục chuyên khoa, phòng, bác sĩ: xem, thêm, sửa | `CatalogService` — `GET/POST/PUT /api/v1/specialties`, `/rooms`, `/doctors` | `GET /api/v1/doctors?specialtyId=1` trả 2 bác sĩ; thêm chuyên khoa trùng mã → 409 `CODE_TAKEN` | REST: URI, phương thức, mã trạng thái (54–58) |
| P3-2 | ★ | API nội bộ bác sĩ, phòng theo danh sách mã | `findDoctorsByIds`, `findRoomsByIds` — `GET /internal/v1/doctors?ids=`, `/rooms?ids=` | `?ids=1,2` trả đúng 2 bác sĩ kèm tên chuyên khoa | API Composition — nguồn dữ liệu (89–91) |
| P3-3 | ★ | Giờ làm việc của bác sĩ trong một ngày | `WorkScheduleService.getWorkingHours` — `GET /internal/v1/doctors/{id}/working-hours?date=` | Bác sĩ 1, một ngày thứ Hai → 2 ca (07:30–11:30 và 13:30–16:30, 20 phút); Chủ nhật → danh sách rỗng | Service Contract (21–24) |
| P3-4 | ★ | Ca làm việc: xem, thêm, sửa, ngừng; chặn ca chồng giờ | `WorkScheduleService.listByDoctor`, `create`, `update`, `delete` | Thêm ca chồng giờ ca có sẵn cùng ngày → 409 `SCHEDULE_OVERLAP`; ngừng ca → `active = false`, không xóa dòng | — |
| P3-5 | ★ | Web: trang *Chuyên khoa & bác sĩ* (ai cũng xem); trang quản trị danh mục; trang quản trị lịch làm việc | Thay trang khung `doctors`, `admin/catalog`, `admin/work-schedules` | Thêm ca mới trên web → slot mới xuất hiện ở trang đặt lịch | — |
| P3-6 | ★ | Web: trang *Lịch khám trong ngày* của bác sĩ, nút "Đã khám" | Thay trang khung `doctor-agenda` (dùng API của P4-6 và P2-5) | Chọn vai trò `bs.an` thấy lịch của bác sĩ 1, bấm "Đã khám" → `COMPLETED` | — |
| P3-7 | ☆ | Cache danh sách chuyên khoa / bác sĩ (đọc nhiều, ít đổi) | `@Cacheable` | — | Cacheable — ràng buộc REST (55) |

**Nội dung báo cáo của phần:** ERD `provider_db`; cách cấu hình ca làm việc sinh ra slot.

---

## 6. P4 — Auth Service và API Gateway

**Trọng tâm báo cáo của Hướng 4:** thiết kế API Composition tại Gateway.

### 6.1. Auth Service

Cách xây dựng Auth do phần P4 chọn (`docs/02` mục 3.3). Bảng dưới mô tả phương án **tự viết** trên khung có sẵn. Nếu chọn **Keycloak**, P4-1 – P4-3 được thay bằng: thêm container Keycloak vào `docker-compose.yml`, tạo realm với 4 vai trò và 5 tài khoản mẫu như `docs/03` mục 5, đưa `role` và `doctorId` vào token; P4-4 xác minh token bằng khóa công khai của Keycloak. Tiêu chí *Xong khi* về đăng nhập, 401, 403 giữ nguyên.

| # | Mức | Việc | Lớp / API | Xong khi | Giáo trình |
|---|---|---|---|---|---|
| P4-1 | ★ | Đăng ký tài khoản bệnh nhân, băm mật khẩu BCrypt | `AccountService.register` — `POST /api/v1/auth/register` | Đăng ký mới → 201, cột `password_hash` bắt đầu bằng `$2a$`; đăng ký trùng tên → 409 `USERNAME_TAKEN` | Authentication (39); không ghi log thông tin nhạy cảm (43) |
| P4-2 | ★ | Đăng nhập, cấp JWT HS256: `sub`, `role`, `doctorId`, `exp` | `AccountService.login`, `JwtService.generateToken` — `POST /api/v1/auth/login` | Đăng nhập `benhnhan` / `123456` → `accessToken`; dán vào jwt.io thấy `sub = 5`, `role = PATIENT`, có `exp`; sai mật khẩu → 401 | JWT: cấu trúc, chữ ký, hạn dùng (41, 73–75); token phải có hạn (43) |
| P4-3 | ★ | Tài khoản của tôi, danh sách, quản trị viên tạo tài khoản | `getById`, `list`, `create` — `/api/v1/accounts...` | Tài khoản không phải ADMIN tạo tài khoản → 403; tạo DOCTOR thiếu `doctorId` → 400 | Authorization (39, 72–73) |

### 6.2. API Gateway

| # | Mức | Việc | Lớp / API | Xong khi | Giáo trình |
|---|---|---|---|---|---|
| P4-4 | ★ | **Bộ lọc JWT** (`GlobalFilter`): cho qua đường dẫn công khai; xác minh chữ ký và hạn; xóa header `X-User-*` client tự gửi; gắn `X-User-Id`, `X-User-Role`, `X-Doctor-Id`; token sai → 401 | Lớp mới trong `api-gateway/.../security/` | Không token gọi `/api/v1/patients/mine` → 401; token hợp lệ → 200; tự gửi `X-User-Id: 1` kèm token của tài khoản 5 → service vẫn nhận 5 | API Gateway là lớp bảo mật đầu tiên (42–43); kết hợp Gateway + JWT (76–77) |
| P4-5 | ★ | Chặn thô theo vai trò các API đánh dấu **(GW)** trong `docs/06` | Cùng bộ lọc hoặc bộ lọc thứ hai | Vai trò PATIENT gọi `POST /api/v1/doctors` → 403 | Authorization, 403 (73) |
| P4-6 | ★ | **API Composition** `my-appointments` và `doctor-agenda`: gọi Scheduling, rồi gọi **song song** Provider và Patient (`Mono.zip`), ghép theo mã; bọc Circuit Breaker; Provider / Patient lỗi thì vẫn trả lịch, phần thiếu để trống | `CompositeController`; dùng sẵn `SchedulingClient`, `ProviderClient`, `PatientClient` | Có lịch hẹn → JSON đủ tên bác sĩ, phòng, bệnh nhân; `docker compose stop provider-service` → vẫn trả danh sách, `doctor = null` | API Composition (89–91, 108); Circuit Breaker, Fallback (45–46, 109) |
| P4-7 | ★ | **Correlation ID**: sinh `X-Correlation-Id` cho mỗi request, chuyển xuống service; các service ghi mã này vào log | Bộ lọc Gateway + cấu hình log từng service | Một request qua Gateway → log của Gateway và service cùng một mã | Distributed Tracing, Correlation ID (85) |
| P4-8 | ☆ | Giới hạn số request (rate limiting) | `RequestRateLimiter` của Spring Cloud Gateway (cần Redis) hoặc Bucket4j | Gửi quá ngưỡng → 429 | Rate Limiting, Token Bucket (77–78) |

### 6.3. Web

| # | Mức | Việc | Xong khi |
|---|---|---|---|
| P4-9 | ★ | Trang *Đăng nhập*, *Đăng ký*; lưu token; interceptor gắn `Authorization: Bearer`; 401 → về trang đăng nhập; **bỏ Chế độ thử** (`core/identity.*`) | Đăng nhập `letan` → menu lễ tân; token hết hạn → tự về trang đăng nhập |
| P4-10 | ★ | Trang *Lịch khám của tôi* (dùng P4-6), nút hủy lịch; trang quản trị *Tài khoản* | Bệnh nhân thấy lịch kèm tên bác sĩ, phòng |

**Nội dung báo cáo của phần:** sơ đồ tuần tự API Composition (luồng 3, `docs/02` mục 5) — trọng tâm báo cáo Hướng 4; luồng đăng nhập → JWT → Gateway → service; lý do chọn phương án xây dựng Auth (`docs/02` mục 3.3).

---

## 7. P5 — Reminder Service và kiểm thử toàn hệ thống

| # | Mức | Việc | Lớp / API | Xong khi | Giáo trình |
|---|---|---|---|---|---|
| P5-1 | ★ | Nhật ký nhắc lịch | `ReminderController.logs` — `GET /api/v1/reminders/logs?status=` | Trả tối đa 100 dòng mới nhất, lọc được theo trạng thái | — |
| P5-2 | ★ | Một lần quét: lấy lịch CONFIRMED trong 24 giờ tới, bỏ lịch đã nhắc, lấy email, gửi, ghi nhật ký; `SKIPPED` khi không có email; `FAILED` thì lần quét sau thử lại | `ReminderService.runOnce`; `POST /api/v1/reminders/run` | Có lịch CONFIRMED trong 24 giờ tới → `run` → thư trong Mailpit (cổng 8025); `run` lần 2 → `sent = 0` | Cron (yêu cầu Hướng 4); Idempotency (46); Retry (46) |
| P5-3 | ★ | Chứng minh độ bền: Reminder tắt thì đặt lịch vẫn chạy; bật lại thì nhắc bù | — | `docker compose stop reminder-service` → vẫn đặt lịch; `start` → lần quét sau gửi nhắc cho lịch vừa đặt | Loose Coupling — service sập, service khác vẫn sống (84); Service Autonomy (13) |
| P5-4 | ★ | Web: trang *Nhật ký nhắc lịch*, nút "Quét ngay", liên kết sang Mailpit | Thay trang khung `admin/reminders` | Quản trị viên xem được thư vừa gửi | — |
| P5-5 | ★ | Bộ request mẫu cho 5 luồng (Postman collection hoặc file `.http`) và kịch bản demo | `docs/` hoặc thư mục `tests/` | Một người không tham gia code chạy được đủ 5 luồng theo kịch bản | — |
| P5-6 | ★ | Kiểm tra đóng gói: clone repo sạch sang máy khác → `docker compose up -d --build` chạy ngay | — | Máy chưa từng chạy dự án bật được hệ thống bằng một lệnh | Containerization (112–115) |
| P5-7 | ☆ | Gửi nhắc thứ hai (ví dụ trước 2 giờ) | Thêm giá trị `ReminderType` | — | — |

**Nội dung báo cáo của phần:** sơ đồ tuần tự luồng nhắc lịch; cách chống gửi lặp; kết quả kịch bản tắt / bật service.

---

## 8. Việc chung khi hoàn thiện

| Việc | Nội dung |
|---|---|
| Báo cáo | Mỗi phần viết mục của mình (bảng dữ liệu, API, sơ đồ); phần chung: kiến trúc tổng thể, đối chiếu yêu cầu đề tài (`docs/02` mục 9), đối chiếu giáo trình (`docs/02` mục 3.3) |
| Sơ đồ theo giáo trình | ERD tách theo từng service, không khóa ngoại chéo (88–89); sơ đồ tuần tự phân biệt lời gọi đồng bộ (nét liền, có mũi tên trả về) và thông điệp bất đồng bộ qua broker (nét đứt) (106–107); DFD có Message Broker là một thành phần (107) |
| Demo | Chạy trọn 5 luồng trên web; tắt Reminder / Provider để thể hiện chịu lỗi |
| Đóng gói | Chỉ dùng `docker-compose.yml` (không có file dev), web ở cổng 4200 |
