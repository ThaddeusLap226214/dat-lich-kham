# 04. Việc của từng người

Đọc file này để biết mình làm việc gì, đang ở đâu, làm gì trước, chờ ai và làm tới đâu thì xong. Phần chung, đăng nhập và Auth Service đã làm xong ([`00-bat-dau-tu-day.md`](00-bat-dau-tu-day.md)), nên mỗi người chỉ code trong thư mục service của mình.

Hạn nộp: **30/11/2026**. Không chia sprint: mỗi người tự sắp việc của mình, nhưng làm **"Làm trước tiên"** trước vì người khác đang cần. Việc ☆ là nâng cao, làm khi các việc ★ đã xong; ai thấy phần mình nhẹ thì nhận thêm ☆, hoặc tự đề xuất việc nâng cao khác.

## Mục lục

1. Phân việc cho 5 người
2. Việc của từng người
3. Khi phải chờ người khác: dùng bản giả
4. Cách làm một việc đến khi xong
5. Quy ước khi code
6. Việc chung khi hoàn thiện

---

## 1. Phân việc cho 5 người

| Người | Service (thư mục) | Trang web | Đầu mối thường xuyên với |
|---|---|---|---|
| Như | Patient Service (`patient-service/`) | *Hồ sơ bệnh nhân của tôi* · *Hồ sơ bệnh nhân* (lễ tân) · *Hàng chờ duyệt hồ sơ trùng* | Vũ (sự kiện gộp / tách), Lập, Hiếu (hồ sơ rút gọn) |
| Vũ | Scheduling Service (`scheduling-service/`) | *Đặt lịch khám* · *Lịch hẹn theo bác sĩ* (lễ tân) | Vinh (giờ làm việc), Như (hồ sơ, sự kiện gộp / tách), Lập (lịch hẹn), Hiếu (lịch hẹn, sự kiện xác nhận / hủy) |
| Vinh | Provider Service (`provider-service/`) | *Chuyên khoa & bác sĩ* · quản trị danh mục · quản trị lịch làm việc · *Lịch khám trong ngày* (bác sĩ) · *Lịch khám của tôi* (bệnh nhân) | Vũ (slot), Lập (API ghép cho hai trang lịch khám) |
| Lập | API Gateway (`api-gateway/`) | *Đăng nhập* (chuyển sang Keycloak) | mọi người (token, chặn quyền, API ghép) |
| Hiếu | Reminder Service — dịch vụ thông báo: nhắc lịch, xác nhận, báo hủy (`reminder-service/`) | *Nhật ký thông báo* · *Mẫu thư* | Vũ (lấy lịch, sự kiện xác nhận / hủy), Như (lấy email) |

---

## 2. Việc của từng người

Cách đọc bảng:
- **Mã** là mã việc; việc mới đánh mã tiếp.
- **Đặc tả**: mã chức năng `F..` ở [`01-nghiep-vu-va-dac-ta-chuc-nang.md`](01-nghiep-vu-va-dac-ta-chuc-nang.md) mục 6.1 (trang, API, kiểm tra, trạng thái, quyền) và mã chính sách `CS..` ở mục 7 của cùng file.
- **Chờ ai khi kiểm thử**: viết code được ngay; chỉ bước kiểm cuối cần việc của người khác. Trong lúc chờ, dùng bản giả (mục 3).

### 2.1. Như — Patient Service

**Đang ở đâu:** bảng, entity, mọi API đã có chỗ (chưa viết thì trả 501). Đã chạy (chuyển từ repo MPI): tìm kiếm có phân trang, xem, sửa hồ sơ (kiểm quyền chủ hồ sơ), chuẩn hóa tên (`NameNormalizer`), tính điểm giống nhau (`MatchScorer`), bộ phát sự kiện (`PatientEventPublisher`), 8 unit test. Trang *Hồ sơ bệnh nhân* đã khớp hợp đồng (danh sách, tìm, lọc, sửa). Năm trang MPI cũ (`dashboard`, `patient-master`, `patient-import`, `facility`, `source-system`) đang ẩn khỏi menu: giữ hay xóa do Như quyết trong P1-7.

