# 01. Nghiệp vụ và đặc tả chức năng

**Tên đề tài:** Nền tảng đặt lịch khám đa khoa dựa trên kiến trúc Microservices
**Hướng đề tài:** Hướng 4 — Hệ thống Lập lịch & Xử lý ràng buộc thời gian

Đọc file này khi code một chức năng: hệ thống làm gì, mỗi chức năng `F..` chạy ra sao (mục 6) và theo chính sách `CS..` nào (mục 7). `CT..` là chứng từ (dữ liệu ghi lại một việc — [`02-co-so-du-lieu.md`](02-co-so-du-lieu.md) mục 7). Tư liệu cho báo cáo (yêu cầu đề tài, đối chiếu giáo trình, Petclinic): [`00-bat-dau-tu-day.md`](00-bat-dau-tu-day.md) mục 7.

## Mục lục

1. Bài toán và người dùng
2. Kiến trúc
3. Module và chức năng
4. Quy tắc định danh bệnh nhân (MPI)
5. Các luồng chính
6. Đặc tả chức năng trên hệ thống
7. Chính sách nghiệp vụ đã chốt
8. Ngoài phạm vi

---

## 1. Bài toán và người dùng

### 1.1. Bài toán

Một phòng khám đa khoa có nhiều chuyên khoa, mỗi chuyên khoa có nhiều bác sĩ, mỗi bác sĩ có lịch làm việc riêng.

| Vấn đề | Cách hệ thống giải quyết |
|---|---|
| Bệnh nhân phải đến xếp hàng lấy số | Đặt lịch trực tuyến theo chuyên khoa, bác sĩ, khung giờ |
| Hai người đặt trùng một khung giờ của cùng một bác sĩ | Giữ chỗ tạm và chặn đặt trùng bằng ràng buộc của cơ sở dữ liệu |
| Bệnh nhân quên lịch hẹn | Tác vụ định kỳ tự gửi email nhắc trước giờ khám |
| Một người có nhiều hồ sơ rời rạc (đặt hộ, trẻ em chưa có CCCD, đến quầy không mang giấy tờ, gõ nhầm) | Quản lý định danh bệnh nhân — **MPI** (Master Patient Index): phát hiện hồ sơ nghi trùng và gộp hồ sơ **khi có lễ tân duyệt** |

### 1.2. Người dùng

| Vai trò | Ai | Chức năng |
|---|---|---|
| `PATIENT` | Bệnh nhân hoặc người nhà đặt hộ | Đăng ký, khai hồ sơ bệnh nhân, đặt / hủy lịch, xem lịch của mình. Một tài khoản quản lý được nhiều hồ sơ (bản thân, con, người nhà) |
| `RECEPTIONIST` | Lễ tân | Tạo hồ sơ cho bệnh nhân đến quầy, bổ sung giấy tờ, duyệt hồ sơ nghi trùng, gộp / tách hồ sơ, đặt / hủy lịch hộ |
| `DOCTOR` | Bác sĩ | Xem lịch khám trong ngày của mình, đánh dấu đã khám |
| `ADMIN` | Quản lý phòng khám | Quản lý chuyên khoa, bác sĩ, phòng, lịch làm việc, tài khoản |

---

## 2. Kiến trúc

### 2.1. Sơ đồ tổng thể

```
                           Web (Angular) — http://localhost:4200
                                        │
                                        ▼
        ┌──────────────────────────────────────────────────────────────┐
        │ API Gateway (8080): định tuyến, xác minh token Keycloak,     │
        │ gắn danh tính, Circuit Breaker, API Composition              │
        └──────────────────────────────────────────────────────────────┘
            │            │            │             │             │
            ▼            ▼            ▼             ▼             ▼
      Auth Service   Provider      Patient      Scheduling     Reminder
                     Service       Service      Service        Service
                                   (MPI)                       (thông báo)
            │            │            │             │             │
            ▼            ▼            ▼             ▼             ▼
         auth_db    provider_db   patient_db   scheduling_db  reminder_db

  Gọi đồng bộ (REST nội bộ)   Scheduling → Provider (giờ làm việc)
                              Reminder → Scheduling (lịch sắp tới) · Reminder → Patient (email)
                              Gateway → Scheduling, Provider, Patient (ghép dữ liệu)
                              Auth → Keycloak (tạo / khóa người dùng)
  Sự kiện (RabbitMQ)          Patient ──gộp / tách hồ sơ──► Scheduling
                              Scheduling ──đã xác nhận / đã hủy lịch──► Reminder ──email──► Mailpit
  Hạ tầng                     Eureka (danh bạ service) · Keycloak (đăng nhập, keycloak_db) · MySQL · RabbitMQ · Mailpit
```

### 2.2. Nguyên tắc thiết kế

| Nguyên tắc | Thể hiện trong hệ thống |
|---|---|
| **Một cổng vào** | Web chỉ gọi Gateway. API nội bộ giữa các service có dạng `/internal/v1/...` và không có route trên Gateway, nên không gọi được từ bên ngoài |
| **Database per Service** | Mỗi service có cơ sở dữ liệu riêng và tài khoản MySQL riêng chỉ có quyền trên cơ sở dữ liệu đó. Bảng chỉ lưu **mã** của thực thể thuộc service khác (`patient_id`, `doctor_id`), không có khóa ngoại chéo |
| **Hợp đồng trước (contract-first)** | Toàn bộ đường dẫn, dữ liệu vào/ra, mã lỗi, sự kiện được định nghĩa trong `03-hop-dong-api.md` trước khi viết phần xử lý |
| **Giao tiếp đồng bộ khi cần kết quả ngay** | REST qua `RestClient` / `WebClient` gọi theo tên service (Eureka + LoadBalancer): Gateway ghép dữ liệu, Scheduling hỏi Provider giờ làm việc, Reminder hỏi Scheduling và Patient |
| **Giao tiếp bất đồng bộ khi chỉ cần báo tin** | Patient Service phát sự kiện "đã gộp / đã tách hồ sơ" qua RabbitMQ, Scheduling Service tự lắng nghe và cập nhật lịch hẹn; Scheduling phát sự kiện "đã xác nhận / đã hủy lịch", Reminder Service nhận để gửi thư |
| **Chống lỗi lan truyền** | Circuit Breaker + Fallback tại Gateway; Reminder tắt thì việc đặt lịch không bị ảnh hưởng; tin nhắn xử lý lỗi vào Dead Letter Queue |

