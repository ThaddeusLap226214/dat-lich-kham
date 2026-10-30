# 10. Bộ chứng từ và hợp đồng nghiệp vụ

Tài liệu này trả lời hai câu: hệ thống **phải lưu những dữ liệu gì** (bộ chứng từ) và **trường hợp X thì xử lý thế nào** (hợp đồng nghiệp vụ — các chính sách đã chốt). Đọc cùng [`11-module-va-quy-trinh.md`](11-module-va-quy-trinh.md): chứng từ là "giấy vào, giấy ra" của từng bước trong quy trình, chính sách là phần "theo quy tắc nào".

**Chốt ngày 30/10/2026.** Mọi giá trị ở mục 4 là giá trị dùng để code, không còn câu "chưa chốt". Muốn đổi một chính sách: sửa file này trong cùng PR, ghi lý do trong mô tả PR, các phần bị ảnh hưởng xem và đồng ý.

Nguồn: `docs/02`, `docs/03`, `docs/06`, `docs/07`, `frontend/src/app/core/app-pages.ts`, dữ liệu mẫu `*/src/main/resources/data.sql`.

## Mục lục

1. [Cách đọc](#1-cách-đọc)
2. [Danh mục chứng từ (mức 1)](#2-danh-mục-chứng-từ-mức-1)
3. [Trường dữ liệu (mức 2)](#3-trường-dữ-liệu-mức-2)
4. [Hợp đồng nghiệp vụ — chính sách đã chốt](#4-hợp-đồng-nghiệp-vụ--chính-sách-đã-chốt)
5. [Thay đổi `docs/03`, `docs/06` kéo theo](#5-thay-đổi-docs03-docs06-kéo-theo)
6. [Kiểm hai chiều: bảng ↔ chứng từ](#6-kiểm-hai-chiều-bảng--chứng-từ)
7. [Kịch bản điền số (mức 3)](#7-kịch-bản-điền-số-mức-3)

---

## 1. Cách đọc

- **Chứng từ** là giấy tờ hoặc thông tin ghi lại một việc đã xảy ra, dùng để lập, chuyển giao và kiểm tra công việc. Ở một phòng khám chưa có hệ thống, đó là sổ hẹn, phiếu khai, cuộc gọi nhắc lịch, bảng phân ca dán tường. Cột "Ngoài đời" ở mục 2 ghi dạng đó để thấy hệ thống thay thế cái gì.
- **Loại:**

  | Loại | Nghĩa | Ví dụ |
  |---|---|---|
  | GD — giao dịch | Ghi một lần một việc xảy ra | phiếu giữ chỗ, phiếu hủy |
  | DM — danh mục | Dùng đi dùng lại, ít đổi | chuyên khoa, bác sĩ, ca làm việc |
  | Sổ | Máy tự ghi dồn nhiều lần | nhật ký thông báo, bản ghi chuyển lịch |
  | BC — báo cáo | Tính ra để xem, không lưu | bảng slot trống, danh sách khám trong ngày |
  | Đính kèm | Do bên ngoài phát hành, chỉ lấy vài trường | CCCD, thẻ BHYT |

- **Hướng tin học hóa:** *Thay* (lập thẳng trên hệ thống) · *Tự sinh* (máy tính ra, không ai phải lập) · *Lấy trường* (chỉ chép vài trường từ giấy bên ngoài) · *Ngoài phạm vi*.
- **Chính sách** không phải chứng từ: chính sách không ghi một việc, mà chi phối nhiều việc. Chính sách nằm ở mục 4.

---

## 2. Danh mục chứng từ (mức 1)

Mỗi chứng từ một dòng: ai lập, lập từ chứng từ nào, chuyển cho ai, ai kiểm (hoặc hệ thống tự kiểm gì), lưu ở đâu.

### Nhóm A — Chuẩn bị của phòng khám (Provider, Auth)

| Mã | Chứng từ | Loại | Ngoài đời | Ai lập | Từ chứng từ | Chuyển cho | Ai kiểm, kiểm gì | Lưu ở | Hướng |
|---|---|---|---|---|---|---|---|---|---|
| CT01 | Danh mục chuyên khoa | DM | bảng niêm yết ở sảnh | Quản lý | — | mọi người (trang *Chuyên khoa & bác sĩ*) | hệ thống: mã không trùng | Provider `specialties` | Thay |
| CT02 | Danh mục phòng khám | DM | biển phòng, sơ đồ tầng | Quản lý | — | CT04 | hệ thống: mã không trùng | Provider `rooms` | Thay |
| CT03 | Danh mục bác sĩ | DM | bảng giới thiệu bác sĩ | Quản lý | CT01 | khách xem; CT04; CT05 | hệ thống: chuyên khoa tồn tại | Provider `doctors` | Thay |
| CT04 | Lịch phân công bác sĩ (ca lặp hằng tuần) | DM | bảng phân ca dán tường, file Excel | Quản lý | CT03, CT02 | CT10 | hệ thống: không chồng giờ với ca khác của bác sĩ trong cùng ngày | Provider `work_schedules` | Thay |
| CT05 | Phiếu cấp tài khoản nhân viên (lễ tân, bác sĩ, quản lý) | GD | quản lý nhờ người phụ trách máy tạo tài khoản | Quản lý | CT03 (với bác sĩ) | Keycloak; nhân viên nhận tên đăng nhập, mật khẩu ban đầu | hệ thống: tên đăng nhập không trùng, tài khoản bác sĩ phải có mã bác sĩ | Auth `accounts` + Keycloak | Thay |

### Nhóm B — Bệnh nhân (Auth, Patient)

| Mã | Chứng từ | Loại | Ngoài đời | Ai lập | Từ chứng từ | Chuyển cho | Ai kiểm, kiểm gì | Lưu ở | Hướng |
|---|---|---|---|---|---|---|---|---|---|
| CT06 | Phiếu đăng ký tài khoản | GD | không có (khách gọi điện) | Khách | — | Keycloak (để đăng nhập) | hệ thống: tên đăng nhập, email không trùng | Auth `accounts` + Keycloak | Thay (bước mới) |
| CT07 | Phiếu thông tin bệnh nhân (hồ sơ) | GD, sau đó dùng như DM | phiếu khai giấy tại quầy | Bệnh nhân hoặc Lễ tân | CT08 (nếu có) | CT11; CT15 (lấy email) | hệ thống: dò trùng theo bảng tình huống, CCCD đủ 12 số, dưới 14 tuổi phải có người giám hộ | Patient `patients` | Thay |
| CT08 | Giấy tờ tùy thân / thẻ BHYT | Đính kèm | bản gốc khách mang theo | Cơ quan nhà nước | — | CT07, CT09 | Lễ tân đối chiếu bản gốc | chỉ lưu loại + số trong `patients` | Lấy trường |
| CT09 | Phiếu bổ sung giấy tờ cho hồ sơ tạm | GD | lễ tân ghi thêm vào phiếu khai | Lễ tân | CT07 (hồ sơ tạm), CT08 | Patient; nếu nghi trùng → CT20 | hệ thống: hồ sơ phải đang `TEMPORARY`, dò trùng giấy tờ | Patient `patients` | Thay |

### Nhóm C — Hẹn khám (Scheduling)

| Mã | Chứng từ | Loại | Ngoài đời | Ai lập | Từ chứng từ | Chuyển cho | Ai kiểm, kiểm gì | Lưu ở | Hướng |
|---|---|---|---|---|---|---|---|---|---|
| CT10 | Bảng slot trống | BC | lễ tân lật sổ hẹn xem còn giờ nào | Hệ thống | CT04 + các lịch CT11, CT12 | bệnh nhân, lễ tân | — | không lưu, tính mỗi lần gọi | Tự sinh |
| CT11 | Phiếu giữ chỗ | GD | không có ("em giữ giúp chị giờ đó nhé") | Hệ thống, theo yêu cầu của bệnh nhân / lễ tân | CT10, CT07 | người đặt; CT12 | hệ thống: slot thuộc ca, chưa ai giữ, hồ sơ không trùng giờ, các giới hạn CS03–CS06 | Scheduling `appointments` (`HELD`) | Thay (bước mới) |
| CT12 | Phiếu hẹn khám | GD | dòng trong sổ hẹn, giấy hẹn | Hệ thống | CT11 | người đặt (trang *Lịch khám của tôi*); CT14; CT15; CT16 | hệ thống: chưa quá hạn giữ chỗ | Scheduling `appointments` (`CONFIRMED`) | Thay |
| CT13 | Phiếu hủy lịch | GD | gọi điện báo hủy, gạch sổ | Người đặt hoặc Lễ tân | CT12 | Scheduling (nhả slot); CT24 | hệ thống: hạn hủy CS07, quyền CS08 | Scheduling `appointments` (`CANCELLED`) | Thay |

### Nhóm D — Thông báo (Reminder Service — dịch vụ thông báo)

| Mã | Chứng từ | Loại | Ngoài đời | Ai lập | Từ chứng từ | Chuyển cho | Ai kiểm, kiểm gì | Lưu ở | Hướng |
|---|---|---|---|---|---|---|---|---|---|
| CT14 | Email xác nhận đặt lịch | GD | lễ tân đọc lại giờ hẹn qua điện thoại | Hệ thống (Reminder), khi nhận sự kiện "đã xác nhận" | CT12 + email ở CT07 + mẫu CT25 | email của hồ sơ | hệ thống: mỗi lịch một thư xác nhận | Reminder `reminder_logs` (`CONFIRMATION`) | Thay (máy làm thay) |
| CT15 | Email nhắc lịch | GD | lễ tân gọi điện nhắc hôm trước | Hệ thống (Reminder), quét định kỳ | CT12 + email ở CT07 + mẫu CT25 | email của hồ sơ | hệ thống: mỗi lịch chỉ nhắc một lần | Reminder `reminder_logs` (`BEFORE_APPOINTMENT`) | Thay (máy làm thay) |
| CT24 | Email báo hủy lịch | GD | lễ tân gọi lại báo đã hủy | Hệ thống (Reminder), khi nhận sự kiện "đã hủy" | CT13 + email ở CT07 + mẫu CT25 | email của hồ sơ | hệ thống: mỗi lịch một thư báo hủy | Reminder `reminder_logs` (`CANCELLATION`) | Thay (bước mới) |
| CT25 | Mẫu thư thông báo (nhắc, xác nhận, báo hủy) | DM | không có (mỗi người nói một kiểu) | Quản lý | — | CT14, CT15, CT24 | hệ thống: tiêu đề, nội dung không trống | Reminder `message_templates` | Thay (bước mới) |

Nhật ký của cả ba loại thư nằm chung bảng `reminder_logs` (sổ: máy tự ghi dồn), Quản lý và Lễ tân xem ở trang *Nhật ký thông báo*.

### Nhóm E — Ngày khám (Gateway ghép, Scheduling)

| Mã | Chứng từ | Loại | Ngoài đời | Ai lập | Từ chứng từ | Chuyển cho | Ai kiểm, kiểm gì | Lưu ở | Hướng |
|---|---|---|---|---|---|---|---|---|---|
| CT16 | Danh sách khám trong ngày | BC | bản in danh sách từ sổ hẹn | Hệ thống (Gateway ghép) | CT12 + CT03, CT02, CT07 | Bác sĩ, Lễ tân | — | không lưu | Tự sinh |
| CT17 | Xác nhận đã khám | GD | bác sĩ đánh dấu vào danh sách | Bác sĩ hoặc Lễ tân | CT16 | Scheduling | hệ thống: chỉ lịch `CONFIRMED`, từ giờ bắt đầu (CS09), bác sĩ chỉ đánh dấu lịch của mình | Scheduling `appointments` (`COMPLETED`) | Thay |
| CT18 | Phiếu tiếp đón tại quầy (số thứ tự) | — | phát số thứ tự | — | — | — | — | — | Ngoài phạm vi |
| CT19 | Ghi nhận bệnh nhân không đến | GD | gạch tên trong danh sách | Bác sĩ hoặc Lễ tân | CT16 | Scheduling | hệ thống: chỉ lịch `CONFIRMED`, sau giờ bắt đầu (CS10) | Scheduling `appointments` (`NO_SHOW`) | Thay (**đưa vào 30/10**) |

### Nhóm F — Hồ sơ trùng (Patient, Scheduling)

| Mã | Chứng từ | Loại | Ngoài đời | Ai lập | Từ chứng từ | Chuyển cho | Ai kiểm, kiểm gì | Lưu ở | Hướng |
|---|---|---|---|---|---|---|---|---|---|
| CT20 | Đề nghị gộp hồ sơ (hàng chờ) và quyết định duyệt | GD | lễ tân tình cờ thấy hai phiếu giống nhau | Hệ thống đề nghị; Lễ tân quyết | CT07, CT09 | Lễ tân → CT21 hoặc "không trùng" | Lễ tân so sánh hai hồ sơ; **hệ thống không bao giờ tự gộp** | Patient `duplicate_candidates` | Thay |
| CT21 | Biên bản gộp hồ sơ | GD | — | Lễ tân | CT20 | Scheduling qua sự kiện `patient.merged` → CT22 | hệ thống: hồ sơ giữ lại phải thuộc cặp | Patient `merge_history` (`MERGE`) | Thay |
| CT22 | Bản ghi chuyển lịch hẹn | Sổ | — | Hệ thống (Scheduling) | CT21, CT23 | — | hệ thống: mỗi sự kiện xử lý đúng một lần | Scheduling `patient_reassignments` | Tự sinh |
| CT23 | Biên bản tách hồ sơ | GD | — | Lễ tân | CT21 | Scheduling qua sự kiện `patient.unmerged` → CT22 | hệ thống: hồ sơ phải đang `MERGED` | Patient `merge_history` (`UNMERGE`) | Thay |

**Tổng:** 25 chứng từ — 24 có chỗ lưu hoặc tự sinh, 1 ngoài phạm vi (CT18). Phần in phiếu hẹn của CT12 cũng ngoài phạm vi. CT24, CT25 thêm ngày 30/10 khi Reminder Service thành dịch vụ thông báo.

### Kiểm chuỗi: mỗi chứng từ có trước, có sau

```
CT01 → CT03 → CT04 → CT10 → CT11 → CT12 → CT14 (thư xác nhận) ; CT15 (thư nhắc)
CT02 ────────↗                       ├──→ CT16 → CT17 (đã khám) | CT19 (không đến)
                                     └──→ CT13 (hủy) → CT24 (thư báo hủy)
CT25 (mẫu thư) → CT14, CT15, CT24
CT06 (tài khoản) → CT07 (hồ sơ) → CT11
CT08 → CT07 / CT09 → CT20 → CT21 → CT22 ; CT21 → CT23 → CT22
CT03 → CT05 (tài khoản bác sĩ)
```

Đứng đầu chuỗi, không có chứng từ trước: CT01, CT02, CT25 (phòng khám tự lập), CT06 (khách tự đăng ký), CT08 (bên ngoài phát hành).

---

## 3. Trường dữ liệu (mức 2)

**Trường chung** của mọi chứng từ giao dịch: mã (`id`, tự tăng) · lúc tạo, lúc sửa (`created_at`, `updated_at`, tự điền) · người lập (`..._by_account_id`, lấy từ header `X-User-Id`) · trạng thái (`status`).

✔ = bắt buộc. "Lấy từ" bỏ trống nghĩa là người dùng nhập.

| Mã | Trường → cột | Lấy từ |
|---|---|---|
| CT01 | mã `code` ✔ duy nhất · tên `name` ✔ duy nhất · mô tả `description` · còn hoạt động `active` ✔ | |
| CT02 | mã `code` ✔ duy nhất · tên `name` ✔ · vị trí `location` · `active` ✔ | |
| CT03 | họ tên `full_name` ✔ · học vị `title` · chuyên khoa `specialty_id` ✔ · `phone`, `email` · còn nhận lịch `active` ✔ | `specialty_id` ← CT01 |
| CT04 | bác sĩ `doctor_id` ✔ · phòng `room_id` ✔ · thứ `day_of_week` ✔ (1 = Thứ Hai … 7 = Chủ nhật) · `start_time`, `end_time` ✔ · thời lượng một lượt `slot_minutes` ✔ (5–120) · áp dụng từ `effective_from` ✔ · đến `effective_to` · `active` ✔ | `doctor_id` ← CT03, `room_id` ← CT02 |
| CT05 | `username` ✔ · `full_name` ✔ · `email` · `phone` · `role` ✔ · `doctor_id` (✔ khi `DOCTOR`) · `enabled` ✔ · liên kết người dùng Keycloak · mật khẩu ban đầu (chỉ gửi sang Keycloak, **không lưu** ở `accounts`) | `doctor_id` ← CT03 |
| CT06 | `username` ✔ · mật khẩu ✔ (chỉ gửi Keycloak) · `full_name` ✔ · `email` · `phone` | `role` = `PATIENT`, tự điền |
| CT07 | họ tên `full_name` ✔ · ngày sinh `date_of_birth` ✔ · giới tính `gender` ✔ · `phone`, `email`, `address` · `id_type`, `id_number` · giám hộ `guardian_name`, `guardian_phone` (✔ khi dưới 14 tuổi, CS19), `guardian_relation` | `normalized_name` tự tính · `owner_account_id` = tài khoản đang đăng nhập nếu là `PATIENT`, để trống nếu lễ tân tạo · `created_by_account_id` ← `X-User-Id` · `status` tự tính theo bảng tình huống (`docs/06` B3) |
| CT08 | loại `id_type` (`CCCD`, `PASSPORT`, `BHYT`) · số `id_number` (CCCD đủ 12 số) | chép từ giấy, chỉ hai trường này |
| CT09 | `id_type` ✔ · `id_number` ✔ | kết quả `status` tự tính · `updated_at` |
| CT10 | `startTime`, `endTime`, `roomId`, `workScheduleId`, `available` | tính từ CT04 + lịch đang giữ / đã đặt |
| CT11 | hồ sơ `patient_id` ✔ · bác sĩ `doctor_id` ✔ · giờ bắt đầu `start_time` ✔ · lý do khám `reason` | `end_time` = `start_time` + `slot_minutes` · `room_id`, `work_schedule_id` chép từ ca (bản chụp, không hỏi lại Provider) · `booked_by_account_id` ← `X-User-Id` · `status` = `HELD` · `hold_expires_at` = lúc giữ + 5 phút |
| CT12 | các trường của CT11 | `status` = `CONFIRMED` · `confirmed_at` |
| CT13 | lý do hủy `cancel_reason` | `cancelled_by_account_id` ← `X-User-Id` · `cancelled_at` · `status` = `CANCELLED` |
| CT14 | — (máy lập) | như CT15, `reminder_type` = `CONFIRMATION`; nội dung điền mẫu CT25 bằng dữ liệu của sự kiện `appointment.confirmed` |
| CT15 | — (máy lập) | `appointment_id`, `patient_id`, `appointment_start` (chép từ CT12) · `recipient_email` ← CT07 · `reminder_type` = `BEFORE_APPOINTMENT` · `status` · `attempt_count`, `last_error`, `sent_at`; nội dung điền mẫu CT25 |
| CT24 | — (máy lập) | như CT15, `reminder_type` = `CANCELLATION`; lý do hủy lấy từ sự kiện `appointment.cancelled` |
| CT25 | loại thư `type` ✔ (cố định 3 loại, mỗi loại một mẫu) · tiêu đề `subject` ✔ · nội dung `body` ✔ (chỗ điền `{patientName}`, `{startTime}`, `{appointmentId}`, `{cancelReason}`) | `updated_by_account_id` ← `X-User-Id` · `updated_at` |
| CT16 | — (máy lập) | giờ, trạng thái, lý do ← CT12 · bác sĩ ← CT03 · phòng ← CT02 · bệnh nhân (tên, ngày sinh) ← CT07 |
| CT17 | — | `status` = `COMPLETED` · `completed_at` |
| CT19 | — | `status` = `NO_SHOW` · `no_show_at` (**cột mới**) |
| CT20 | — (máy đề nghị) · quyết định: ghi chú `review_note` | `patient_id`, `candidate_patient_id`, `reason`, `score` (chỉ tham khảo) · `status` · `reviewed_by_account_id`, `reviewed_at` |
| CT21 | hồ sơ giữ lại `keepPatientId` ✔ · ghi chú `note` | `action` = `MERGE` · `source_patient_id`, `target_patient_id`, `source_previous_status`, `duplicate_candidate_id` · `event_id` · `performed_by_account_id`, `performed_at` |
| CT22 | — | `appointment_id`, `from_patient_id`, `to_patient_id`, `merge_event_id`, `reassigned_at`, `reverted_at` |
| CT23 | ghi chú `note` | `action` = `UNMERGE` · `related_merge_id` · `event_id` · `performed_by_account_id`, `performed_at` |

---

## 4. Hợp đồng nghiệp vụ — chính sách đã chốt

Chỉ ghi điều thuộc **nghiệp vụ ngoài đời, không hiển nhiên**. Việc hiển nhiên (kiểm định dạng email, không để trống tên…) người code tự quyết.

Hai nguyên tắc khi code:
1. **Con số đặt trong `application.yml`**, không viết cứng trong Java. Đổi chính sách thì chỉ sửa cấu hình.
2. **Giao dịch lưu giá trị lúc phát sinh** (bản chụp): lịch hẹn chép giờ, phòng, ca lúc giữ chỗ; sau này đổi ca không làm sai lịch cũ.

Mã lỗi theo dạng `ApiError` của `docs/06` A4. Cột "Từ" là mã câu hỏi cũ, để tra lại.

### 4.1. Scheduling Service (P2)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi | Từ |
|---|---|---|---|---|---|
| CS01 | Thời gian giữ chỗ | 5 phút | CT11 | `scheduling.hold-minutes: 5` (đã có) | S1 |
| CS02 | Xác nhận đúng lúc hết hạn | Quá hạn khi `now > hold_expires_at`; đúng mốc vẫn xác nhận được | CT12 | 409 `HOLD_EXPIRED` (đã có) | K4 |
| CS03 | Số chỗ đang giữ của một tài khoản | Tối đa 3 lịch `HELD` cùng lúc | CT11 | `scheduling.max-holds-per-account: 3` · 409 `TOO_MANY_HOLDS` | S3 |
| CS04 | Số lịch sắp tới của một hồ sơ | Tối đa 5 lịch `HELD` + `CONFIRMED` chưa tới giờ; kiểm lúc giữ chỗ | CT11 | `scheduling.max-upcoming-per-patient: 5` · 409 `TOO_MANY_APPOINTMENTS` | S4 |
| CS05 | Đặt trước tối đa | 30 ngày tính từ hôm nay | CT10, CT11 | `scheduling.book-ahead-days: 30` · `GET /slots` ngày ngoài khoảng trả mọi slot `available = false` · giữ chỗ trả 400 `TOO_FAR_IN_ADVANCE` | K1 |
| CS06 | Đặt sát giờ | Bệnh nhân không đặt được khi còn dưới 2 giờ tới giờ khám; lễ tân đặt được tới giờ bắt đầu (khách có mặt tại quầy) | CT10, CT11 | `scheduling.book-before-hours: 2` · `GET /slots` với vai trò `PATIENT` trả `available = false` cho các slot đó · giữ chỗ trả 400 `TOO_LATE_TO_BOOK` | K2 |
| CS07 | Hạn hủy | Bệnh nhân hủy được khi còn **từ 2 giờ trở lên** (đúng 2 giờ vẫn được); lễ tân hủy được tới giờ bắt đầu, không bị giới hạn 2 giờ | CT13 | `scheduling.cancel-before-hours: 2` (đã có) · 409 `TOO_LATE_TO_CANCEL` | S2, K3 |
| CS08 | Ai được hủy | Người đã đặt và lễ tân; **bác sĩ không hủy** | CT13 | 403 `FORBIDDEN` | A1 |
| CS09 | Đánh dấu đã khám | Chỉ từ giờ bắt đầu của lịch trở đi | CT17 | 409 `TOO_EARLY_TO_COMPLETE` | K5 |
| CS10 | Bệnh nhân không đến | Có trạng thái `NO_SHOW`. Bác sĩ (lịch của mình) hoặc lễ tân ghi; chỉ lịch `CONFIRMED`, chỉ sau giờ bắt đầu. Không có cron tự chuyển. Người không đến không bị phạt | CT19 | API mới `POST /appointments/{id}/no-show` · 409 `TOO_EARLY_FOR_NO_SHOW`, 409 `INVALID_STATUS` | A (N1) |
| CS11 | Ca không chia hết thời lượng slot | Bỏ phần lẻ cuối ca | CT10 | — | T1 |
| CS12 | Hai lịch liền kề của một hồ sơ (08:10–08:30 và 08:30–08:50) | **Không** tính là trùng giờ (khoảng nửa mở `[bắt đầu, kết thúc)`) | CT11 | — | T3 |
| CS13 | Đổi lịch | Không có API riêng: hủy lịch cũ rồi đặt lịch mới | CT13, CT11 | — | B (N2) |
| CS14 | Lịch hẹn khi gộp hồ sơ | Chuyển **mọi** lịch của hồ sơ bị gộp (mọi trạng thái) sang hồ sơ giữ lại; khi tách, trả lại đúng các lịch đã chuyển | CT22 | — | N9 |
| CS15 | "Lịch khám của tôi" gồm những lịch nào | Lịch do tài khoản đặt **hoặc** lịch của hồ sơ mà tài khoản đang quản lý (`owner_account_id`), kể cả lịch lễ tân đặt hộ | CT12 | API nội bộ thêm tham số `patientIds` (mục 5); Gateway ghép ở `my-appointments` | A2 |

### 4.2. Provider Service (P3)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi | Từ |
|---|---|---|---|---|---|
| CS16 | Sửa hoặc ngừng ca khi đã có lịch hẹn | Cho sửa / ngừng. Thay đổi chỉ ảnh hưởng slot tính từ lúc đó; lịch đã đặt **giữ nguyên** vì đã chép giờ, phòng lúc đặt. Provider không kiểm lịch hẹn (để Provider không phải gọi ngược sang Scheduling) | CT04 | — | K8 |
| CS17 | Ngày nghỉ, ngày lễ, bác sĩ nghỉ đột xuất | Ngoài phạm vi. Ngừng ca dài hạn bằng `effective_to` hoặc `active = false` | CT04 | — | N5, G05 |
| CS18 | Thông tin bác sĩ | Không có số chứng chỉ hành nghề, ảnh, giới thiệu | CT03 | — | G18 |

### 4.3. Patient Service (P1)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi | Từ |
|---|---|---|---|---|---|
| CS19 | Người giám hộ | Dưới 14 tuổi (tính theo ngày sinh, tại ngày tạo / sửa) bắt buộc `guardianName`, `guardianPhone` | CT07 | `patient.guardian-required-under-age: 14` · 400 `GUARDIAN_REQUIRED` | T5 |
| CS20 | Chuẩn hóa số điện thoại | Bỏ dấu cách, dấu chấm, dấu gạch; `+84…` hoặc `84…` đổi thành `0…`. Làm trước khi lưu và trước khi so trùng | CT07 | — | T4 |
| CS21 | Số hồ sơ một tài khoản quản lý | Không giới hạn | CT07 | — | S5 |
| CS22 | Hồ sơ tạm có hạn bổ sung giấy tờ không | Không hết hạn | CT07, CT09 | — | K6 |
| CS23 | Gộp hồ sơ: thông tin liên hệ | Giữ nguyên thông tin của hồ sơ giữ lại, không chép gì từ hồ sơ bị gộp | CT21 | — | T6 |
| CS24 | Tách hồ sơ: trạng thái cặp trong hàng chờ | Cặp chuyển sang `NOT_DUPLICATE` (đã xác nhận là hai người khác nhau), `review_note` = ghi chú lần tách | CT23, CT20 | — | G13 |
| CS25 | Ai gộp / tách | Chỉ lễ tân; quản lý **không** gộp, tách | CT20, CT21, CT23 | 403 `FORBIDDEN` | A4 |
| CS26 | Bác sĩ xem hồ sơ | Bác sĩ đọc được mọi hồ sơ (giữ như `docs/06`) | CT07 | — | A3 |
| CS27 | Giấy tờ trên một hồ sơ | Một cặp loại + số; chỉ kiểm định dạng CCCD (12 số). Không lưu ngày cấp, nơi cấp, bản chụp | CT08 | 400 `INVALID_ID_NUMBER` (đã có) | G11 |

### 4.4. Reminder Service — dịch vụ thông báo (P5)

| Mã | Chính sách | Giá trị chốt | Áp vào | Cấu hình / mã lỗi | Từ |
|---|---|---|---|---|---|
| CS28 | "Trong 24 giờ tới" | Lịch `CONFIRMED` có `0 < start_time − now ≤ 24 giờ`; quét mỗi 5 phút | CT15 | `reminder.hours-before: 24`, `reminder.cron` (đã có) | T2 |
| CS29 | Đặt sát giờ (dưới 24 giờ) | Nhắc ở lần quét kế tiếp | CT15 | — | K7 |
| CS30 | Hồ sơ không có email | Mọi loại thư ghi `SKIPPED`, không có kênh khác (không SMS) | CT14, CT15, CT24 | — | N8 |
| CS31 | Thư theo sự kiện | Lịch chuyển `CONFIRMED` → gửi email xác nhận (CT14). Lịch chuyển `CANCELLED`, ai hủy cũng vậy → gửi thư báo hủy kèm lý do (CT24). Lịch `EXPIRED`, `COMPLETED`, `NO_SHOW` không gửi thư | CT14, CT24 | Scheduling phát sự kiện `appointment.confirmed`, `appointment.cancelled` (mục 5) | N6, D (N4) — **đổi 30/10** |
| CS32 | Nội dung thư | Mỗi loại thư (nhắc, xác nhận, báo hủy) một mẫu trong bảng `message_templates`, Quản lý sửa trên trang *Mẫu thư*. Chỗ điền: tên bệnh nhân, ngày giờ khám, mã lịch hẹn, lý do hủy. Mẫu nạp sẵn có lời dặn "Vui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân". Tên bác sĩ, phòng: việc nâng cao | CT25 | `message_templates` | G17 — **đổi 30/10** |
| CS35 | Đặt lịch dưới 24 giờ trước giờ khám | Nhận cả thư xác nhận lẫn thư nhắc (hai loại thư khác nhau, không gộp) | CT14, CT15 | — | mới |
| CS36 | Sự kiện nhận lặp, Reminder đang tắt | Bỏ qua sự kiện có `eventId` đã xử lý; mỗi lịch tối đa một thư cho mỗi loại. Reminder tắt thì sự kiện chờ trong hàng đợi, bật lại gửi tiếp. Thư gửi lỗi (`FAILED`) được thử lại ở lần quét định kỳ kế tiếp, mọi loại thư | CT14, CT15, CT24 | `processed_events` trong `reminder_db`; khóa duy nhất (`appointment_id`, `reminder_type`) | mới |

### 4.5. Auth Service (P6) — đã làm sẵn, ghi để tra

| Mã | Chính sách | Giá trị chốt | Áp vào | Từ |
|---|---|---|---|---|
| CS33 | Thứ tự tạo tài khoản bác sĩ | Tạo bác sĩ (CT03) trước, rồi mới tạo tài khoản (CT05). Auth không gọi Provider để kiểm `doctorId`; trang *Tài khoản* cho chọn bác sĩ từ danh sách | CT05 | G08 |
| CS34 | Quản lý tài khoản nhân viên | Quản lý tạo tài khoản kèm mật khẩu ban đầu; khóa / mở tài khoản. Đổi vai trò, đặt lại mật khẩu: làm trên màn hình quản trị của Keycloak, không có API riêng | CT05 | G07 |

### 4.6. Ngoài phạm vi (đã chốt không làm)

Tiếp đón tại quầy và số thứ tự (CT18, câu C) · in phiếu hẹn · SMS · bác sĩ nghỉ đột xuất, ngày lễ · bệnh nhân đến trễ · phạt người không đến · số chứng chỉ hành nghề, ảnh bác sĩ · nhiều giấy tờ trên một hồ sơ.

---

## 5. Thay đổi `docs/03`, `docs/06` kéo theo

Các chính sách ở mục 4 làm đổi hợp đồng kỹ thuật như dưới đây. Phần tài liệu được sửa trước khi nhóm bắt đầu (trong PR gom tài liệu); phần code thuộc người làm service đó.

| # | Thay đổi | Sửa tài liệu | Code thuộc |
|---|---|---|---|
| 1 | `appointments.status` thêm `NO_SHOW`; thêm cột `no_show_at`; `slot_lock` tính cả `NO_SHOW` (như `COMPLETED`) | `docs/03` mục 3.4, 4.2 | P2 |
| 2 | API mới `POST /api/v1/appointments/{id}/no-show` — quyền: DOCTOR (lịch của mình), RECEPTIONIST | `docs/06` B4 | P2; nút "Không đến" ở trang *Lịch khám trong ngày* (P3) và *Lịch hẹn theo bác sĩ* (P2) |
| 3 | `AppointmentResponse` thêm `confirmedAt`, `cancelledAt`, `cancelledByAccountId`, `completedAt`, `noShowAt`; `AppointmentView` thêm `cancelReason` | `docs/06` B4, B6 | P2, P4 |
| 4 | API nội bộ `GET /internal/v1/appointments` thêm tham số `patientIds` (danh sách mã, phân tách bằng dấu phẩy) | `docs/06` B4 | P2 (Gateway dùng cho CS15) |
| 5 | Mã lỗi mới: `TOO_MANY_HOLDS`, `TOO_MANY_APPOINTMENTS`, `TOO_FAR_IN_ADVANCE`, `TOO_LATE_TO_BOOK`, `TOO_EARLY_TO_COMPLETE`, `TOO_EARLY_FOR_NO_SHOW` (Scheduling); `GUARDIAN_REQUIRED` (Patient) | `docs/06` A4, B3, B4 | P2, P1 |
| 6 | `cancel`: ghi rõ lễ tân không bị giới hạn 2 giờ; bác sĩ không được hủy | `docs/06` B4 | P2 |
| 7 | Sự kiện gộp: chuyển mọi lịch của hồ sơ bị gộp, không chỉ `HELD` / `CONFIRMED` | `docs/06` A7 | P2 |
| 8 | `unmerge`: cặp trong hàng chờ chuyển `NOT_DUPLICATE` | `docs/06` B3 | P1 |
| 9 | Cấu hình mới: `scheduling.max-holds-per-account`, `scheduling.max-upcoming-per-patient`, `scheduling.book-ahead-days`, `scheduling.book-before-hours`, `patient.guardian-required-under-age` | `application.yml` của từng service | P2, P1 |
| 10 | Sự kiện mới: exchange `appointment.events` (loại topic), routing key `appointment.confirmed`, `appointment.cancelled`; hàng đợi `reminder.appointment-events` + hàng đợi lỗi `reminder.appointment-events.dlq`; lớp `AppointmentEvent` {eventId, eventType (`APPOINTMENT_CONFIRMED` / `APPOINTMENT_CANCELLED`), occurredAt, appointmentId, patientId, doctorId, roomId, startTime, endTime, cancelReason, performedByAccountId}; Scheduling phát **sau khi** lưu thành công | `docs/06` A7, B4 | P2 (phát), P5 (nhận) |
| 11 | `reminder_db` thêm bảng `processed_events` (như bên Scheduling) và `message_templates` {`type` khóa chính, `subject`, `body`, `updated_by_account_id`, `updated_at`}, nạp sẵn 3 mẫu; `reminder_logs.reminder_type` có 3 giá trị `BEFORE_APPOINTMENT`, `CONFIRMATION`, `CANCELLATION` | `docs/03` mục 3.5, 5 | P5 |
| 12 | API mới `GET /api/v1/reminders/templates`, `PUT /api/v1/reminders/templates/{type}` — ADMIN **(GW)**; `GET /reminders/logs` lọc thêm theo `type` | `docs/06` B5 | P5 |
| 13 | Mô tả Reminder Service: "dịch vụ thông báo — nhắc lịch, xác nhận, báo hủy; nhận sự kiện từ Scheduling" | `docs/02`, sơ đồ tổng quan (thêm mũi tên Scheduling → RabbitMQ → Reminder) | — |

---

## 6. Kiểm hai chiều: bảng ↔ chứng từ

**Chiều 1 — mỗi chứng từ có chỗ lưu chưa:** mục 2, cột "Lưu ở". Không có bảng riêng: CT10, CT16 (tính mỗi lần), CT08 (chỉ lấy hai trường), CT18 (ngoài phạm vi).

**Chiều 2 — mỗi bảng ứng với chứng từ nào:**

| Database | Bảng | Ứng với |
|---|---|---|
| `auth_db` | `accounts` | CT05, CT06 |
| `provider_db` | `specialties` · `rooms` · `doctors` · `work_schedules` | CT01 · CT02 · CT03 · CT04 |
| `patient_db` | `patients` | CT07, CT08, CT09 |
| | `duplicate_candidates` | CT20 |
| | `merge_history` | CT21, CT23 |
| `scheduling_db` | `appointments` | CT11, CT12, CT13, CT17, CT19 |
| | `patient_reassignments` | CT22 |
| | `processed_events` | bảng kỹ thuật (chống xử lý sự kiện hai lần), không ứng chứng từ |
| `reminder_db` | `reminder_logs` | CT14, CT15, CT24 |
| | `message_templates` | CT25 |
| | `processed_events` | bảng kỹ thuật (chống xử lý sự kiện hai lần), không ứng chứng từ |
| `keycloak_db` | (bảng của Keycloak) | mật khẩu, vai trò của CT05, CT06 |

Cột kỹ thuật không ứng chứng từ: `version` (khóa lạc quan), `slot_lock`, `active_identity` (cột tự tính), `normalized_name`, `score` (chỉ tham khảo).

---

## 7. Kịch bản điền số (mức 3)

Bảy kịch bản đưa chứng từ chạy qua API và bảng, để kiểm chứng từ nối nhau đúng và số khớp nhau. Dùng làm kịch bản nghiệm thu và demo.

### Giả định chung

| Mục | Nội dung |
|---|---|
| Dữ liệu nền | Dữ liệu mẫu `docs/03` mục 5. Mỗi kịch bản chạy trên dữ liệu mẫu mới (sau `docker compose down -v` rồi `up`), nên mã mới tính từ cuối dữ liệu mẫu: tài khoản mới bắt đầu từ 12, hồ sơ từ 11, lịch hẹn từ 15, `reminder_logs` từ 6. |
| Mốc ngày | Dữ liệu mẫu tính theo ngày khởi tạo lần đầu. Giả định khởi tạo Chủ nhật 25/10/2026: thứ Hai tuần tới = 26/10, thứ Ba = 27/10; thứ Hai tuần trước = 19/10. Khi đó lịch mẫu 6, 7, 8, 9, 10, 12, 13 nằm ở 26/10; lịch 11 ở 27/10. Khởi tạo ngày khác thì cộng / trừ ngày tương ứng. |
| Lịch mẫu liên quan | Lịch 6: hồ sơ 1, bác sĩ 1, 26/10 08:10, `CONFIRMED` · 7: hồ sơ 2, bác sĩ 2, 26/10 07:45, `CONFIRMED` · 8: hồ sơ 4, bác sĩ 1, 26/10 08:30, `CONFIRMED` · 9: hồ sơ 6, bác sĩ 4, 26/10 08:30, `CONFIRMED` · 10: hồ sơ 7, bác sĩ 1, 26/10 13:30, `CONFIRMED` · 11: hồ sơ 8, bác sĩ 3, 27/10 13:30, `CONFIRMED` · 12: hồ sơ 5, bác sĩ 2, 26/10 08:00, `EXPIRED` · 13: hồ sơ 2, bác sĩ 2, 26/10 09:00, `CONFIRMED` (đặt bởi tài khoản 5). |
| Lịch làm việc | Bác sĩ 1 (lịch `work_schedules` 1–7): T2–T6 07:30–11:30, chiều T2 và T4 13:30–16:30, slot 20 phút, phòng 1 (P101) · Bác sĩ 2: T2–T7 07:30–11:30, slot 15 phút, phòng 2 (P102), ca thứ Hai là lịch 8 · Bác sĩ 6: chiều T2, T3, T5, T6 13:30–16:30, 20 phút, phòng 6 (P103), ca thứ Năm là lịch 24. Giờ hợp lệ của slot 20 phút bắt đầu ca 07:30 là 07:30, 07:50, 08:10, 08:30, 08:50, 09:10… |
| Tài khoản mẫu | 1 `admin` · 2 `letan` · 3 `bs.an` (bác sĩ 1) · 4 `bs.binh` (bác sĩ 2) · 5 `benhnhan` (hồ sơ 1, 2) · 10 `nam.tran` (hồ sơ 4, 5) · 11 `lan.pham` (hồ sơ 6) |
| Nhân vật thêm | Chị Ngô Thị Mai (sinh 1992) và con Ngô Gia Huy (sinh 2020), chỉ dùng ở KB2 để chạy khâu đăng ký. Bác sĩ Đặng Quốc Phong, nhân viên lễ tân thứ hai chỉ dùng ở KB1. Dữ liệu cá nhân của các nhân vật thêm (SĐT, số CCCD, email) là giá trị minh họa. |

### KB1 — Phòng khám chuẩn bị (thêm chuyên khoa Tim mạch)

Mục tiêu: chạy CT01–CT05. Thứ tự bắt buộc: có bác sĩ trước, rồi mới tạo tài khoản bác sĩ (cần `doctorId`).

| Bước | Ai làm | Thao tác | Chứng từ | Kết quả mong đợi |
|---|---|---|---|---|
| 1 | Quản lý (tài khoản 1) | `POST /specialties` {`code` `TIM`, `name` "Tim mạch"} | CT01 | 201; `specialties.id` = 6 |
| 2 | Quản lý | Thử lại `POST /specialties` với `code` `TIM` | CT01 | 409 `CODE_TAKEN`; vẫn 6 chuyên khoa mẫu + 1 |
| 3 | Quản lý | `POST /rooms` {`code` `P204`, `name` "Phòng khám Tim mạch", `location` "Tầng 2"} | CT02 | 201; `rooms.id` = 7 |
| 4 | Quản lý | `POST /doctors` {`fullName` "Đặng Quốc Phong", `title` "BS.CKI", `specialtyId` 6} | CT03 | 201; `doctors.id` = 7, `active` = true |
| 5 | Quản lý | `POST /doctors/7/work-schedules` {`roomId` 7, `dayOfWeek` 2 (Thứ Ba), 13:30–16:30, `slotMinutes` 20, `effectiveFrom` 2026-10-26} | CT04 | 201; `work_schedules.id` = 26 |
| 6 | Quản lý | `POST /doctors/7/work-schedules` ca Thứ Ba 16:00–18:00 | CT04 | 409 `SCHEDULE_OVERLAP` (chồng 16:00–16:30 với ca 26) |
| 7 | Quản lý | `POST /accounts` {`username` `bs.phong`, `fullName` "Đặng Quốc Phong", `role` `DOCTOR`, `doctorId` 7} | CT05 | 201; `accounts.id` = 12, `doctor_id` = 7; người dùng Keycloak có `accountId` = 12, `doctorId` = 7. Thiếu `doctorId` → 400 `DOCTOR_ID_REQUIRED` |
| 8 | Quản lý | `POST /accounts` {`username` `letan2`, `role` `RECEPTIONIST`} | CT05 | 201; `accounts.id` = 13, `doctor_id` NULL |
| 9 | Khách | Mở trang `doctors`, chọn chuyên khoa Tim mạch | CT01, CT03 | Thấy bác sĩ 7; `GET /doctors/7/work-schedules` trả ca 26 |
| 10 | Khách / Bệnh nhân | `GET /slots?doctorId=7&date=2026-10-27` | CT10 | 9 slot từ 13:30 đến 16:10, phòng 7, `available` = true |

Ghi chú: phải làm bước 4 trước bước 7; Auth không kiểm `doctorId` tồn tại (CS33). Bước 7, 8 gửi kèm mật khẩu ban đầu (CS34).

### KB2 — Mẹ đặt lịch online cho con, đến khi khám xong

Chị Mai đăng ký, khai hồ sơ cho mình và con, đặt khám nhi với bác sĩ 2 (BS.CKI Trần Thị Bình), 08:00 thứ Hai 26/10/2026.

| Bước | Ai làm | Thao tác | Chứng từ | Kết quả mong đợi |
|---|---|---|---|---|
| 1 | Chị Mai (khách) | Trang `register`: `POST /auth/register` {`username` `mai.ngo`, `fullName` "Ngô Thị Mai", `email` `mai.ngo@example.com`, `phone`} | CT06 | 201; `accounts.id` = 12, `role` `PATIENT`, `enabled` true; người dùng Keycloak có `accountId` = 12 |
| 2 | Chị Mai | Đăng nhập qua Keycloak | — | Token có `accountId` = 12, `role` = `PATIENT`; Gateway gắn `X-User-Id: 12` |
| 3 | Chị Mai | `POST /patients` cho bản thân: Ngô Thị Mai, sinh 1992, nữ, `idType` `CCCD`, số 12 chữ số chưa ai dùng | CT07, CT08 | 201 `CREATED_ACTIVE`; `patients.id` = 11, `owner_account_id` = 12, `status` `ACTIVE`, `active_identity` = `CCCD:<số>` |
| 4 | Chị Mai | `POST /patients` cho con: Ngô Gia Huy, sinh 2020, nam, không giấy tờ, giám hộ Ngô Thị Mai (quan hệ "Mẹ"), email của mẹ | CT07 | 201 `CREATED_TEMPORARY`; `patients.id` = 12, `owner_account_id` = 12, `duplicateQueued` = false (không trùng tên + ngày sinh + SĐT ai) |
| 5 | Chị Mai | Trang `doctors`: chọn chuyên khoa Nhi khoa (id 2) → bác sĩ 2 | CT01, CT03, CT04 | `GET /doctors?specialtyId=2` trả bác sĩ 2; ca T2 07:30–11:30 |
| 6 | Chị Mai (21:00 ngày 25/10) | `GET /slots?doctorId=2&date=2026-10-26` | CT10 | 16 slot 15 phút từ 07:30; `available` = false ở 07:45 (lịch 7) và 09:00 (lịch 13); 08:00 `available` = true (lịch 12 đã `EXPIRED`) |
| 7 | Chị Mai (21:00) | `POST /appointments/hold` {`doctorId` 2, `patientId` 12, `startTime` 2026-10-26T08:00:00, `reason` "Ho, sốt 2 ngày"} | CT11 | 201; `appointments.id` = 15, `HELD`, `end_time` 08:15, `room_id` 2, `work_schedule_id` 8, `booked_by_account_id` 12, `hold_expires_at` 2026-10-25T21:05:00, `slot_lock` = `2@2026-10-26 08:00:00` |
| 8 | Chị Mai (21:02) | `POST /appointments/15/confirm` | CT12 | `CONFIRMED`, `confirmed_at` 21:02; Scheduling phát `appointment.confirmed` |
| 8b | Hệ thống (Reminder, vài giây sau) | Nhận `appointment.confirmed` | CT14 | Thư xác nhận tới `mai.ngo@example.com` theo mẫu `CONFIRMATION`; `reminder_logs.id` = 6, loại `CONFIRMATION`, `SENT` |
| 9 | Chị Mai | `GET /composite/my-appointments` | CT12 | Một `AppointmentView`: `appointmentId` 15, bác sĩ "Trần Thị Bình" BS.CKI Nhi khoa, phòng P102, bệnh nhân "Ngô Gia Huy" sinh 2020 |
| 10 | Hệ thống (cron 21:05) | Quét lịch `CONFIRMED` trong 24 giờ tới | CT15 | Lịch 15 nằm trong 24 giờ: gửi email tới `mai.ngo@example.com` (email của hồ sơ 12); `reminder_logs.id` = 7, loại `BEFORE_APPOINTMENT`, `SENT`, `attempt_count` 1, `appointment_start` 2026-10-26T08:00. Đặt dưới 24 giờ nên nhận cả thư xác nhận (bước 8b) lẫn thư nhắc (CS35) |
| 11 | Bác sĩ Bình (tài khoản 4) | 26/10, `GET /composite/doctor-agenda?doctorId=2&date=2026-10-26` | CT16 | Có lịch 7 (07:45), 15 (08:00, Ngô Gia Huy, lý do "Ho, sốt 2 ngày"), 13 (09:00) |
| 12 | Bác sĩ Bình | 08:12, `POST /appointments/15/complete` | CT17 | `COMPLETED`, `completed_at` 2026-10-26T08:12 |

### KB3 — Ông cụ không email, lễ tân đặt hộ (hồ sơ Nguyễn Văn Hùng)

Hồ sơ 7 (Nguyễn Văn Hùng, sinh 09/09/1958, CCCD, SĐT 0935111222, không email, không `owner_account_id`). Lễ tân (tài khoản 2) đặt khám Nội tổng quát bác sĩ 1, thứ Tư 28/10/2026 09:10.

| Bước | Ai làm | Thao tác | Chứng từ | Kết quả mong đợi |
|---|---|---|---|---|
| 1 | Lễ tân (26/10, 10:00) | Trang `patients`: `GET /patients?keyword=Hùng` | CT07 | Hai dòng: hồ sơ 7 (`ACTIVE`, sinh 1958-09-09) và hồ sơ 8 (`TEMPORARY`, sinh 1958-09-19, đang chờ duyệt trùng, cặp 8–7). Lễ tân chọn hồ sơ 7 |
| 2 | Lễ tân | `GET /slots?doctorId=1&date=2026-10-28` | CT10 | 21 slot (sáng 12, chiều 9); 09:10 `available` = true |
| 3 | Lễ tân | `POST /appointments/hold` {`doctorId` 1, `patientId` 7, `startTime` 2026-10-28T09:10:00, `reason` "Tái khám huyết áp"} | CT11 | 201; `appointments.id` = 15, `HELD`, `end_time` 09:30, `room_id` 1, `work_schedule_id` 3, `booked_by_account_id` 2, `hold_expires_at` 10:05 |
| 4 | Lễ tân | `POST /appointments/15/confirm` | CT12, CT14 | `CONFIRMED`; Reminder nhận `appointment.confirmed`, hồ sơ 7 không email → `reminder_logs.id` = 6, loại `CONFIRMATION`, `SKIPPED` |
| 5 | Người nhà (nếu có tài khoản) | `GET /composite/my-appointments` | CT12 | Không thấy lịch 15: hồ sơ 7 không có `owner_account_id` nên không thuộc tài khoản nào. Nếu hồ sơ có chủ thì chủ hồ sơ thấy cả lịch lễ tân đặt hộ (CS15) |
| 6 | Hệ thống (cron, khoảng 09:10 ngày 27/10) | Quét nhắc lịch | CT15 | `PatientClient` trả hồ sơ 7 không có email → `reminder_logs.id` = 7, loại `BEFORE_APPOINTMENT`, `status` `SKIPPED`, `recipient_email` NULL. Không có kênh nhắc khác (CS30) |
| 7 | Bác sĩ An (tài khoản 3) | 28/10, `GET /composite/doctor-agenda?doctorId=1&date=2026-10-28` | CT16 | Lịch 15 hiện bệnh nhân "Nguyễn Văn Hùng" sinh 1958-09-09 |
| 8 | Bác sĩ An | 09:30, `POST /appointments/15/complete` | CT17 | `COMPLETED`, `completed_at` 2026-10-28T09:30 |

### KB4 — Hai người tranh một slot, giữ chỗ quá hạn

Slot: bác sĩ 1, thứ Năm 29/10/2026 08:10 (lịch 4). Bên A: tài khoản 5 (hồ sơ 1). Bên B: tài khoản 10 (hồ sơ 4). Cả hai cùng dùng ngày 28/10.

| Bước | Ai làm | Thao tác | Chứng từ | Kết quả mong đợi |
|---|---|---|---|---|
| 1 | A và B (20:00) | `GET /slots?doctorId=1&date=2026-10-29` | CT10 | Cả hai thấy 08:10 `available` = true |
| 2 | A (20:00:00) | `POST /appointments/hold` {`doctorId` 1, `patientId` 1, `startTime` 2026-10-29T08:10:00} | CT11 | 201; `appointments.id` = 15, `HELD`, `hold_expires_at` 20:05, `slot_lock` = `1@2026-10-29 08:10:00` |
| 3 | B (20:00:01) | `POST /appointments/hold` {`doctorId` 1, `patientId` 4, cùng `startTime`} | CT11 | 409 `SLOT_ALREADY_HELD`; web tải lại slot, 08:10 `available` = false |
| 4 | B (20:03) | Thử giữ lại | CT11 | Vẫn 409 `SLOT_ALREADY_HELD` (chưa quá 20:05) |
| 5 | B (20:06) | Thử giữ lại | CT11 | Trước khi ghi, `expireHolds(now)` đưa lịch 15 sang `EXPIRED` (`slot_lock` NULL); lịch mới 201: `appointments.id` = 16, `HELD`, `hold_expires_at` 20:11 |
| 6 | A (20:07) | `POST /appointments/15/confirm` | CT12 | 409 `HOLD_EXPIRED` |
| 7 | A (20:07) | `GET /slots?doctorId=1&date=2026-10-29` | CT10 | 08:10 `available` = false (lịch 16 đang giữ) |
| 8 | B (20:08) | `POST /appointments/16/confirm` | CT12 | `CONFIRMED`, `confirmed_at` 20:08; `slot_lock` giữ nguyên |

### KB5 — Hủy kịp và hủy quá muộn

Hạn hủy: bệnh nhân hủy được khi còn từ 2 giờ trở lên; lễ tân không bị giới hạn (CS07).

| Bước | Ai làm | Thao tác | Chứng từ | Kết quả mong đợi |
|---|---|---|---|---|
| 1 | Anh Nam (tài khoản 10, 25/10 20:00) | `POST /appointments/8/cancel` {`reason` "Bận việc đột xuất"} (lịch 8: hồ sơ 4, bác sĩ 1, 26/10 08:30; còn 12 giờ 30 phút) | CT13 | `CANCELLED`, `cancel_reason`, `cancelled_by_account_id` 10, `cancelled_at` 25/10 20:00; `slot_lock` NULL |
| 2 | Chị Hoa (tài khoản 5, 20:10) | `GET /slots?doctorId=1&date=2026-10-26` | CT10 | 08:30 `available` = true |
| 3 | Chị Hoa | `POST /appointments/hold` {`doctorId` 1, `patientId` 1, `startTime` 2026-10-26T08:30:00} | CT11 | 201; `appointments.id` = 15, `HELD`. Hồ sơ 1 đã có lịch 6 lúc 08:10–08:30, liền kề nên không tính trùng giờ (CS12) |
| 4 | Chị Hoa (26/10, 07:30) | `POST /appointments/13/cancel` (lịch 13: hồ sơ 2, bác sĩ 2, 09:00; còn 1 giờ 30 phút; đặt bởi tài khoản 5) | CT13 | 409 `TOO_LATE_TO_CANCEL`; lịch 13 vẫn `CONFIRMED` |
| 5 | Lễ tân (tài khoản 2, 07:35) | `POST /appointments/13/cancel` {`reason` "Con sốt cao, gia đình báo qua điện thoại"} | CT13 | `CANCELLED`, `cancelled_by_account_id` 2 — lễ tân không bị giới hạn 2 giờ (CS07) |
| 6 | Chị Hoa | `POST /appointments/13/cancel` lần nữa | CT13 | 409 `INVALID_STATUS` |
| 7 | Hệ thống (Reminder) | Nhận `appointment.cancelled` của lịch 8 (bước 1) và lịch 13 (bước 5) | CT24 | Mỗi lịch một thư báo hủy, gửi tới email của hồ sơ (hồ sơ 4, hồ sơ 2), nội dung có lý do hủy; hồ sơ không email → `SKIPPED` (CS31, CS30). Lần hủy bị từ chối ở bước 4, 6 không sinh sự kiện |

### KB6 — Hồ sơ trùng → gộp → lịch chuyển → tách

Dùng cặp 3–1 trong hàng chờ (`duplicate_candidates.id` = 1, `SAME_NAME_DOB_PHONE`, 60 điểm): hồ sơ 3 "Le Thi Hoa" (`TEMPORARY`, không giấy tờ) và hồ sơ 1 "Lê Thị Hoa" (`ACTIVE`). Tình huống: lễ tân gộp, sau đó phát hiện hai người khác nhau dùng chung SĐT gia đình nên tách.

| Bước | Ai làm | Thao tác | Chứng từ | Kết quả mong đợi |
|---|---|---|---|---|
| 1 | Lễ tân (26/10, 09:00) | Đặt hộ hồ sơ 3 (đang `TEMPORARY`): giữ và xác nhận bác sĩ 6, 29/10 13:30 (lịch 24) | CT11, CT12 | `appointments.id` = 15, `patient_id` 3, `doctor_id` 6, `room_id` 6, `CONFIRMED`, `booked_by_account_id` 2 |
| 2 | Lễ tân | `GET /duplicate-candidates/count?status=PENDING` | CT20 | `{"count": 2}` (cặp 3–1 và 8–7) |
| 3 | Lễ tân | `GET /duplicate-candidates?status=PENDING`, mở cặp id 1 | CT20 | Hiện cạnh nhau hồ sơ 3 và hồ sơ 1, `reason` `SAME_NAME_DOB_PHONE`, `score` 60 |
| 4 | Lễ tân (09:30) | `POST /duplicate-candidates/1/merge` {`keepPatientId` 1, `note` "Cùng tên, ngày sinh, SĐT"} | CT21 | `duplicate_candidates.status` `MERGED`; hồ sơ 3 `MERGED`, `merged_into_id` 1; `merge_history.id` = 2, `action` `MERGE`, `source_patient_id` 3, `target_patient_id` 1, `source_previous_status` `TEMPORARY`; phát `PATIENT_MERGED` (`eventId` mới, `mergeHistoryId` 2) |
| 5 | Hệ thống (Scheduling) | Nhận `patient.merged` | CT22 | `processed_events` thêm `eventId`; lịch 15: `patient_id` 3 → 1; `patient_reassignments.id` = 2 (`appointment_id` 15, `from_patient_id` 3, `to_patient_id` 1, `reverted_at` NULL) |
| 6 | Bác sĩ Giang (tài khoản 9) hoặc Lễ tân | `GET /composite/doctor-agenda?doctorId=6&date=2026-10-29` | CT16 | Lịch 15 hiện bệnh nhân "Lê Thị Hoa" sinh 1990-05-12 (hồ sơ 1) |
| 7 | Lễ tân (11:00) | Phát hiện gộp nhầm: `POST /patients/3/unmerge` {`note` "Hai người khác nhau, chung SĐT gia đình"} | CT23 | Hồ sơ 3 về `TEMPORARY`, `merged_into_id` NULL; `merge_history.id` = 3, `action` `UNMERGE`, `related_merge_id` 2; phát `PATIENT_UNMERGED` (`relatedMergeEventId` = `eventId` bước 4) |
| 8 | Hệ thống (Scheduling) | Nhận `patient.unmerged` | CT22 | Lịch 15: `patient_id` 1 → 3; `patient_reassignments.id` = 2 có `reverted_at` |
| 9 | Hệ thống | Cặp id 1 sau khi tách | CT20 | Cặp id 1 chuyển `NOT_DUPLICATE`, `review_note` = ghi chú lần tách (CS24) |
| 10 | Hệ thống | Gửi lại cùng sự kiện lần hai (RabbitMQ giao lặp) | CT22 | Bỏ qua vì `eventId` đã có trong `processed_events`; lịch 15 không đổi |

### KB7 — Bệnh nhân không đến

Dùng lịch 6 (hồ sơ 1, bác sĩ 1, 26/10 08:10–08:30, `CONFIRMED`, đặt bởi tài khoản 5). Chính sách CS10.

| Bước | Ai làm | Thao tác | Chứng từ | Kết quả mong đợi |
|---|---|---|---|---|
| 1 | Hệ thống (cron) | Quét 24 giờ trước | CT15 | Hồ sơ 1 có email `hoa.le@example.com` → `SENT` |
| 2 | Bác sĩ An (tài khoản 3, 26/10 08:05) | `POST /appointments/6/no-show` | CT19 | 409 `TOO_EARLY_FOR_NO_SHOW` (chưa tới giờ bắt đầu 08:10) |
| 3 | Bác sĩ An (08:15) | Mở `GET /composite/doctor-agenda?doctorId=1&date=2026-10-26`, bệnh nhân không có mặt → bấm "Không đến" | CT16, CT19 | `POST /appointments/6/no-show` → `NO_SHOW`, `no_show_at` 08:15 |
| 4 | Bác sĩ An | Gọi lại `no-show` hoặc `complete` cho lịch 6 | CT19, CT17 | 409 `INVALID_STATUS` |
| 5 | Bác sĩ Bình (tài khoản 4, bác sĩ 2) | `POST /appointments/6/no-show` | CT19 | 403 `FORBIDDEN` (không phải lịch của mình) |
| 6 | Chị Hoa (tài khoản 5) | `GET /composite/my-appointments` | CT12 | Lịch 6 hiện `NO_SHOW`, phân biệt được với `CANCELLED` |