| Mã | Việc | Đặc tả | Xong khi | Chờ ai khi kiểm thử |
|---|---|---|---|---|
| P1-1 | **Làm trước tiên.** Hồ sơ của tài khoản; hồ sơ rút gọn cho service khác (`/internal/v1/patients?ids=`) | F21 (phần xem), F27 | `mine` với `X-User-Id: 5` trả 2 hồ sơ mẫu; `internal … ?ids=1,3` trả 2 bản rút gọn có `email` | — |
| P1-2 | Tạo hồ sơ theo bảng tình huống, ghi hàng chờ trùng | F21, F22, F24 · CS19, CS20 | Tạo hồ sơ CCCD `048190001234` khác tên → `CREATED_TEMPORARY`, `duplicateQueued = true`, `duplicate_candidates` có dòng mới; hồ sơ trẻ 5 tuổi thiếu người giám hộ → 400 `GUARDIAN_REQUIRED`; SĐT `+84 935 111 222` lưu thành `0935111222` | — |
| P1-3 | Bổ sung giấy tờ cho hồ sơ tạm | F23 · CS27 | Hồ sơ 2 bổ sung CCCD mới → `ACTIVE`; bổ sung CCCD của hồ sơ 1 → vẫn `TEMPORARY`, có cặp mới lý do `ID_ADDED_BELONGS_TO_OTHER` | — |
| P1-4 | Hàng chờ duyệt: danh sách, đếm, xác nhận không trùng | F25 | Lễ tân thấy cặp mẫu 3–1; `reject` → `NOT_DUPLICATE`; gọi lại → 409 `CANDIDATE_ALREADY_REVIEWED` | — |
| P1-5 | Gộp hồ sơ, phát sự kiện **sau khi** lưu database | F25 · CS23, CS25 | Gộp 3 vào 1 → hồ sơ 3 `MERGED`, `merged_into_id = 1`; trang quản trị RabbitMQ (cổng 15672) thấy tin trên `patient.events` | — |
| P1-6 | Tách hồ sơ gộp nhầm, xem lịch sử | F26 · CS24 | Tách hồ sơ 3 → về trạng thái trước khi gộp, cặp thành `NOT_DUPLICATE`; lịch sử có 2 dòng `MERGE`, `UNMERGE`; sự kiện `PATIENT_UNMERGED` có `relatedMergeEventId` | — |
| P1-7 | Web: *Hồ sơ bệnh nhân của tôi*; *Hàng chờ duyệt* (so sánh, gộp, không trùng, tách) | F21, F25, F26 | Chọn vai trò lễ tân: tìm "le thi hoa" thấy 2 hồ sơ; gộp, tách được từ giao diện | — |
| P1-9 | Web: trang *Hồ sơ bệnh nhân* thêm tạo hồ sơ tại quầy và bổ sung giấy tờ | F22, F23 | Lễ tân tạo hồ sơ không giấy tờ → `TEMPORARY`; bổ sung CCCD trên trang → `ACTIVE` | — |
| ☆ P1-8 | Lưu vết ai xem / sửa hồ sơ (nhật ký truy cập) | — | Xem một hồ sơ → bảng lưu vết có dòng mới (mã tài khoản, mã hồ sơ, thời điểm); bảng mới ghi vào `02-co-so-du-lieu.md` | — |
| ☆ P1-10 | Web: xem lịch sử gộp / tách của một hồ sơ | F26 | Hồ sơ 3 sau khi gộp rồi tách hiện 2 dòng lịch sử | — |

**Lớp chính:** `PatientService` (`listMine`, `findSummaries`, `create`, `addIdentity`), `DuplicateReviewService`, `MergeService` (`merge`, `unmerge`, `history`). **Giáo trình:** Database per Service (88–89) · Saga Choreography (92–93), giao dịch bù trừ (48, 92) · Pub/Sub, đặt tên sự kiện (103–104) · Eventual Consistency (86) · Audit (43, cho P1-8). **Báo cáo phần mình:** sơ đồ trạng thái hồ sơ; sơ đồ tuần tự luồng gộp hồ sơ qua RabbitMQ (mũi tên nét đứt cho thông điệp bất đồng bộ — giáo trình 106–107); ERD `patient_db`.

### 2.2. Vũ — Scheduling Service