**Đăng nhập dùng Keycloak** (máy chủ quản lý đăng nhập mã nguồn mở) làm Identity Provider (giáo trình trang 40–41; trang 87–88 xếp xác thực vào Generic Domain, nên dùng giải pháp có sẵn). Keycloak giữ mật khẩu, vai trò, cấp token. Auth Service giữ bảng `accounts` làm nguồn mã tài khoản (số) và `doctorId`; đăng ký và quản trị tạo / khóa tài khoản thì gọi Keycloak Admin API, có bù trừ khi một bên lỗi. Gateway xác minh token rồi gắn `X-User-Id` (từ claim `accountId`), `X-User-Role`, `X-Doctor-Id` ([`03-hop-dong-api.md`](03-hop-dong-api.md) mục A5), nên các service khác chỉ đọc header.

### 2.3. Các service

| Service | Ứng với yêu cầu Hướng 4 | Trách nhiệm | Dữ liệu sở hữu |
|---|---|---|---|
| **API Gateway** | Tiêu chí 1 | Định tuyến, xác minh token Keycloak, gắn danh tính, chặn theo vai trò, API Composition, mã truy vết request | Không có |
| **Keycloak** | (hạ tầng) | Identity Provider: trang đăng nhập, giữ mật khẩu, vai trò, cấp token | `keycloak_db` |
| **Auth Service** | (bổ sung) | Cấp mã tài khoản, đăng ký, quản trị tạo / khóa tài khoản qua Keycloak Admin API | Tài khoản (mã số, vai trò, `doctorId`, liên kết người dùng Keycloak) |
| **Provider Service** | Provider/Resource Service | Chuyên khoa, bác sĩ, phòng khám, ca làm việc hằng tuần | Chuyên khoa, bác sĩ, phòng, ca làm việc |
| **Patient Service (MPI)** | Consumer Service | Hồ sơ bệnh nhân, chống tạo trùng, hàng chờ duyệt, gộp / tách hồ sơ | Hồ sơ, cặp nghi trùng, lịch sử gộp / tách |
| **Scheduling Service** | Scheduling Service | Tìm slot trống, giữ chỗ, xác nhận, hủy, chống đặt trùng | Lịch hẹn, lịch hẹn đã chuyển hồ sơ, sự kiện đã xử lý |
| **Reminder Service** | Reminder/Cron Service | Dịch vụ thông báo — nhắc lịch (cron quét lịch sắp tới), thư xác nhận, thư báo hủy; nhận sự kiện từ Scheduling | Nhật ký thư đã gửi, mẫu thư, sự kiện đã xử lý |

---

## 3. Module và chức năng

### 3.1. Module khối

| Mã | Module khối | Service | Ngoài đời là việc của | Chứng từ | Phần |
|---|---|---|---|---|---|
| M1 | Danh mục và lịch làm việc | Provider Service | Quản lý phòng khám | CT01–CT04 | Vinh |
| M2 | Hồ sơ bệnh nhân (MPI — quản lý định danh bệnh nhân) | Patient Service | Quầy lễ tân, lưu trữ hồ sơ | CT07–CT09, CT20, CT21, CT23 | Như |
| M3 | Hẹn khám | Scheduling Service; hai màn hình ghép ở Gateway | Lễ tân (sổ hẹn), bác sĩ | CT10–CT13, CT16, CT17, CT19, CT22 | Vũ; phần ghép: Lập |
| M4 | Thông báo (nhắc lịch, xác nhận, báo hủy) | Reminder Service | Lễ tân gọi điện xác nhận, nhắc hôm trước, báo hủy | CT14, CT15, CT24, CT25 | Hiếu |
| M5 | Tài khoản và đăng nhập | Auth Service + Keycloak | Quản lý cấp tài khoản | CT05, CT06 | đã làm sẵn |

**Thành phần hạ tầng** (không phải module nghiệp vụ, không có quy trình riêng): API Gateway (cổng vào duy nhất: xác minh đăng nhập, chặn theo vai trò, ghép dữ liệu — Lập) · Keycloak (máy chủ đăng nhập) · Eureka (danh bạ service) · RabbitMQ (hàng đợi sự kiện) · MySQL · Mailpit (hộp thư thử) · Web Angular.

Phụ thuộc giữa các module khối (mũi tên = "cần dữ liệu của"):

```
M3 Hẹn khám ──(giờ làm việc, đồng bộ)──► M1 Danh mục
M3 Hẹn khám ◄──(sự kiện gộp / tách)──── M2 Hồ sơ
M4 Thông báo ──(lịch sắp tới, đồng bộ)─► M3 ; ──(email hồ sơ, đồng bộ)─► M2
M4 Thông báo ◄──(sự kiện đã xác nhận / đã hủy)── M3 Hẹn khám
Gateway (ghép) ──► M3, M1, M2
M5 Tài khoản: cấp accountId cho mọi module qua token đăng nhập; không module nào gọi M5 trực tiếp
```

Không có vòng phụ thuộc đồng bộ: M1 và M2 không gọi ai, M3 chỉ gọi M1, M4 gọi M3 và M2. M3 báo cho M4 bằng sự kiện (bất đồng bộ), nên M3 không phải chờ M4 và vẫn chạy khi M4 tắt.

### 3.2. Module chi tiết (chức năng con)

Mỗi chức năng con là một việc, một vai trò làm, ra một kết quả rõ.

