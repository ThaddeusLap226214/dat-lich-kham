# 06. Danh sách API — hợp đồng giữa các service cần tuân thủ

Tài liệu là **hợp đồng dịch vụ** (Service Contract — giáo trình trang 21–24) giữa các service: đường dẫn, dữ liệu gửi/nhận, mã lỗi. Mọi API trong danh sách đã có sẵn trong code (controller + DTO). Phần xử lý bên trong đang trả **501 Not Implemented** kèm dòng "Chưa làm: …" mô tả việc cần viết. Người phụ trách service thay phần đó bằng code thật, **không đổi đường dẫn, tên trường hay mã lỗi** khi chưa thống nhất — service khác và web đang dựa vào chúng.

Xem nhanh bằng Swagger (trang tài liệu API tự sinh): chạy hệ thống theo chế độ phát triển (`docs/01` mục 7.1) rồi mở `http://localhost:<cổng>/swagger-ui.html` — cổng 8081 → 8085 ứng với Auth → Reminder.

## Tổng quan API theo phần việc

| Phần | Service | Danh sách API | Liên quan tới service khác |
|---|---|---|---|
| P4 | Auth Service | [B1](#b1-auth-service--auth-service8081) | Cấp JWT; Gateway xác minh ([A5](#a5-đăng-nhập-và-danh-tính)) |
| P3 | Provider Service | [B2](#b2-provider-service--provider-service8082) | Cung cấp API nội bộ: giờ làm việc cho Scheduling; bác sĩ, phòng theo mã cho Gateway |
| P1 | Patient Service (MPI) | [B3](#b3-patient-service--patient-service8083) | Cung cấp API nội bộ: hồ sơ rút gọn cho Gateway, Reminder; phát sự kiện gộp / tách ([A7](#a7-sự-kiện-qua-rabbitmq)) |
| P2 | Scheduling Service | [B4](#b4-scheduling-service--scheduling-service8084) | Gọi Provider lấy giờ làm việc; cung cấp API nội bộ lịch hẹn cho Gateway, Reminder; nhận sự kiện gộp / tách ([A7](#a7-sự-kiện-qua-rabbitmq)) |
| P5 | Reminder Service | [B5](#b5-reminder-service--reminder-service8085) | Gọi Scheduling lấy lịch sắp tới, gọi Patient lấy email |
| P4 | API Gateway | [B6](#b6-api-gateway--api-gateway8080-api-composition) | Gọi Scheduling, Provider, Patient để ghép dữ liệu (API Composition) |

Quy tắc chung mọi phần phải theo: [A1. Đường dẫn](#a1-đường-dẫn) · [A2. Phương thức, mã trả về](#a2-phương-thức-http-và-mã-trả-về) · [A3. Dữ liệu JSON](#a3-dữ-liệu-json) · [A4. Lỗi](#a4-lỗi) · [A5. Đăng nhập và danh tính](#a5-đăng-nhập-và-danh-tính) · [A6. Service gọi service](#a6-service-gọi-service) · [A7. Sự kiện qua RabbitMQ](#a7-sự-kiện-qua-rabbitmq).

---

## Phần A — Quy tắc chung

### A1. Đường dẫn

| Loại | Dạng | Ai gọi |
|---|---|---|
| **API công khai** | `/api/v1/<tài-nguyên>` | Web gọi qua Gateway (`http://localhost:8080/api/v1/...`) |
| **API nội bộ** | `/internal/v1/<tài-nguyên>` | Chỉ service khác gọi, theo tên service qua Eureka (`http://provider-service/internal/v1/...`). Gateway **không** có route cho `/internal`, nên từ ngoài gọi vào sẽ nhận 404 |
| **API ghép dữ liệu** | `/api/v1/composite/...` | Web gọi; Gateway tự xử lý (API Composition) |

- Tên tài nguyên: danh từ số nhiều, chữ thường, nối bằng gạch ngang: `/work-schedules`, `/duplicate-candidates`.
- Hành động không phải thêm/sửa/xóa thì viết thành động từ sau mã: `POST /appointments/{id}/confirm`, `POST /duplicate-candidates/{id}/merge`.

### A2. Phương thức HTTP và mã trả về

| Việc | Phương thức | Thành công |
|---|---|---|
| Đọc | `GET` | `200` |
| Tạo mới | `POST` | `201 Created` |
| Sửa | `PUT` | `200` |
| Xóa / ngừng | `DELETE` | `204 No Content` |
| Hành động (xác nhận, hủy, gộp…) | `POST /{id}/<hành-động>` | `200` |

`GET`, `PUT`, `DELETE` là **lũy đẳng** (idempotent — gọi nhiều lần cho cùng kết quả), `POST` thì không (giáo trình trang 55–56). Vì vậy các thao tác `POST` quan trọng phải tự chống làm hai lần: giữ chỗ dựa vào ràng buộc `UNIQUE` của cơ sở dữ liệu, xử lý sự kiện dựa vào bảng `processed_events`, gửi nhắc dựa vào bảng `reminder_logs`.

### A3. Dữ liệu JSON

- Tên trường **camelCase**: `fullName`, `dateOfBirth`, `startTime`.
- Ngày: `"2026-10-05"`. Giờ: `"08:00:00"`. Ngày giờ: `"2026-10-05T08:00:00"` — **giờ Việt Nam, không kèm múi giờ**.
- Mã (id) là số nguyên.
- Enum gửi đúng chữ in hoa: `"CONFIRMED"`, `"CCCD"`.
- Danh sách có phân trang (chỉ Patient đang dùng): tham số `?page=0&size=20`, trả về:
  ```json
  { "content": [ ... ], "page": 0, "size": 20, "totalElements": 57, "totalPages": 3 }
  ```

### A4. Lỗi

Mọi service trả lỗi cùng một dạng (lớp `ApiError`, bắt tại `GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-10-05T08:00:01",
  "status": 409,
  "error": "SLOT_ALREADY_HELD",
  "message": "Khung giờ này vừa có người giữ",
  "path": "/api/v1/appointments/hold"
}
```

- `error`: **mã lỗi** viết HOA_GẠCH_DƯỚI, web dựa vào mã này để xử lý. `message`: câu tiếng Việt để hiển thị.
- Ném lỗi nghiệp vụ trong code: `throw new BusinessException(HttpStatus.CONFLICT, "SLOT_ALREADY_HELD", "Khung giờ này vừa có người giữ");`
- Không tìm thấy: `throw BusinessException.notFound("Bác sĩ", id);` → 404 `NOT_FOUND`.

| Mã HTTP | Khi nào | `error` |
|---|---|---|
| 400 | Dữ liệu gửi lên sai (thiếu trường, sai định dạng) | `VALIDATION_FAILED` (tự động từ `@Valid`) hoặc mã riêng |
| 400 | Nội dung không phải JSON hợp lệ (kể cả sai mã UTF-8) | `INVALID_JSON` (tự động) |
| 400 | Thiếu header / tham số bắt buộc, sai kiểu tham số | `BAD_REQUEST` (tự động) |
| 401 | Chưa đăng nhập / token sai, hết hạn — **Gateway trả** | `UNAUTHORIZED` |
| 403 | Đăng nhập rồi nhưng không đủ quyền | `FORBIDDEN` |
| 404 | Không có bản ghi | `NOT_FOUND` |
| 409 | Xung đột nghiệp vụ (trùng, sai trạng thái, hết hạn) | mã riêng, xem từng API |
| 501 | Phần khung chưa viết | `NOT_IMPLEMENTED` |
| 503 | Service phía sau không phản hồi — **Gateway trả** | `SERVICE_UNAVAILABLE` |

### A5. Đăng nhập và danh tính

```
Web ──(Authorization: Bearer <JWT>)──► Gateway ──(X-User-Id, X-User-Role, X-Doctor-Id)──► Service
```

1. Web đăng nhập ở `POST /api/v1/auth/login`, nhận `accessToken` (JWT).
2. Mọi request sau gửi kèm header `Authorization: Bearer <accessToken>`.
3. **Gateway** xác minh JWT (chữ ký + hạn dùng), rồi gắn 3 header cho service phía sau:

   | Header | Giá trị | Lấy từ claim JWT |
   |---|---|---|
   | `X-User-Id` | Mã tài khoản | `sub` |
   | `X-User-Role` | `PATIENT` / `RECEPTIONIST` / `DOCTOR` / `ADMIN` | `role` |
   | `X-Doctor-Id` | Mã bác sĩ (chỉ tài khoản DOCTOR) | `doctorId` |

   Ngoài ra Gateway gắn `X-Correlation-Id` (mã UUID riêng cho mỗi request, giữ nguyên khi service gọi tiếp service khác) để truy vết một request qua nhiều service trong log (giáo trình trang 85).

4. Service **không tự đọc JWT**, chỉ tin 3 header trên. Service vẫn phải **tự kiểm tra quyền trên từng bản ghi** (ví dụ bệnh nhân chỉ xem được hồ sơ của mình) — thiếu bước này là lỗi bảo mật phổ biến nhất (giáo trình trang 43, mục 1.14.1). Gateway phải **xóa** các header `X-User-*` / `X-Doctor-Id` mà client tự gửi lên, tránh giả danh.
5. Không cần đăng nhập: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/specialties`, `GET /api/v1/doctors/**`.

**JWT:** token phải chứa mã tài khoản (`sub`), vai trò (`role`), mã bác sĩ (`doctorId`, có thể null) và hạn dùng (`exp`) — đây là phần bắt buộc của hợp đồng. Cấu hình mặc định của khung (phương án tự viết): thuật toán HS256, khóa ký lấy từ biến môi trường `JWT_SECRET` (Auth ký, Gateway xác minh, cùng một giá trị), thêm claim `username`, `iat`, hạn 480 phút (`jwt.expiration-minutes`). Nếu dùng Keycloak (`docs/02` mục 3.3), thuật toán và cách lấy khóa theo Keycloak; các header Gateway gắn xuống service không đổi.

**Khi Gateway chưa có bộ lọc JWT:** tự gửi 3 header bằng tay khi thử API, ví dụ trong Swagger hoặc Postman: `X-User-Id: 5`, `X-User-Role: PATIENT`. Gateway hiện chuyển nguyên header xuống service.

### A6. Service gọi service

- Gọi theo **tên service**, không ghi địa chỉ IP/cổng: `http://provider-service/...`. Đã có sẵn bean `RestClient.Builder` gắn `@LoadBalanced` trong `client/ClientConfig.java` của Scheduling và Reminder; Gateway dùng `WebClient.Builder`.
- Chỉ gọi **API nội bộ** (`/internal/v1/...`) hoặc API công khai có sẵn; **không** đọc database của nhau.
- Lời gọi đồng bộ phải có **thời gian chờ tối đa** (timeout) và phương án khi service kia lỗi: Gateway dùng Circuit Breaker + Fallback (đã cấu hình, chờ tối đa 5 giây); Scheduling gọi Provider lỗi thì trả 503; Reminder gọi lỗi thì thử lại ở lần quét sau (giáo trình trang 45–46).
- Mỗi service tự khai báo record cho dữ liệu nhận về (ví dụ `WorkingHours` trong Scheduling), chỉ chứa trường mình cần. **Không dùng chung code** giữa các service — dùng chung thì sửa một chỗ vỡ nhiều nơi.

### A7. Sự kiện qua RabbitMQ

| Mục | Giá trị |
|---|---|
| Exchange | `patient.events` (loại topic) |
| Routing key | `patient.merged`, `patient.unmerged` |
| Hàng đợi của Scheduling | `scheduling.patient-events` (nhận `patient.*`) |
| Hàng đợi lỗi (DLQ) | `scheduling.patient-events.dlq` — tin nhắn xử lý lỗi được chuyển vào đây thay vì lặp vô hạn |
| Định dạng | JSON, lớp `PatientEvent` (có bản sao trong mỗi service) |

```json
{
  "eventId": "3f1c2a9e-8b7d-4e21-9a40-2c6f0e5d7b11",
  "eventType": "PATIENT_MERGED",
  "occurredAt": "2026-10-05T09:15:00",
  "sourcePatientId": 3,
  "targetPatientId": 1,
  "mergeHistoryId": 7,
  "relatedMergeEventId": null,
  "performedByAccountId": 2
}
```

- `PATIENT_MERGED`: hồ sơ `sourcePatientId` (B) đã gộp vào `targetPatientId` (A) → Scheduling chuyển các lịch `HELD`/`CONFIRMED` của B sang A.
- `PATIENT_UNMERGED`: tách lại; `relatedMergeEventId` = `eventId` của lần gộp bị hoàn tác → Scheduling trả các lịch đã chuyển về B.
- Tên sự kiện là một việc **đã xảy ra**, đặt ở dạng quá khứ; bên phát không biết và không chờ bên nhận (Pub/Sub — giáo trình trang 103–104).
- Bên nhận **phải bỏ qua** sự kiện có `eventId` đã xử lý (bảng `processed_events`).
- Patient Service phát sự kiện **sau khi** đã lưu thay đổi vào database thành công.

---

## Phần B — Danh sách API

Cột "Quyền": ai được gọi. `Khách` = không cần đăng nhập. Service tự kiểm tra quyền dựa trên `X-User-Role` (trả 403 nếu sai); Gateway chặn thô trước ở những chỗ ghi **(GW)**.

### B1. Auth Service — `auth-service:8081`

| Phương thức & đường dẫn | Quyền | Gửi lên | Trả về | Lỗi riêng |
|---|---|---|---|---|
| `POST /api/v1/auth/register` | Khách | `RegisterRequest` {username, password, fullName, email, phone} | 201 `AccountResponse` (role = PATIENT) | 409 `USERNAME_TAKEN`, 409 `EMAIL_TAKEN` |
| `POST /api/v1/auth/login` | Khách | `LoginRequest` {username, password} | 200 `LoginResponse` {accessToken, tokenType = "Bearer", expiresInSeconds, account} | 401 `INVALID_CREDENTIALS`, 403 `ACCOUNT_DISABLED` |
| `GET /api/v1/accounts/me` | Đã đăng nhập | — | `AccountResponse` | |
| `GET /api/v1/accounts?role=` | ADMIN **(GW)** | — | `AccountResponse[]` | |
| `POST /api/v1/accounts` | ADMIN **(GW)** | `CreateAccountRequest` {…, role, doctorId} | 201 `AccountResponse` | 409 `USERNAME_TAKEN`; 400 `DOCTOR_ID_REQUIRED` khi role = DOCTOR mà thiếu doctorId |

`AccountResponse` = {id, username, fullName, email, phone, role, doctorId, enabled}.

### B2. Provider Service — `provider-service:8082`

| Phương thức & đường dẫn | Quyền | Gửi lên | Trả về | Lỗi riêng |
|---|---|---|---|---|
| `GET /api/v1/specialties` | Khách | — | `SpecialtyResponse[]` (chỉ đang hoạt động) | |
| `POST /api/v1/specialties` | ADMIN **(GW)** | `SpecialtyRequest` {code, name, description, active} | 201 | 409 `CODE_TAKEN` |
| `PUT /api/v1/specialties/{id}` | ADMIN **(GW)** | `SpecialtyRequest` | 200 | 404 |
| `GET /api/v1/rooms` | ADMIN, RECEPTIONIST | — | `RoomResponse[]` | |
| `POST /api/v1/rooms`, `PUT /api/v1/rooms/{id}` | ADMIN **(GW)** | `RoomRequest` {code, name, location, active} | 201 / 200 | 409 `CODE_TAKEN` |
| `GET /api/v1/doctors?specialtyId=` | Khách | — | `DoctorResponse[]` {id, fullName, title, specialtyId, specialtyName, phone, email, active} | |
| `GET /api/v1/doctors/{id}` | Khách | — | `DoctorResponse` | 404 |
| `POST /api/v1/doctors`, `PUT /api/v1/doctors/{id}` | ADMIN **(GW)** | `DoctorRequest` {fullName, title, specialtyId, phone, email, active} | 201 / 200 | 404 `NOT_FOUND` (chuyên khoa) |
| `GET /api/v1/doctors/{id}/work-schedules` | Khách | — | `WorkScheduleResponse[]` | |
| `POST /api/v1/doctors/{id}/work-schedules` | ADMIN **(GW)** | `WorkScheduleRequest` {roomId, dayOfWeek, startTime, endTime, slotMinutes, effectiveFrom, effectiveTo, active} | 201 | 409 `SCHEDULE_OVERLAP` (chồng giờ ca khác cùng ngày của bác sĩ) |
| `PUT /api/v1/work-schedules/{id}` | ADMIN **(GW)** | `WorkScheduleRequest` | 200 | 409 `SCHEDULE_OVERLAP` |
| `DELETE /api/v1/work-schedules/{id}` | ADMIN **(GW)** | — | 204 (đặt `active = false`) | |
| **Nội bộ** `GET /internal/v1/doctors/{id}/working-hours?date=2026-10-05` | Scheduling | — | `WorkingHoursResponse` {doctorId, date, shifts: [{workScheduleId, roomId, startTime, endTime, slotMinutes}]} — ngày nghỉ thì `shifts` rỗng | 404 |
| **Nội bộ** `GET /internal/v1/doctors?ids=1,2,3` | Gateway | — | `DoctorResponse[]` | |
| **Nội bộ** `GET /internal/v1/rooms?ids=1,2` | Gateway | — | `RoomResponse[]` | |

### B3. Patient Service — `patient-service:8083`

| Phương thức & đường dẫn | Quyền | Gửi lên | Trả về | Lỗi riêng |
|---|---|---|---|---|
| `POST /api/v1/patients` | PATIENT, RECEPTIONIST | `PatientRequest` {fullName, dateOfBirth, gender, phone, email, address, idType, idNumber, guardianName, guardianPhone, guardianRelation} | `CreatePatientResponse` {patient, outcome, duplicateQueued}. **201** nếu tạo mới (`CREATED_ACTIVE` / `CREATED_TEMPORARY`), **200** nếu dùng lại hồ sơ có sẵn (`REUSED_EXISTING`) | 400 `INVALID_ID_NUMBER` (CCCD phải đủ 12 số) |
| `GET /api/v1/patients/mine` | PATIENT | — | `PatientResponse[]` các hồ sơ tài khoản đang quản lý (trừ MERGED) | |
| `GET /api/v1/patients?keyword=&status=&page=&size=` | RECEPTIONIST, DOCTOR | — | `PageResponse<PatientResponse>` | |
| `GET /api/v1/patients/{id}` | Chủ hồ sơ, RECEPTIONIST, DOCTOR | — | `PatientResponse` | 403, 404 |
| `PUT /api/v1/patients/{id}` | Chủ hồ sơ, RECEPTIONIST | `PatientRequest` (không đổi giấy tờ ở đây) | `PatientResponse` | 409 `PATIENT_MERGED` (hồ sơ đã gộp thì không sửa) |
| `POST /api/v1/patients/{id}/identity` | RECEPTIONIST | `AddIdentityRequest` {idType, idNumber} | `PatientResponse` — tình huống 4: thành ACTIVE; tình huống 5: vẫn TEMPORARY và có cặp mới trong hàng chờ | 409 `NOT_TEMPORARY` |
| `POST /api/v1/patients/{id}/unmerge` | RECEPTIONIST | `ReviewNoteRequest` {note} | `MergeHistoryResponse` | 409 `NOT_MERGED` |
| `GET /api/v1/patients/{id}/merge-history` | RECEPTIONIST | — | `MergeHistoryResponse[]` | |
| `GET /api/v1/duplicate-candidates?status=PENDING&page=&size=` | RECEPTIONIST **(GW)** | — | `PageResponse<DuplicateCandidateResponse>` (kèm đủ 2 hồ sơ để so sánh) | |
| `GET /api/v1/duplicate-candidates/count?status=PENDING` | RECEPTIONIST **(GW)** | — | `{"count": 3}` | |
| `POST /api/v1/duplicate-candidates/{id}/merge` | RECEPTIONIST **(GW)** | `MergeRequest` {keepPatientId, note} — `keepPatientId` là hồ sơ giữ lại | `MergeHistoryResponse` + phát sự kiện `PATIENT_MERGED` | 409 `CANDIDATE_ALREADY_REVIEWED`, 400 `KEEP_PATIENT_NOT_IN_PAIR` |
| `POST /api/v1/duplicate-candidates/{id}/reject` | RECEPTIONIST **(GW)** | `ReviewNoteRequest` {note} | `DuplicateCandidateResponse` (status = NOT_DUPLICATE) | 409 `CANDIDATE_ALREADY_REVIEWED` |
| **Nội bộ** `GET /internal/v1/patients?ids=1,2` | Gateway, Reminder | — | `PatientSummary[]` {id, fullName, dateOfBirth, gender, phone, email, guardianPhone, status, mergedIntoId} | |

**Quy tắc tạo hồ sơ** (`POST /api/v1/patients`) — theo bảng tình huống trong `docs/02` mục 4.1:

| Có giấy tờ? | Tìm thấy hồ sơ ACTIVE cùng giấy tờ? | Tên (chuẩn hóa) + ngày sinh khớp? | Kết quả |
|---|---|---|---|
| Có | Có | Có | `REUSED_EXISTING` — trả hồ sơ cũ (tình huống 1) |
| Có | Có | Không | `CREATED_TEMPORARY` + cặp `SAME_ID_DIFFERENT_INFO` vào hàng chờ (tình huống 2) |
| Có | Không | — | `CREATED_ACTIVE` |
| Không | — | Trùng tên + ngày sinh + SĐT với hồ sơ có sẵn | `CREATED_TEMPORARY` + cặp `SAME_NAME_DOB_PHONE` vào hàng chờ (tình huống 3) |
| Không | — | Không trùng | `CREATED_TEMPORARY` |

### B4. Scheduling Service — `scheduling-service:8084`

| Phương thức & đường dẫn | Quyền | Gửi lên | Trả về | Lỗi riêng |
|---|---|---|---|---|
| `GET /api/v1/slots?doctorId=1&date=2026-10-05` | Đã đăng nhập | — | `SlotResponse[]` {startTime, endTime, roomId, workScheduleId, available} — đủ mọi slot trong ngày, `available = false` nếu đã có người giữ/đặt hoặc đã qua giờ | 503 nếu Provider không phản hồi |
| `POST /api/v1/appointments/hold` | PATIENT, RECEPTIONIST | `HoldRequest` {doctorId, patientId, startTime, reason} | 201 `AppointmentResponse` (status = HELD, holdExpiresAt) | **409 `SLOT_ALREADY_HELD`**, 409 `PATIENT_TIME_CONFLICT`, 400 `SLOT_NOT_IN_SCHEDULE` |
| `POST /api/v1/appointments/{id}/confirm` | Người đã giữ chỗ | — | `AppointmentResponse` (CONFIRMED) | 409 `HOLD_EXPIRED`, 409 `INVALID_STATUS`, 403 |
| `POST /api/v1/appointments/{id}/cancel` | Người đặt, RECEPTIONIST | `CancelRequest` {reason} | `AppointmentResponse` (CANCELLED) | 409 `TOO_LATE_TO_CANCEL` (PATIENT hủy muộn hơn `scheduling.cancel-before-hours` = 2 giờ trước giờ khám), 409 `INVALID_STATUS` |
| `POST /api/v1/appointments/{id}/complete` | DOCTOR (lịch của mình), RECEPTIONIST | — | `AppointmentResponse` (COMPLETED) | 403, 409 `INVALID_STATUS` |
| `GET /api/v1/appointments/mine` | Đã đăng nhập | — | `AppointmentResponse[]` do tài khoản đặt (chỉ có mã, chưa ghép tên — web nên gọi bản ghép ở B6) | |
| `GET /api/v1/appointments?doctorId=&date=&status=` | RECEPTIONIST, DOCTOR | — | `AppointmentResponse[]` | |
| `GET /api/v1/appointments/{id}` | Người đặt, RECEPTIONIST, DOCTOR | — | `AppointmentResponse` | 404 |
| **Nội bộ** `GET /internal/v1/appointments?bookedByAccountId=&doctorId=&status=&from=&to=` | Gateway, Reminder | — | `AppointmentResponse[]` (mọi tham số đều không bắt buộc; `from`, `to` dạng ngày giờ) | |

`AppointmentResponse` = {id, patientId, doctorId, roomId, workScheduleId, bookedByAccountId, startTime, endTime, status, holdExpiresAt, reason, cancelReason, createdAt}.

**Nhận sự kiện:** `patient.merged`, `patient.unmerged` (Phần A7).

### B5. Reminder Service — `reminder-service:8085`

| Phương thức & đường dẫn | Quyền | Trả về |
|---|---|---|
| `GET /api/v1/reminders/logs?status=` | ADMIN, RECEPTIONIST | `ReminderLogResponse[]` — 100 dòng gần nhất |
| `POST /api/v1/reminders/run` | ADMIN **(GW)** | `RunResult` {checked, sent, skipped, failed} — quét ngay, không chờ cron |

**Cron:** `reminder.cron = "0 */5 * * * *"` (mỗi 5 phút), nhắc lịch `CONFIRMED` bắt đầu trong `reminder.hours-before = 24` giờ tới. Email gửi vào Mailpit — xem ở `http://localhost:8025`.

**Luồng một lần quét:**
```
SchedulingClient.findConfirmedBetween(now, now + 24h)
  → bỏ các lịch đã có log SENT / SKIPPED
  → PatientClient.findByIds(các patientId còn lại) lấy email
  → gửi mail (JavaMailSender) → ghi reminder_logs
```

### B6. API Gateway — `api-gateway:8080` (API Composition)

| Phương thức & đường dẫn | Quyền | Trả về |
|---|---|---|
| `GET /api/v1/composite/my-appointments` | Đã đăng nhập | `AppointmentView[]` |
| `GET /api/v1/composite/doctor-agenda?doctorId=1&date=2026-10-05` | DOCTOR (chỉ `doctorId` của mình), RECEPTIONIST | `AppointmentView[]` |

```json
{
  "appointmentId": 12,
  "status": "CONFIRMED",
  "startTime": "2026-10-05T08:00:00",
  "endTime": "2026-10-05T08:20:00",
  "reason": "Ho kéo dài",
  "doctor": { "id": 1, "fullName": "Nguyễn Văn An", "title": "ThS.BS", "specialtyName": "Nội tổng quát" },
  "room": { "id": 1, "code": "P101", "name": "Phòng khám Nội 1" },
  "patient": { "id": 1, "fullName": "Lê Thị Hoa", "dateOfBirth": "1990-05-12" }
}
```

**Luồng ghép** (`my-appointments`) — đây là trọng tâm chấm điểm của Hướng 4:

```
① SchedulingClient.findByAccount(X-User-Id)        → danh sách lịch (chỉ có mã)
② gom các doctorId, roomId, patientId khác nhau
③ gọi song song:
     ProviderClient.findDoctors(doctorIds)
     ProviderClient.findRooms(roomIds)
     PatientClient.findPatients(patientIds)
④ ghép theo mã → AppointmentView
```

- Gọi song song bằng `Mono.zip(...)`, mỗi lời gọi bọc Circuit Breaker như `ApiGatewayController` của Petclinic.
- Provider hoặc Patient không phản hồi → **vẫn trả danh sách lịch**, phần thiếu để `null` (web hiện "Không rõ"). Scheduling không phản hồi → 503.
- Các lớp `SchedulingClient`, `ProviderClient`, `PatientClient` đã viết sẵn lời gọi; chỉ còn phần ghép trong `CompositeController`.