**Đang ở đâu:** bảng có ràng buộc chống đặt trùng, entity, mọi API đã có chỗ (trả 501), client gọi Provider (đã có timeout 3 giây), bộ nhận sự kiện gộp / tách. API giờ làm việc của Provider đã chạy.

| Mã | Việc | Đặc tả | Xong khi | Chờ ai khi kiểm thử |
|---|---|---|---|---|
| P2-1 | **Làm trước tiên.** API đọc lịch hẹn, gồm API nội bộ `/internal/v1/appointments` (thêm tham số `patientIds`); `AppointmentResponse` thêm `confirmedAt`, `cancelledAt`, `cancelledByAccountId`, `completedAt`, `noShowAt`; `GET /appointments/{id}` tự kiểm quyền | F3B | `mine` của tài khoản 5 trả 4 lịch (mã 1, 6, 7, 13); nội bộ lọc `status=CONFIRMED` trả 7 lịch; `?patientIds=1,2` trả đúng lịch của hai hồ sơ | — |
| P2-2 | Nhả chỗ giữ quá hạn (chạy định kỳ) | F34 | Lịch `HELD` có `hold_expires_at` đã qua → trong 30 giây thành `EXPIRED` | — |
| P2-3 | Tính slot trống | F31 · CS05, CS06, CS11 | Bác sĩ 1, thứ Hai tuần tới → 21 slot (12 sáng + 9 chiều), 08:10, 08:30, 13:30 `available = false`; ngày sau 30 ngày → mọi slot `false`; Provider tắt → 503 thay vì treo | — (API giờ làm việc của Provider đã sẵn sàng) |
| P2-4 | Giữ chỗ, chống đặt trùng | F32 · CS03, CS04, CS05, CS06, CS12 | Giữ một slot hai lần → lần 2 nhận 409 `SLOT_ALREADY_HELD`; giờ không thuộc ca → 400 `SLOT_NOT_IN_SCHEDULE`; hồ sơ có lịch chồng giờ → 409 `PATIENT_TIME_CONFLICT`, lịch liền kề → 201; giữ chỗ thứ 4 cùng lúc → 409 `TOO_MANY_HOLDS` | — |
| P2-5 | Xác nhận, hủy, đánh dấu đã khám | F33, F35, F38 · CS02, CS07, CS08, CS09 | Xác nhận sau 5 phút → 409 `HOLD_EXPIRED`; bệnh nhân hủy khi còn dưới 2 giờ → 409 `TOO_LATE_TO_CANCEL`, lễ tân vẫn hủy được; bác sĩ hủy → 403; đánh dấu trước giờ khám → 409 `TOO_EARLY_TO_COMPLETE`; bác sĩ khác đánh dấu → 403; đánh dấu xong thì `completed_by_account_id` = mã người đánh dấu | — |
| P2-10 | Ghi bệnh nhân không đến: `POST /appointments/{id}/no-show` (bảng đã có trạng thái `NO_SHOW` và cột `no_show_at`, xem `00-bat-dau-tu-day.md` mục 5 dòng 13) | F39 · CS10 | Chạy đúng kịch bản KB7 ở `02-co-so-du-lieu.md` mục 8; `no_show_by_account_id` = mã người ghi | — |
| P2-12 | Phát sự kiện `appointment.confirmed`, `appointment.cancelled` **sau khi** lưu thành công (cùng cách Patient phát sự kiện gộp hồ sơ) | F33, F35 · CS31 | Xác nhận một lịch → trang quản trị RabbitMQ thấy tin trên `appointment.events` đúng khuôn `03-hop-dong-api.md` A7; hủy → có tin `appointment.cancelled` kèm `cancelReason`; xác nhận bị từ chối (409) → không có tin | — |
| P2-6 | Nhận sự kiện gộp / tách hồ sơ, bỏ qua sự kiện đã xử lý | F3A · CS14 | Gửi cùng một sự kiện 2 lần → lịch chỉ chuyển 1 lần; gộp 3 vào 1 → mọi lịch của hồ sơ 3 sang hồ sơ 1; tách → về hồ sơ 3; tắt Scheduling lúc gộp, bật lại → sự kiện vẫn được xử lý | Như (P1-5, P1-6). Trong lúc chờ: tự gửi tin mẫu theo `03-hop-dong-api.md` A7 từ trang quản trị RabbitMQ |
| P2-7 | Web: *Đặt lịch khám*; *Lịch hẹn theo bác sĩ* cho lễ tân, có nút Hủy, Đã khám, Không đến | F31–F33, F35, F38, F39 | Hai cửa sổ trình duyệt cùng giữ một slot → chỉ một bên thành công, bên kia được báo và thấy slot bị khóa | Vinh (P3-1 danh sách chuyên khoa, bác sĩ), Như (P1-1, P1-2 chọn / khai hồ sơ) |
| P2-9 | Trang *Lịch hẹn theo bác sĩ* lấy dữ liệu từ `GET /composite/doctor-agenda` để hiện tên bệnh nhân, bác sĩ, phòng | F37 | Lễ tân chọn bác sĩ 1, ngày có lịch → bảng hiện tên; sau khi gộp hồ sơ, lịch chuyển hiện tên hồ sơ giữ lại | Lập (P4-6). Trong lúc chờ: dùng `GET /appointments` |
| ☆ P2-8 | Bộ nhớ đệm giờ làm việc để giảm số lần gọi Provider | F31 | Gọi `GET /slots` cùng bác sĩ, cùng ngày hai lần → log Provider chỉ có một lời gọi `working-hours` | — |