| Mã | Chức năng con | Vai trò làm | Kết quả |
|---|---|---|---|
| **M1** | | | |
| F11 | Quản lý chuyên khoa, phòng, bác sĩ | Quản lý | CT01, CT02, CT03 |
| F12 | Lập ca làm việc của bác sĩ | Quản lý | CT04 |
| F13 | Xem chuyên khoa, bác sĩ, ca làm việc | Khách, mọi vai trò | danh sách để chọn bác sĩ |
| F14 | Cung cấp giờ làm việc trong ngày (nội bộ) | Hệ thống (cho M3) | ca của bác sĩ trong ngày |
| F15 | Cung cấp bác sĩ, phòng theo danh sách mã (nội bộ) | Hệ thống (cho Gateway) | tên bác sĩ, phòng |
| **M2** | | | |
| F21 | Khai hồ sơ (bản thân, người nhà) | Bệnh nhân | CT07 |
| F22 | Tìm, xem, sửa, tạo hồ sơ tại quầy | Lễ tân (xem: cả bác sĩ) | CT07 |
| F23 | Bổ sung giấy tờ cho hồ sơ tạm | Lễ tân | CT09 |
| F24 | Dò hồ sơ nghi trùng | Hệ thống (trong F21, F22, F23) | CT20 (đề nghị) |
| F25 | Duyệt hàng chờ: gộp hoặc xác nhận không trùng | Lễ tân | CT20 (quyết định), CT21 |
| F26 | Tách hồ sơ gộp nhầm, xem lịch sử gộp / tách | Lễ tân | CT23 |
| F27 | Cung cấp hồ sơ rút gọn (nội bộ) | Hệ thống (cho Gateway, M4) | tên, ngày sinh, email |
| **M3** | | | |
| F31 | Xem slot trống | Bệnh nhân, Lễ tân | CT10 |
| F32 | Giữ chỗ | Bệnh nhân, Lễ tân | CT11 |
| F33 | Xác nhận lịch | Người đã giữ chỗ | CT12 |
| F34 | Nhả chỗ giữ quá hạn | Hệ thống (định kỳ) | CT11 → hết hạn |
| F35 | Hủy lịch (cũng là cách đổi lịch, CS13) | Người đặt, Lễ tân | CT13 |
| F36 | Xem lịch khám của tôi | Bệnh nhân | CT12 kèm tên (ghép ở Gateway) |
| F37 | Xem danh sách khám trong ngày / lịch hẹn theo bác sĩ | Bác sĩ, Lễ tân | CT16 (ghép ở Gateway) |
| F38 | Đánh dấu đã khám | Bác sĩ, Lễ tân | CT17 |
| F39 | Ghi bệnh nhân không đến | Bác sĩ, Lễ tân | CT19 |
| F3A | Chuyển lịch hẹn khi hồ sơ được gộp / tách | Hệ thống (theo sự kiện) | CT22 |
| F3B | Cung cấp lịch hẹn (nội bộ) | Hệ thống (cho Gateway, M4) | danh sách lịch |
| **M4** | | | |
| F41 | Quét và gửi nhắc lịch; thử lại thư gửi lỗi | Hệ thống (mỗi 5 phút) | CT15 |
| F42 | Xem nhật ký thông báo, quét ngay | Quản lý (xem: cả lễ tân) | nhật ký CT14, CT15, CT24 |
| F43 | Gửi email xác nhận khi lịch được xác nhận | Hệ thống (theo sự kiện) | CT14 |
| F44 | Gửi thư báo hủy khi lịch bị hủy | Hệ thống (theo sự kiện) | CT24 |
| F45 | Quản lý mẫu thư | Quản lý | CT25 |
| **M5** | | | |
| F51 | Đăng ký tài khoản bệnh nhân | Khách | CT06 |
| F52 | Đăng nhập, đăng xuất | Mọi vai trò | token đăng nhập |
| F53 | Cấp, khóa, mở tài khoản nhân viên | Quản lý | CT05 |
| F54 | Xem tài khoản của tôi | Mọi vai trò | thông tin tài khoản |

---

## 4. Quy tắc định danh bệnh nhân (MPI)

**Trạng thái hồ sơ:**

```
TEMPORARY (hồ sơ tạm) ──bổ sung giấy tờ, không trùng ai──► ACTIVE (đang dùng)
ACTIVE / TEMPORARY ──lễ tân duyệt gộp vào hồ sơ khác──► MERGED (đã gộp)
MERGED ──lễ tân tách do gộp nhầm──► trạng thái trước khi gộp
```

**Nguyên tắc cốt lõi: hệ thống không bao giờ tự gộp hai hồ sơ.** Khi phát hiện trùng, hệ thống chỉ đưa cặp hồ sơ vào hàng chờ duyệt; lễ tân quyết định.

**Tạo hồ sơ và bổ sung giấy tờ theo tình huống** (họ tên so sánh sau khi chuẩn hóa: bỏ dấu, viết thường):

| # | Tình huống | Hệ thống xử lý | Kết quả API |
|---|---|---|---|
| 1 | Có giấy tờ; giấy tờ đã thuộc hồ sơ `ACTIVE`; họ tên và ngày sinh khớp | Dùng lại hồ sơ có sẵn | `POST /patients` → 200 `REUSED_EXISTING` |
| 2 | Có giấy tờ; giấy tờ đã thuộc hồ sơ `ACTIVE`; họ tên hoặc ngày sinh không khớp | Tạo hồ sơ `TEMPORARY`, đưa cặp vào hàng chờ (`SAME_ID_DIFFERENT_INFO`); vẫn đặt lịch được bằng hồ sơ tạm | 201 `CREATED_TEMPORARY`, `duplicateQueued = true` |
| — | Có giấy tờ chưa thuộc hồ sơ nào | Tạo hồ sơ `ACTIVE` | 201 `CREATED_ACTIVE` |
| 3 | Không có giấy tờ (trẻ em dưới 14 tuổi, người cao tuổi được đặt hộ, cấp cứu, quên giấy tờ) | Tạo hồ sơ `TEMPORARY`; nếu trùng họ tên + ngày sinh + số điện thoại với hồ sơ có sẵn thì đưa cặp vào hàng chờ (`SAME_NAME_DOB_PHONE`) | 201 `CREATED_TEMPORARY`, `duplicateQueued` = có trùng hay không |
| 4 | Hồ sơ tạm được bổ sung giấy tờ chưa thuộc ai | Chuyển sang `ACTIVE` | `POST /patients/{id}/identity` → hồ sơ `ACTIVE` |
| 5 | Hồ sơ tạm được bổ sung giấy tờ đã thuộc hồ sơ `ACTIVE` khác | Không tự gộp; giữ `TEMPORARY`, đưa cặp vào hàng chờ (`ID_ADDED_BELONGS_TO_OTHER`) | `POST /patients/{id}/identity` → hồ sơ vẫn `TEMPORARY` |

---

## 5. Các luồng chính

### 5.1. Luồng xử lý trên hệ thống

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

**Luồng 5 — Thông báo: nhắc lịch, xác nhận, báo hủy**
```
Cron trong Reminder chạy mỗi 5 phút
  → Scheduling: lịch CONFIRMED trong 24 giờ tới
  → bỏ các lịch đã nhắc (nhật ký của chính Reminder)
  → Patient: email của hồ sơ → gửi email → ghi nhật ký

Scheduling xác nhận / hủy lịch → phát APPOINTMENT_CONFIRMED / APPOINTMENT_CANCELLED (RabbitMQ)
  → Reminder nhận, lấy email từ Patient → gửi thư xác nhận / báo hủy → ghi nhật ký
```

Chọn giao tiếp bất đồng bộ cho luồng 4 vì Patient không cần biết service nào đang dùng `patient_id`: nếu Scheduling tạm tắt, sự kiện nằm chờ trong hàng đợi và được xử lý khi Scheduling bật lại, thao tác gộp của lễ tân không bị ảnh hưởng.

### 5.2. Quy trình xuyên module

Hai quy trình chạy qua nhiều module, dùng làm luồng demo chính.

**Q1 — Từ đăng ký đến khám xong** (khớp kịch bản KB2 ở `02-co-so-du-lieu.md` mục 8)

| # | Ai | Làm gì | Giấy vào | Giấy ra | Module |
|---|---|---|---|---|---|
| 1 | Khách | Đăng ký tài khoản | — | CT06 | M5 |
| 2 | Bệnh nhân | Khai hồ sơ cho mình hoặc người nhà | CT08 (nếu có) | CT07 | M2 |
| 3 | Bệnh nhân | Chọn chuyên khoa, bác sĩ, xem giờ còn trống | CT01, CT03, CT04 | CT10 | M1 → M3 |
| 4 | Bệnh nhân | Chọn giờ, giữ chỗ rồi xác nhận | CT10, CT07 | CT11 → CT12 | M3 |
| 5 | Phòng khám | Gửi thư xác nhận lịch hẹn | CT12, email CT07, CT25 | CT14 | M4 |
| 6 | Phòng khám | Nhắc bệnh nhân trước giờ khám | CT12, email CT07, CT25 | CT15 | M4 |
| 7 | Bác sĩ | Xem danh sách khám trong ngày | CT12 | CT16 | M3 |
| 8 | Bác sĩ | Khám xong đánh dấu, hoặc ghi không đến | CT16 | CT17 / CT19 | M3 |

Nhánh hủy: ở bất kỳ lúc nào trước bước 7, người đặt hoặc lễ tân hủy lịch (CT13, M3) → phòng khám gửi thư báo hủy (CT24, M4).

**Q2 — Từ hồ sơ trùng đến lịch hẹn đi theo hồ sơ** (khớp KB6)

| # | Ai | Làm gì | Giấy vào | Giấy ra | Module |
|---|---|---|---|---|---|
| 1 | Bệnh nhân / Lễ tân | Khai hồ sơ trùng thông tin với hồ sơ có sẵn | CT07 | CT20 (đề nghị) | M2 |
| 2 | Lễ tân | So sánh hai hồ sơ, quyết định gộp | CT20 | CT21 | M2 |
| 3 | Phòng khám | Lịch hẹn của hồ sơ bị gộp chuyển sang hồ sơ giữ lại | CT21 | CT22 | M3 |
| 4 | Lễ tân | Phát hiện gộp nhầm, tách lại | CT21 | CT23 | M2 |
| 5 | Phòng khám | Lịch hẹn trả về hồ sơ cũ | CT23 | CT22 (hoàn lại) | M3 |

---

## 6. Đặc tả chức năng trên hệ thống

Bảng mục 6.1 là đặc tả để code: đọc cùng [`03-hop-dong-api.md`](03-hop-dong-api.md) (hình dạng request / response) và [`02-co-so-du-lieu.md`](02-co-so-du-lieu.md) (cột).

### 6.1. Từng chức năng con trên hệ thống

Quyền: **(GW)** = Gateway chặn theo vai trò trước; còn lại service tự kiểm bằng `X-User-Role`, `X-User-Id`, `X-Doctor-Id`. "Trạng thái" ghi trạng thái chứng từ sau bước đó.