**Lớp chính:** `AppointmentService` (`hold`, `confirm`, `cancel`, `complete`, `listMine`, `listByDoctor`, `search`, `expireHolds`), `SlotService.findSlots`, `PatientEventHandler.handle`. **Giáo trình:** giao tiếp đồng bộ (26, 101) · Timeout, Fallback (45–46) · `POST` không lũy đẳng (56) · kiểm quyền trên từng bản ghi (43) · Idempotency (46) · Saga và bù trừ (92–94) · Message Broker làm vùng đệm (103–104) · DLQ (107) · CQRS (94–95, cho P2-8). **Báo cáo phần mình:** thuật toán tính slot; sơ đồ tuần tự hai người tranh một slot; sơ đồ trạng thái lịch hẹn; ERD `scheduling_db`.

### 2.3. Vinh — Provider Service

**Đang ở đâu:** bảng, entity, mọi API đã có chỗ (trả 501), dữ liệu mẫu 6 bác sĩ / 25 ca. API nội bộ giờ làm việc (P3-3) và bác sĩ, phòng theo mã (P3-2) đã chạy ([`00-bat-dau-tu-day.md`](00-bat-dau-tu-day.md) mục 5 dòng 8, 9).

| Mã | Việc | Đặc tả | Xong khi | Chờ ai khi kiểm thử |
|---|---|---|---|---|
| P3-1 | **Làm trước tiên.** Danh mục chuyên khoa, phòng, bác sĩ: xem, thêm, sửa | F11, F13 | `GET /api/v1/doctors?specialtyId=1` trả 2 bác sĩ; thêm chuyên khoa trùng mã → 409 `CODE_TAKEN` | — |
| P3-4 | Quản lý ca làm việc, chặn ca chồng giờ | F12 · CS16 | Thêm ca chồng giờ ca có sẵn cùng ngày → 409 `SCHEDULE_OVERLAP`; ngừng ca → `active = false`, không xóa dòng | — |
| P3-5 | Web: *Chuyên khoa & bác sĩ*, quản trị danh mục, quản trị lịch làm việc | F11–F13 | Thêm ca mới trên web → slot mới xuất hiện ở `GET /api/v1/slots` | Vũ (P2-3) |
| P3-6 | Web: *Lịch khám trong ngày* của bác sĩ, nút "Đã khám" và "Không đến" | F37, F38, F39 | Chọn vai trò `bs.an` thấy lịch của bác sĩ 1; bấm "Đã khám" → `COMPLETED`; bấm "Không đến" → `NO_SHOW` | Lập (P4-6), Vũ (P2-5, P2-10). Trong lúc chờ: service Angular trả dữ liệu cứng đúng khuôn `AppointmentView` (`03-hop-dong-api.md` B6) |
| P3-8 | Web: *Lịch khám của tôi*, nút hủy lịch | F36, F35 | Bệnh nhân thấy lịch kèm tên bác sĩ, phòng, kể cả lịch lễ tân đặt cho hồ sơ của mình (CS15); bấm hủy → `CANCELLED` | Lập (P4-6), Vũ (P2-5). Trong lúc chờ: như P3-6 |
| ☆ P3-7 | Bộ nhớ đệm danh sách chuyên khoa / bác sĩ | F13 | Gọi `GET /api/v1/specialties` hai lần → lần 2 không sinh câu SQL | — |
| ☆ P3-9 | Ngày nghỉ, ngày lễ của bác sĩ | F12, F14 | Thêm ngày nghỉ cho bác sĩ 1 → `working-hours` ngày đó trả `shifts` rỗng; bảng mới ghi vào `02-co-so-du-lieu.md` | — |