| Mã | Trang web (`path`) | API | Nhập | Hệ thống tự kiểm / tự tính | Trạng thái | Quyền | Đầu ra |
|---|---|---|---|---|---|---|---|
| F11 | *Chuyên khoa, bác sĩ, phòng* (`admin/catalog`) | `POST/PUT /specialties`, `/rooms`, `/doctors`; `GET /rooms` | mã, tên, mô tả, vị trí; bác sĩ: họ tên, học vị, chuyên khoa, liên hệ | mã không trùng → 409 `CODE_TAKEN`; chuyên khoa tồn tại | `active` | ADMIN (GW) | CT01–CT03 |
| F12 | *Lịch làm việc bác sĩ* (`admin/work-schedules`) | `GET/POST /doctors/{id}/work-schedules`, `PUT/DELETE /work-schedules/{id}` | phòng, thứ, giờ, `slotMinutes`, ngày áp dụng | `start < end`; không chồng ca → 409 `SCHEDULE_OVERLAP`; xóa = `active = false` | `active` | ADMIN (GW) | CT04 |
| F13 | *Chuyên khoa & bác sĩ* (`doctors`) | `GET /specialties`, `/doctors?specialtyId=`, `/doctors/{id}`, `/doctors/{id}/work-schedules` | chọn chuyên khoa | chỉ trả mục đang hoạt động | — | Khách | danh sách |
| F14 | — | nội bộ `GET /internal/v1/doctors/{id}/working-hours?date=` | — | ca có `day_of_week` = thứ của ngày, `active`, trong khoảng áp dụng; không có → `shifts` rỗng | — | Scheduling | giờ làm việc |
| F15 | — | nội bộ `GET /internal/v1/doctors?ids=`, `/internal/v1/rooms?ids=` | — | — | — | Gateway | tên bác sĩ, phòng |
| F21 | *Hồ sơ bệnh nhân của tôi* (`my-patients`) | `POST /patients`, `GET /patients/mine`, `PUT /patients/{id}` | thông tin CT07 | F24; CCCD 12 số → 400 `INVALID_ID_NUMBER`; giám hộ (CS19) → 400 `GUARDIAN_REQUIRED`; chuẩn hóa SĐT (CS20); `owner_account_id` = người đăng nhập | `TEMPORARY` / `ACTIVE` | PATIENT; sửa: chủ hồ sơ | CT07 |
| F22 | *Hồ sơ bệnh nhân* (`patients`) | `GET /patients?keyword=&status=`, `GET/PUT /patients/{id}`, `POST /patients` | từ khóa; thông tin CT07 | như F21; tạo tại quầy thì `owner_account_id` để trống; hồ sơ đã gộp không sửa → 409 `PATIENT_MERGED` | như F21 | RECEPTIONIST; xem: cả DOCTOR | CT07 |
| F23 | *Hồ sơ bệnh nhân* (`patients`) | `POST /patients/{id}/identity` | loại, số giấy tờ | chỉ hồ sơ tạm → 409 `NOT_TEMPORARY`; F24 | `TEMPORARY` → `ACTIVE`, hoặc giữ `TEMPORARY` + cặp mới | RECEPTIONIST | CT09 |
| F24 | — (chạy trong F21, F22, F23) | — | — | bảng tình huống mục 4; tính `score` (`MatchScorer`) chỉ để tham khảo | cặp `PENDING` | Hệ thống | CT20 |
| F25 | *Hàng chờ duyệt hồ sơ trùng* (`duplicate-queue`) | `GET /duplicate-candidates`, `/count`, `POST …/{id}/merge`, `POST …/{id}/reject` | chọn hồ sơ giữ lại, ghi chú | cặp đã duyệt → 409 `CANDIDATE_ALREADY_REVIEWED`; hồ sơ giữ lại phải thuộc cặp → 400; **phát `PATIENT_MERGED` sau khi lưu xong** | cặp `MERGED` / `NOT_DUPLICATE`; hồ sơ bị gộp `MERGED` | RECEPTIONIST (GW) | CT20, CT21 + sự kiện |
| F26 | *Hàng chờ duyệt hồ sơ trùng* (nút tách) | `POST /patients/{id}/unmerge`, `GET /patients/{id}/merge-history` | ghi chú | hồ sơ phải `MERGED` → 409 `NOT_MERGED`; trả về `source_previous_status`; cặp → `NOT_DUPLICATE` (CS24); phát `PATIENT_UNMERGED` | hồ sơ về trạng thái cũ | RECEPTIONIST | CT23 + sự kiện |
| F27 | — | nội bộ `GET /internal/v1/patients?ids=` | — | — | — | Gateway, Reminder | hồ sơ rút gọn có email |
| F31 | *Đặt lịch khám* (`booking`) | `GET /slots?doctorId=&date=` | bác sĩ, ngày | gọi F14; cắt ca theo `slotMinutes`, bỏ phần lẻ (CS11); `available = false` nếu đã có người giữ / đặt, đã qua giờ, ngoài 30 ngày (CS05), hoặc dưới 2 giờ với PATIENT (CS06); Provider lỗi → 503 | — | đã đăng nhập | CT10 |
| F32 | *Đặt lịch khám* (`booking`) | `POST /appointments/hold` | bác sĩ, hồ sơ, giờ bắt đầu, lý do | nhả chỗ quá hạn trước; giờ thuộc ca → 400 `SLOT_NOT_IN_SCHEDULE`; trùng slot → 409 `SLOT_ALREADY_HELD` (database chặn bằng `slot_lock`); hồ sơ chồng giờ → 409 `PATIENT_TIME_CONFLICT`; CS03, CS04, CS05, CS06; chép `room_id`, `work_schedule_id`, tính `end_time`, `hold_expires_at` | `HELD` | PATIENT, RECEPTIONIST | CT11 |
| F33 | *Đặt lịch khám* (`booking`) | `POST /appointments/{id}/confirm` | — | không phải người đã giữ chỗ → 403; quá hạn → 409 `HOLD_EXPIRED` (CS02); lưu xong thì phát sự kiện `appointment.confirmed` | `CONFIRMED` | người đã giữ chỗ | CT12 + sự kiện |
| F34 | — | không có API; chạy định kỳ | — | `HELD` có `hold_expires_at` đã qua → `EXPIRED`, nhả `slot_lock` | `EXPIRED` | Hệ thống | — |
| F35 | *Lịch khám của tôi* (`my-appointments`), *Lịch hẹn theo bác sĩ* (`appointments`) | `POST /appointments/{id}/cancel` | lý do | CS07 → 409 `TOO_LATE_TO_CANCEL`; CS08 → 403; không phải `CONFIRMED` → 409 `INVALID_STATUS`; nhả `slot_lock`; lưu xong thì phát sự kiện `appointment.cancelled` (kèm lý do) | `CANCELLED` | người đặt, RECEPTIONIST | CT13 + sự kiện |
| F36 | *Lịch khám của tôi* (`my-appointments`) | `GET /composite/my-appointments` (Gateway) | — | Gateway lấy hồ sơ của tài khoản (F27 / `mine`), lấy lịch theo người đặt hoặc theo hồ sơ (CS15, F3B), gọi song song F15, F27 để ghép tên; service ghép lỗi → trả danh sách, phần thiếu để `null` | — | đã đăng nhập | CT12 kèm tên |
| F37 | *Lịch khám trong ngày* (`doctor-agenda`), *Lịch hẹn theo bác sĩ* (`appointments`) | `GET /composite/doctor-agenda?doctorId=&date=` (Gateway); `GET /appointments?doctorId=&date=` | bác sĩ, ngày | bác sĩ chỉ xem `doctorId` của mình | — | DOCTOR, RECEPTIONIST | CT16 |
| F38 | *Lịch khám trong ngày* (nút "Đã khám"), *Lịch hẹn theo bác sĩ* | `POST /appointments/{id}/complete` | — | bác sĩ khác → 403; trước giờ bắt đầu → 409 `TOO_EARLY_TO_COMPLETE` (CS09); không phải `CONFIRMED` → 409 `INVALID_STATUS`; ghi `completed_by_account_id` | `COMPLETED` | DOCTOR (lịch của mình), RECEPTIONIST | CT17 |
| F39 | *Lịch khám trong ngày* (nút "Không đến"), *Lịch hẹn theo bác sĩ* | `POST /appointments/{id}/no-show` | — | như F38; trước giờ bắt đầu → 409 `TOO_EARLY_FOR_NO_SHOW` (CS10); ghi `no_show_by_account_id` | `NO_SHOW` | DOCTOR (lịch của mình), RECEPTIONIST | CT19 |
| F3A | — | nhận sự kiện `patient.merged`, `patient.unmerged` (RabbitMQ) | — | bỏ qua `eventId` đã có trong `processed_events`; gộp: chuyển mọi lịch của hồ sơ bị gộp (CS14), ghi `patient_reassignments`; tách: trả lại, ghi `reverted_at`; lỗi → hàng đợi lỗi (DLQ) | `patient_id` đổi | Hệ thống | CT22 |
| F3B | — | nội bộ `GET /internal/v1/appointments?bookedByAccountId=&patientIds=&doctorId=&status=&from=&to=` | — | mọi tham số không bắt buộc | — | Gateway, Reminder | danh sách lịch |
| F41 | — | `reminder.cron` mỗi 5 phút | — | gọi F3B (`CONFIRMED`, 24 giờ tới, CS28) → bỏ lịch đã có log `SENT` / `SKIPPED` → gọi F27 lấy email → điền mẫu `BEFORE_APPOINTMENT` (F45) → gửi thư → ghi log; không email → `SKIPPED` (CS30); lỗi gửi → `FAILED`, tăng `attempt_count`; mỗi lần quét cũng gửi lại các dòng `FAILED` của mọi loại thư (CS36); service khác lỗi → thử lại lần sau | `SENT` / `SKIPPED` / `FAILED` | Hệ thống | CT15 |
| F42 | *Nhật ký thông báo* (`admin/reminders`) | `GET /reminders/logs?status=&type=`, `POST /reminders/run` | lọc trạng thái, loại thư | 100 dòng mới nhất; "Quét ngay" chạy F41 một lần | — | xem: ADMIN, RECEPTIONIST; quét: ADMIN (GW) | `RunResult` |
| F43 | — | nhận sự kiện `appointment.confirmed` (RabbitMQ, hàng đợi `reminder.appointment-events`) | — | bỏ qua `eventId` đã có trong `processed_events`; gọi F27 lấy email; điền mẫu `CONFIRMATION` bằng dữ liệu trong sự kiện; gửi, ghi log loại `CONFIRMATION`; không email → `SKIPPED`; lỗi gửi → `FAILED` (F41 gửi lại); lỗi xử lý → hàng đợi lỗi | `SENT` / `SKIPPED` / `FAILED` | Hệ thống | CT14 |
| F44 | — | nhận sự kiện `appointment.cancelled` | — | như F43, mẫu `CANCELLATION`, điền thêm `cancelReason` | như F43 | Hệ thống | CT24 |
| F45 | *Mẫu thư* (`admin/message-templates`, trang mới) | `GET /reminders/templates`, `PUT /reminders/templates/{type}` | tiêu đề, nội dung có chỗ điền | 3 loại cố định, không thêm, không xóa; tiêu đề, nội dung không trống | — | ADMIN (GW) | CT25 |
| F51 | *Đăng ký* (`register`) | `POST /auth/register` | tên đăng nhập, mật khẩu, họ tên, email, SĐT | trùng → 409 `USERNAME_TAKEN` / `EMAIL_TAKEN`; Auth cấp `accountId` rồi tạo người dùng Keycloak, Keycloak lỗi thì xóa dòng vừa tạo (bù trừ) | `enabled` | Khách | CT06 |
| F52 | *Đăng nhập* (`login`) → trang Keycloak | Keycloak (OIDC — chuẩn đăng nhập); Gateway xác minh token | tên đăng nhập, mật khẩu | Keycloak kiểm mật khẩu, cấp token có `accountId`, `role`, `doctorId`; Gateway xóa `X-User-*` client tự gửi rồi gắn lại từ token | — | mọi vai trò | token |
| F53 | *Tài khoản* (`admin/accounts`) | `GET /accounts?role=`, `POST /accounts`, khóa / mở tài khoản | tên đăng nhập, họ tên, vai trò, bác sĩ (chọn từ danh sách), mật khẩu ban đầu | `DOCTOR` thiếu `doctorId` → 400 `DOCTOR_ID_REQUIRED`; tạo / khóa đồng thời ở Keycloak | `enabled` | ADMIN (GW) | CT05 |
| F54 | (đầu trang, mọi vai trò) | `GET /accounts/me` | — | — | — | đã đăng nhập | thông tin tài khoản |

### 6.2. Luồng chứng từ và trạng thái trong từng module

**M3 — vòng đời lịch hẹn** (cột `appointments.status`):

```
           giữ chỗ (F32)          xác nhận (F33)            đã khám (F38)
  (slot) ───────────────► HELD ─────────────────► CONFIRMED ─────────────► COMPLETED
                            │                        │   └──── không đến (F39) ──► NO_SHOW
               quá 5 phút   │                        │
                  (F34)     ▼                hủy (F35)▼
                         EXPIRED                 CANCELLED
```

`HELD`, `CONFIRMED`, `COMPLETED`, `NO_SHOW` giữ khóa slot (`slot_lock`); `EXPIRED`, `CANCELLED` nhả slot cho người khác.

**M2 — vòng đời hồ sơ và cặp nghi trùng:**

```
Hồ sơ:  TEMPORARY ──(bổ sung giấy tờ, F23)──► ACTIVE
        TEMPORARY / ACTIVE ──(gộp, F25)──► MERGED ──(tách, F26)──► trạng thái trước khi gộp
Cặp:    PENDING ──(gộp)──► MERGED ──(tách)──► NOT_DUPLICATE
        PENDING ──(không trùng)──► NOT_DUPLICATE
```

**M4 — một dòng nhật ký thông báo:** `FAILED` ──(lần quét sau, gửi được)──► `SENT`; `SKIPPED` là kết thúc. Mỗi lịch tối đa một dòng cho mỗi loại thư — nhắc, xác nhận, báo hủy (khóa duy nhất `appointment_id` + `reminder_type`).

**M1, M5:** danh mục và tài khoản chỉ có `active` / `enabled` (đang dùng hay ngừng). Không xóa dòng, để chứng từ cũ còn trỏ tới được.

### 6.3. Điểm nối giữa các service

| # | Từ → đến | Cách nối | Dữ liệu | Dùng ở | Khi bên kia lỗi |
|---|---|---|---|---|---|
| N1 | Scheduling → Provider | REST đồng bộ, nội bộ | giờ làm việc trong ngày | F31, F32 | trả 503, không treo (có timeout chung) |
| N2 | Patient → Scheduling | sự kiện RabbitMQ `patient.events` (bất đồng bộ) | gộp / tách hồ sơ | F25, F26 → F3A | Scheduling tắt thì tin nằm chờ trong hàng đợi, bật lại xử lý tiếp; Patient không chờ |
| N3 | Reminder → Scheduling | REST đồng bộ, nội bộ | lịch `CONFIRMED` trong 24 giờ | F41 | bỏ lần quét này, thử lại sau 5 phút |
| N4 | Reminder → Patient | REST đồng bộ, nội bộ | email của hồ sơ | F41 | như N3 |
| N5 | Gateway → Scheduling, Provider, Patient | REST đồng bộ, gọi song song, Circuit Breaker (bộ ngắt mạch) | lịch + tên bác sĩ, phòng, bệnh nhân | F36, F37 | vẫn trả danh sách, phần thiếu để `null` |
| N6 | Auth → Keycloak | Keycloak Admin API (API quản trị) | tạo / khóa người dùng | F51, F53 | báo lỗi, xóa dòng `accounts` vừa tạo (bù trừ) |
| N8 | Scheduling → Reminder | sự kiện RabbitMQ `appointment.events` (bất đồng bộ) | lịch đã xác nhận / đã hủy | F33, F35 → F43, F44 | Reminder tắt thì tin chờ trong hàng đợi, bật lại gửi tiếp; Scheduling không chờ |
| N7 | Web → Gateway → mọi service | REST qua cổng duy nhất, token Keycloak | mọi request của người dùng | mọi trang | Gateway trả lỗi dạng `ApiError` |