**Lớp chính:** `CatalogService`, `WorkScheduleService` (`listByDoctor`, `create`, `update`, `delete`). **Giáo trình:** REST: URI, phương thức, mã trạng thái (54–58) · Cacheable (55, cho P3-7). **Báo cáo phần mình:** ERD `provider_db`; cách cấu hình ca làm việc sinh ra slot.

### 2.4. Lập — API Gateway

**Đang ở đâu:** định tuyến 5 service, Circuit Breaker + Fallback trả `ApiError`, CORS, Correlation ID đã chạy; 3 lớp client (`SchedulingClient`, `ProviderClient`, `PatientClient`) đã có lời gọi lấy lịch theo người đặt, theo bác sĩ, và bác sĩ / phòng / hồ sơ theo mã. Còn: bộ lọc token, chặn vai trò, API ghép, đăng nhập Keycloak trên web.

| Mã | Việc | Đặc tả | Xong khi | Chờ ai khi kiểm thử |
|---|---|---|---|---|
| P4-6 | **Làm sớm** (Vinh, Vũ cần cho trang). API ghép `my-appointments` (lịch do tài khoản đặt **hoặc** của hồ sơ tài khoản quản lý — CS15), `doctor-agenda`; gọi song song, Circuit Breaker; thêm `cancelReason` | F36, F37 | Có lịch hẹn → JSON đủ tên bác sĩ, phòng, bệnh nhân; `docker compose stop provider-service` → vẫn trả danh sách, `doctor = null` | Vũ (P2-1), Như (P1-1). Trong lúc chờ: client giả |
| P4-13 | Mỗi route của Gateway một Circuit Breaker riêng | N5 (`01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 6.3) | Tắt Provider, gọi nhiều lần → route Provider trả 503; ngay sau đó `GET /api/v1/patients` vẫn 200 | — |
| P4-4 | Bộ lọc token: xác minh token Keycloak, cho qua đường dẫn công khai, xóa `X-User-*` client tự gửi, gắn lại từ token | F52 | Không token → 401; token hợp lệ → 200; tự gửi `X-User-Id: 1` kèm token tài khoản 5 → service vẫn nhận 5 | — |
| P4-5 | Chặn theo vai trò các API đánh dấu **(GW)** trong `03-hop-dong-api.md` | F52 | Vai trò `PATIENT` gọi `POST /api/v1/doctors` → 403 dạng `ApiError` | — |
| P4-9 | Web: đăng nhập chuyển sang trang Keycloak, gắn token vào request, hết hạn thì về trang đăng nhập, **bỏ Chế độ thử** | F52 | Đăng nhập `letan` → menu lễ tân; web không còn ô Chế độ thử | — |
| P4-15 | Web: sửa `app-pages.ts` cho khớp phân công và đăng nhập Keycloak | — | Nhãn phần trên từng trang đúng bảng mục 1; `frontend/src` không còn `auth/login`; `ng build` không lỗi | — |
| ☆ P4-8 | Giới hạn số request | — | Gửi quá ngưỡng → 429 | — |

**Lưu ý cho cả nhóm:** P4-4, P4-5, P4-9 merge **cùng một lúc**. Trước thời điểm đó mọi người thử bằng Chế độ thử như hiện nay. Sau đó thử bằng đăng nhập thật; Swagger ở cổng 8081–8085 (chế độ phát triển) vẫn dùng header gõ tay. Ngày merge sẽ báo trước trên nhóm.

**Lớp chính:** bộ lọc token mới (`GlobalFilter`) trong `api-gateway/.../security/`, `CompositeController`. **Giáo trình:** API Gateway là lớp bảo mật đầu tiên (42–43) · Gateway + JWT (76–77) · Authorization, 403 (73) · API Composition (89–91, 108) · Circuit Breaker, Fallback (45–46, 109) · Rate Limiting (77–78, cho P4-8). **Báo cáo phần mình:** sơ đồ tuần tự API Composition (luồng 3, [`01-nghiep-vu-va-dac-ta-chuc-nang.md`](01-nghiep-vu-va-dac-ta-chuc-nang.md) mục 5.1) — trọng tâm báo cáo Hướng 4; luồng đăng nhập → token → Gateway → service.

### 2.5. Hiếu — Reminder Service (dịch vụ thông báo)

Reminder Service gửi ba loại thư: **nhắc lịch** (quét định kỳ, gọi Scheduling), **xác nhận** và **báo hủy** (nhận sự kiện từ Scheduling qua RabbitMQ). Nội dung thư lấy từ mẫu do quản lý soạn.

**Đang ở đâu:** bảng `reminder_logs`, `message_templates` (3 mẫu nạp sẵn), `processed_events` và dữ liệu mẫu đã có; entity, cron, client gọi Scheduling / Patient (đã có timeout 3 giây). Còn: phần quét và gửi thư, nhận sự kiện, mẫu thư, các trang.

| Mã | Việc | Đặc tả | Xong khi | Chờ ai khi kiểm thử |
|---|---|---|---|---|
| P5-1 | Nhật ký thông báo | F42 | Trả tối đa 100 dòng mới nhất, lọc được theo trạng thái và loại thư | — |
| P5-11 | Mẫu thư: API xem / sửa, trang *Mẫu thư*; mọi thư lấy nội dung từ bảng `message_templates` (bảng và 3 mẫu đã có sẵn) | F45 · CS32 | Quản lý sửa tiêu đề mẫu nhắc trên trang → thư nhắc kế tiếp trong Mailpit có tiêu đề mới; vai trò khác gọi `PUT` → 403 | — |
| P5-2 | Một lần quét: lịch `CONFIRMED` trong 24 giờ tới → bỏ lịch đã nhắc → lấy email → gửi → ghi nhật ký | F41 · CS28–CS32 | Có lịch `CONFIRMED` trong 24 giờ tới → `run` → thư trong Mailpit (`http://localhost:8025`) đúng nội dung CS32; `run` lần 2 → `sent = 0`; hồ sơ không email → `SKIPPED` | Vũ (P2-1), Như (P1-1). Trong lúc chờ: client giả. Dữ liệu mẫu không có lịch trong 24 giờ tới → tự thêm một dòng vào `scheduling_db` |
| P5-8 | Nhận sự kiện `appointment.confirmed` → gửi email xác nhận; dùng bảng `processed_events` có sẵn để chống xử lý lặp | F43 · CS31, CS35, CS36 | Xác nhận một lịch → trong vài giây Mailpit có thư xác nhận; gửi lại cùng sự kiện → không có thư thứ hai; tắt Reminder, xác nhận một lịch, bật lại → thư vẫn tới | Vũ (P2-12). Trong lúc chờ: tự gửi tin mẫu theo `03-hop-dong-api.md` A7 từ trang quản trị RabbitMQ |
| P5-10 | Nhận sự kiện `appointment.cancelled` → gửi thư báo hủy kèm lý do | F44 · CS31 | Hủy một lịch → Mailpit có thư báo hủy kèm lý do; hồ sơ không email → nhật ký `SKIPPED` | Vũ (P2-12). Trong lúc chờ: như P5-8 |
| P5-4 | Web: *Nhật ký thông báo*, nút "Quét ngay", liên kết Mailpit | F42 | Quản trị viên xem được thư vừa gửi, lọc theo loại thư | — |
| P5-3 | Chứng minh độ bền khi tắt / bật Reminder | F41 | `docker compose stop reminder-service` → vẫn đặt lịch được; `start` → lần quét sau gửi nhắc cho lịch vừa đặt | Vũ (P2-4, P2-5) |
| ☆ P5-7 | Gửi nhắc lần hai (ví dụ trước 2 giờ) | F41 | Lịch trong 2 giờ tới → thêm dòng nhật ký loại thứ hai, Mailpit có thư thứ hai | — |
| ☆ P5-9 | Thêm tên bác sĩ, phòng vào thư nhắc | F41 | Thư nhắc có tên bác sĩ, phòng; Provider tắt → vẫn gửi thư không có tên | — |

**Lớp chính:** `ReminderService.runOnce`, `ReminderController`. **Giáo trình:** cron (yêu cầu Hướng 4) · Idempotency, Retry (46) · Loose Coupling — service sập, service khác vẫn sống (84) · Service Autonomy (13). **Báo cáo phần mình:** sơ đồ tuần tự luồng nhắc lịch và luồng thư theo sự kiện; cách chống gửi lặp; kết quả kịch bản tắt / bật service.

---

## 3. Khi phải chờ người khác: dùng bản giả

Cột "Chờ ai khi kiểm thử" ở mục 2 là các chỗ cần việc của người khác. **Đừng ngồi chờ.** Viết code theo hợp đồng `03-hop-dong-api.md` và tạm thay phần chưa có bằng bản giả trả dữ liệu cứng.

| Chỗ chờ | Bản giả | Đặt ở |
|---|---|---|
| Service gọi service (ví dụ Reminder cần API của Scheduling) | Lớp client giả, cùng interface với client thật, trả dữ liệu cứng lấy từ dữ liệu mẫu (`02-co-so-du-lieu.md` mục 5), bật bằng một profile cấu hình (ví dụ `fake-clients`) | thư mục service **của mình** — không sửa thư mục người khác |
| Trang web cần API chưa có | Service Angular trả dữ liệu cứng đúng khuôn response trong `03-hop-dong-api.md` | thư mục trang của mình |
| Sự kiện RabbitMQ chưa có ai phát | Tự gửi tin mẫu theo `03-hop-dong-api.md` A7 từ trang quản trị RabbitMQ (`http://localhost:15672`) | — |

Quy tắc:
1. Bản giả trả **đúng khuôn trong `03-hop-dong-api.md`** (tên trường, kiểu, mã lỗi). Đó là hợp đồng; bản giả lệch hợp đồng thì khi ghép sẽ vỡ.
2. Bản giả **không được bật mặc định**: `docker compose up` trên `main` phải chạy bản thật.
3. Khi việc kia vào `main`: tắt profile, chạy lại *Xong khi* bằng bản thật, rồi xóa bản giả trong một PR riêng.

---

## 4. Cách làm một việc đến khi xong

1. Tạo issue tên theo mã (ví dụ `P2-3 Tính slot trống`), chép *Xong khi* vào thân issue.
2. Tạo nhánh `feature/<service>-<việc>` từ `main` mới nhất.
3. Code theo đặc tả (`01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 6.1) + hợp đồng (`03-hop-dong-api.md`) + chính sách (`01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 7).
4. Tự kiểm *Xong khi* **qua Gateway** (`http://localhost:8080/...`) với `docker compose up -d --build`.
5. Mở PR vào `main`, ghi `Closes #<số issue>`, nhờ một người khác duyệt.

**Một việc được coi là xong khi:** API không còn trả 501 · gọi được qua Gateway · dữ liệu và mã lỗi đúng `03-hop-dong-api.md` · đạt *Xong khi* · PR có một người khác duyệt và `main` vẫn chạy sau khi merge.

**Gặp câu nghiệp vụ chưa có trong `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 7:** tự quyết theo cách hợp lý nhất, ghi quyết định vào mô tả PR và thêm một dòng vào `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 7 trong cùng PR.

**Phải đổi hợp đồng** (đường dẫn, tên trường, mã lỗi, cột): sửa `03-hop-dong-api.md` / `02-co-so-du-lieu.md` trong cùng PR, nhắc các phần đang dùng vào xem và đồng ý trước khi merge. Sửa cột của bảng đã có: theo `02-co-so-du-lieu.md` mục 6.

---

## 5. Quy ước khi code

### 5.1. Tìm chỗ cần viết

Mọi chỗ chờ viết trong backend ném `NotImplementedYetException("Chưa làm: …")`, gọi API sẽ nhận **501** kèm mô tả. Liệt kê các chỗ còn chờ của một service:

```powershell
Get-ChildItem .\scheduling-service\src -Recurse -Filter *.java | Select-String "NotImplementedYetException\("
```

### 5.2. Phạm vi sửa

- Mỗi phần chỉ sửa trong thư mục service của mình và các trang web của mình (`frontend/src/app/features/...`).
- Các file dùng chung — `docker-compose.yml`, `pom.xml` gốc, `02-co-so-du-lieu.md` `03-hop-dong-api.md` `frontend/src/app/core/app-pages.ts` — chỉ sửa sau khi thống nhất, trong một Pull Request riêng.
- Đổi đường dẫn API, tên trường JSON, mã lỗi hay cấu trúc bảng là **đổi hợp đồng**: phải báo các phần đang dùng nó (cột "Đầu mối thường xuyên với" ở mục 1) và sửa tài liệu cùng lúc.

### 5.3. Git

- Nhánh `main` luôn phải chạy được `docker compose up -d --build`.
- Mỗi việc một nhánh `feature/<service>-<việc>`, ví dụ `feature/scheduling-hold-slot`; chỉ sửa tài liệu thì đặt `docs/<nội-dung>`. Xong thì mở Pull Request vào `main`, có ít nhất một thành viên khác duyệt.
- Không commit `.env`, `target/`, `node_modules/`.

### 5.4. Dùng AI hỗ trợ viết code

Khung đã cố định công nghệ và hợp đồng; câu lệnh gửi AI cần nêu đủ các ràng buộc:

```
Dự án Spring Boot 4.0.7, Java 25, Jakarta Persistence, Lombok, MySQL 8.4.
Tôi phụ trách <tên service>, chỉ được sửa trong thư mục <tên-service>/.
Bảng dữ liệu: docs/02-co-so-du-lieu.md. Hợp đồng API: docs/03-hop-dong-api.md.
Đặc tả chức năng và chính sách: docs/01-nghiep-vu-va-dac-ta-chuc-nang.md mục 6, 7.
Viết phần thân phương thức <Lớp>.<phương thức> đang ném NotImplementedYetException.
- Giữ nguyên đường dẫn, tên trường DTO, mã lỗi như tài liệu.
- Lỗi nghiệp vụ: throw new BusinessException(HttpStatus.xxx, "MA_LOI", "thông báo tiếng Việt").
- Không sửa schema.sql, không thêm thư viện khi chưa thống nhất.
- Không đọc database của service khác; cần dữ liệu thì gọi API nội bộ qua lớp trong package client/.
```

Sau khi AI sinh code: đọc lại phần thay đổi (`git diff`), build lại service, thử trên Swagger rồi mới commit. Các lỗi hay gặp: viết theo Spring Boot 2/3 cũ (`javax.*` thay vì `jakarta.*`), tự đổi tên trường, tự thêm thư viện.

---

## 6. Việc chung khi hoàn thiện

**Việc chung của cả nhóm** (làm cuối): mỗi người viết mục báo cáo của phần mình (bảng dữ liệu, API, sơ đồ tuần tự) · báo cáo phần chung (kiến trúc tổng thể, sơ đồ luồng dữ liệu, đối chiếu yêu cầu đề tài và giáo trình) — nhóm chia khi tới lúc · tổng duyệt demo cả nhóm.

| Việc | Nội dung |
|---|---|
| Báo cáo | Mỗi phần viết mục của mình (bảng dữ liệu, API, sơ đồ); phần chung: kiến trúc tổng thể, đối chiếu yêu cầu đề tài (`00-bat-dau-tu-day.md` mục 7.2), đối chiếu giáo trình (`00-bat-dau-tu-day.md` mục 7.3) |
| Sơ đồ theo giáo trình | ERD tách theo từng service, không khóa ngoại chéo (88–89); sơ đồ tuần tự phân biệt lời gọi đồng bộ (nét liền, có mũi tên trả về) và thông điệp bất đồng bộ qua broker (nét đứt) (106–107); DFD có Message Broker là một thành phần (107) |
| Demo | Chạy trọn 5 luồng trên web; tắt Reminder / Provider để thể hiện chịu lỗi |
| Đóng gói | Chỉ dùng `docker-compose.yml` (không có file dev), web ở cổng 4200 |