Mã tham chiếu chéo service (không có khóa ngoại, chỉ lưu số): `appointments.patient_id` → Patient · `appointments.doctor_id`, `room_id`, `work_schedule_id` → Provider · `appointments.booked_by_account_id`, `patients.owner_account_id`, `..._by_account_id` → Auth · `accounts.doctor_id` → Provider · `reminder_logs.appointment_id`, `patient_id` → Scheduling, Patient.

---

## 7. Chính sách nghiệp vụ đã chốt

Mọi giá trị dưới đây là giá trị dùng để code. Muốn đổi một chính sách: sửa mục này trong cùng PR, ghi lý do trong mô tả PR, các phần bị ảnh hưởng xem và đồng ý.

Chỉ ghi điều thuộc **nghiệp vụ ngoài đời, không hiển nhiên**. Việc hiển nhiên (kiểm định dạng email, không để trống tên…) người code tự quyết.

Hai nguyên tắc khi code:
1. **Con số đặt trong `application.yml`**, không viết cứng trong Java. Đổi chính sách thì chỉ sửa cấu hình.
2. **Giao dịch lưu giá trị lúc phát sinh** (bản chụp): lịch hẹn chép giờ, phòng, ca lúc giữ chỗ; sau này đổi ca không làm sai lịch cũ.

Mã lỗi theo dạng `ApiError` ([`03-hop-dong-api.md`](03-hop-dong-api.md) mục A4).

### 7.1. Scheduling Service (P2)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi |
|---|---|---|---|---|
| CS01 | Thời gian giữ chỗ | 5 phút | CT11 | `scheduling.hold-minutes: 5` |
| CS02 | Xác nhận đúng lúc hết hạn | Quá hạn khi `now > hold_expires_at`; đúng mốc vẫn xác nhận được | CT12 | 409 `HOLD_EXPIRED` |
| CS03 | Số chỗ đang giữ của một tài khoản | Tối đa 3 lịch `HELD` cùng lúc | CT11 | `scheduling.max-holds-per-account: 3` · 409 `TOO_MANY_HOLDS` |
| CS04 | Số lịch sắp tới của một hồ sơ | Tối đa 5 lịch `HELD` + `CONFIRMED` chưa tới giờ; kiểm lúc giữ chỗ | CT11 | `scheduling.max-upcoming-per-patient: 5` · 409 `TOO_MANY_APPOINTMENTS` |
| CS05 | Đặt trước tối đa | 30 ngày tính từ hôm nay | CT10, CT11 | `scheduling.book-ahead-days: 30` · `GET /slots` ngày ngoài khoảng trả mọi slot `available = false` · giữ chỗ trả 400 `TOO_FAR_IN_ADVANCE` |
| CS06 | Đặt sát giờ | Bệnh nhân không đặt được khi còn dưới 2 giờ tới giờ khám; lễ tân đặt được tới giờ bắt đầu (khách có mặt tại quầy) | CT10, CT11 | `scheduling.book-before-hours: 2` · `GET /slots` với vai trò `PATIENT` trả `available = false` cho các slot đó · giữ chỗ trả 400 `TOO_LATE_TO_BOOK` |
| CS07 | Hạn hủy | Bệnh nhân hủy được khi còn **từ 2 giờ trở lên** (đúng 2 giờ vẫn được); lễ tân hủy được tới giờ bắt đầu, không bị giới hạn 2 giờ | CT13 | `scheduling.cancel-before-hours: 2` · 409 `TOO_LATE_TO_CANCEL` |
| CS08 | Ai được hủy | Người đã đặt và lễ tân; **bác sĩ không hủy** | CT13 | 403 `FORBIDDEN` |
| CS09 | Đánh dấu đã khám | Chỉ từ giờ bắt đầu của lịch trở đi | CT17 | 409 `TOO_EARLY_TO_COMPLETE` |
| CS10 | Bệnh nhân không đến | Có trạng thái `NO_SHOW`. Bác sĩ (lịch của mình) hoặc lễ tân ghi; chỉ lịch `CONFIRMED`, chỉ sau giờ bắt đầu. Không có cron tự chuyển. Người không đến không bị phạt | CT19 | API mới `POST /appointments/{id}/no-show` · 409 `TOO_EARLY_FOR_NO_SHOW`, 409 `INVALID_STATUS` |
| CS11 | Ca không chia hết thời lượng slot | Bỏ phần lẻ cuối ca | CT10 | — |
| CS12 | Hai lịch liền kề của một hồ sơ (08:10–08:30 và 08:30–08:50) | **Không** tính là trùng giờ (khoảng nửa mở `[bắt đầu, kết thúc)`) | CT11 | — |
| CS13 | Đổi lịch | Không có API riêng: hủy lịch cũ rồi đặt lịch mới | CT13, CT11 | — |
| CS14 | Lịch hẹn khi gộp hồ sơ | Chuyển **mọi** lịch của hồ sơ bị gộp (mọi trạng thái) sang hồ sơ giữ lại; khi tách, trả lại đúng các lịch đã chuyển | CT22 | — |
| CS15 | "Lịch khám của tôi" gồm những lịch nào | Lịch do tài khoản đặt **hoặc** lịch của hồ sơ mà tài khoản đang quản lý (`owner_account_id`), kể cả lịch lễ tân đặt hộ | CT12 | API nội bộ có tham số `patientIds` (`03-hop-dong-api.md` B4); Gateway ghép ở `my-appointments` |
| CS37 | Mô hình hẹn | Hẹn theo khung giờ: **mỗi khung giờ hẹn (slot) một người**. Độ dài khung do từng ca làm việc quy định (`slot_minutes`, 5–120 phút); muốn khám nhanh như bệnh viện công (khoảng 10 phút một lượt) thì đổi dữ liệu ca, không sửa code. Không có khung chứa nhiều người, không bốc số. Xét nghiệm và lượt quay lại sau xét nghiệm nằm ngoài phạm vi | CT04, CT10, CT11 | `slot_lock` (UNIQUE) bảo đảm một khung một người |

### 7.2. Provider Service (P3)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi |
|---|---|---|---|---|
| CS16 | Sửa hoặc ngừng ca khi đã có lịch hẹn | Cho sửa / ngừng. Thay đổi chỉ ảnh hưởng slot tính từ lúc đó; lịch đã đặt **giữ nguyên** vì đã chép giờ, phòng lúc đặt. Provider không kiểm lịch hẹn (để Provider không phải gọi ngược sang Scheduling) | CT04 | — |
| CS17 | Ngày nghỉ, ngày lễ, bác sĩ nghỉ đột xuất | Ngoài phạm vi. Ngừng ca dài hạn bằng `effective_to` hoặc `active = false` | CT04 | — |
| CS18 | Thông tin bác sĩ | Không có số chứng chỉ hành nghề, ảnh, giới thiệu | CT03 | — |

### 7.3. Patient Service (P1)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi |
|---|---|---|---|---|
| CS19 | Người giám hộ | Dưới 14 tuổi (tính theo ngày sinh, tại ngày tạo / sửa) bắt buộc `guardianName`, `guardianPhone` | CT07 | `patient.guardian-required-under-age: 14` · 400 `GUARDIAN_REQUIRED` |
| CS20 | Chuẩn hóa số điện thoại | Bỏ dấu cách, dấu chấm, dấu gạch; `+84…` hoặc `84…` đổi thành `0…`. Làm trước khi lưu và trước khi so trùng | CT07 | — |
| CS21 | Số hồ sơ một tài khoản quản lý | Không giới hạn | CT07 | — |
| CS22 | Hồ sơ tạm có hạn bổ sung giấy tờ không | Không hết hạn | CT07, CT09 | — |
| CS23 | Gộp hồ sơ: thông tin liên hệ | Giữ nguyên thông tin của hồ sơ giữ lại, không chép gì từ hồ sơ bị gộp | CT21 | — |
| CS24 | Tách hồ sơ: trạng thái cặp trong hàng chờ | Cặp chuyển sang `NOT_DUPLICATE` (đã xác nhận là hai người khác nhau), `review_note` = ghi chú lần tách | CT23, CT20 | — |
| CS25 | Ai gộp / tách | Chỉ lễ tân; quản lý **không** gộp, tách | CT20, CT21, CT23 | 403 `FORBIDDEN` |
| CS26 | Bác sĩ xem hồ sơ | Bác sĩ đọc được mọi hồ sơ (giữ như `03-hop-dong-api.md`) | CT07 | — |
| CS27 | Giấy tờ trên một hồ sơ | Một cặp loại + số; chỉ kiểm định dạng CCCD (12 số). Không lưu ngày cấp, nơi cấp, bản chụp | CT08 | 400 `INVALID_ID_NUMBER` |

### 7.4. Reminder Service — dịch vụ thông báo (P5)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi |
|---|---|---|---|---|
| CS28 | "Trong 24 giờ tới" | Lịch `CONFIRMED` có `0 < start_time − now ≤ 24 giờ`; quét mỗi 5 phút | CT15 | `reminder.hours-before: 24`, `reminder.cron` |
| CS29 | Đặt sát giờ (dưới 24 giờ) | Nhắc ở lần quét kế tiếp | CT15 | — |
| CS30 | Hồ sơ không có email | Mọi loại thư ghi `SKIPPED`, không có kênh khác (không SMS) | CT14, CT15, CT24 | — |
| CS31 | Thư theo sự kiện | Lịch chuyển `CONFIRMED` → gửi email xác nhận (CT14). Lịch chuyển `CANCELLED`, ai hủy cũng vậy → gửi thư báo hủy kèm lý do (CT24). Lịch `EXPIRED`, `COMPLETED`, `NO_SHOW` không gửi thư | CT14, CT24 | Scheduling phát sự kiện `appointment.confirmed`, `appointment.cancelled` (`03-hop-dong-api.md` A7) |
| CS32 | Nội dung thư | Mỗi loại thư (nhắc, xác nhận, báo hủy) một mẫu trong bảng `message_templates`, Quản lý sửa trên trang *Mẫu thư*. Chỗ điền: tên bệnh nhân, ngày giờ khám, mã lịch hẹn, lý do hủy. Mẫu nạp sẵn có lời dặn "Vui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân". Tên bác sĩ, phòng: việc nâng cao | CT25 | `message_templates` |
| CS35 | Đặt lịch dưới 24 giờ trước giờ khám | Nhận cả thư xác nhận lẫn thư nhắc (hai loại thư khác nhau, không gộp) | CT14, CT15 | — |
| CS36 | Sự kiện nhận lặp, Reminder đang tắt | Bỏ qua sự kiện có `eventId` đã xử lý; mỗi lịch tối đa một thư cho mỗi loại. Reminder tắt thì sự kiện chờ trong hàng đợi, bật lại gửi tiếp. Thư gửi lỗi (`FAILED`) được thử lại ở lần quét định kỳ kế tiếp, mọi loại thư | CT14, CT15, CT24 | `processed_events` trong `reminder_db`; khóa duy nhất (`appointment_id`, `reminder_type`) |

### 7.5. Auth Service (P6) — đã làm sẵn, ghi để tra

| Mã | Chính sách | Giá trị chốt | Áp vào |
|---|---|---|---|
| CS33 | Thứ tự tạo tài khoản bác sĩ | Tạo bác sĩ (CT03) trước, rồi mới tạo tài khoản (CT05). Auth không gọi Provider để kiểm `doctorId`; trang *Tài khoản* cho chọn bác sĩ từ danh sách | CT05 |
| CS34 | Quản lý tài khoản nhân viên | Quản lý tạo tài khoản kèm mật khẩu ban đầu; khóa / mở tài khoản. Đổi vai trò, đặt lại mật khẩu: làm trên màn hình quản trị của Keycloak, không có API riêng | CT05 |

---

## 8. Ngoài phạm vi

- Thanh toán, bảo hiểm y tế; hồ sơ bệnh án, đơn thuốc, kết quả xét nghiệm.
- Ứng dụng di động, SMS thật (email gửi vào hộp thư giả lập Mailpit).
- Triển khai lên máy chủ thật (chỉ chạy cục bộ bằng Docker Compose).
- So khớp hồ sơ gần đúng / học máy; trao đổi dữ liệu với bệnh viện khác theo chuẩn HL7 FHIR.
- Tự động dời lịch khi bác sĩ nghỉ đột xuất; danh sách chờ (waitlist).

- Tiếp đón tại quầy và số thứ tự (CT18, câu C) · khung giờ chứa nhiều người · xét nghiệm và lượt quay lại sau xét nghiệm · in phiếu hẹn · SMS · bác sĩ nghỉ đột xuất, ngày lễ · bệnh nhân đến trễ · phạt người không đến · số chứng chỉ hành nghề, ảnh bác sĩ · nhiều giấy tờ trên một hồ sơ.
