# Reminder Service — Dịch vụ Thông báo & Nhắc lịch (P5)

Dịch vụ thông báo và nhắc lịch thuộc đề tài **Nền tảng đặt lịch khám đa khoa (Hướng 4: Lập lịch & Xử lý ràng buộc thời gian)**.

---

## 1. Danh sách tính năng (docs/04 mục 2.5)

| Mã việc | Chức năng | API / Thành phần | Ghi chú |
|---|---|---|---|
| **P5-1** | Nhật ký thông báo | `GET /api/v1/reminders/logs?status=&type=` | Tối đa 100 bản ghi mới nhất, lọc theo trạng thái và loại thư. |
| **P5-2** | Quét nhắc lịch tự động | `POST /api/v1/reminders/run` & Cron `@Scheduled` (mỗi 5 phút) | Quét lịch `CONFIRMED` 24h tới từ Scheduling, gửi thư nhắc, loại trừ lịch đã gửi, thử lại các thư lỗi. |
| **P5-3** | Chứng minh chịu lỗi khi tắt / bật Reminder | Kiến trúc hàng đợi bền vững RabbitMQ | Tắt Reminder Service $\rightarrow$ Đặt lịch vẫn thành công; Bật lại $\rightarrow$ Tự động tiêu thụ sự kiện tồn đọng và quét bù. |
| **P5-4** | Giao diện quản trị | `admin/reminders` & `admin/message-templates` | Trang web Angular: xem nhật ký thông báo, nút "Quét ngay", link Mailpit, quản lý mẫu thư. |
| **P5-8** | Thư xác nhận đặt lịch | Listener `appointment.confirmed` | Nhận sự kiện RabbitMQ, gửi thư xác nhận kèm mã lịch hẹn, chống xử lý lặp bằng `processed_events`. |
| **P5-10** | Thư báo hủy lịch | Listener `appointment.cancelled` | Nhận sự kiện RabbitMQ, gửi thư thông báo hủy kèm lý do (`cancelReason`). |
| **P5-11** | Quản lý mẫu thư | `GET/PUT /api/v1/reminders/templates` | Xem và sửa nội dung 3 mẫu thư (`BEFORE_APPOINTMENT`, `CONFIRMATION`, `CANCELLATION`), render biến động `{patientName}`, `{startTime}`, `{appointmentId}`, `{cancelReason}`. |

---

## 2. Kịch bản minh chứng P5-3: Khả năng chịu lỗi (Fault Tolerance)

### Nguyên lý kiến trúc
- **Tách rời lỏng (Loose Coupling):** Khi bệnh nhân hoặc lễ tân đặt và xác nhận lịch hẹn, Scheduling Service chỉ phát sự kiện bất đồng bộ qua RabbitMQ Exchange `appointment.events`. Scheduling Service không gọi REST đồng bộ sang Reminder Service. Do đó, nếu Reminder Service tắt hoặc gặp sự cố, luồng đặt lịch khám của phòng khám **vẫn hoạt động 100% bình thường**.
- **Độ bền hàng đợi (Durable Queue):** Hàng đợi `reminder.appointment-events` được thiết lập `durable = true`. Tin nhắn trong hàng đợi được lưu trữ bền vững trên RabbitMQ broker.
- **Tự động phục hồi khi bật lại (Auto Recovery):** Khi Reminder Service khởi động lại, `@RabbitListener` tự động kích hoạt kết nối, lấy các sự kiện tồn đọng trong hàng đợi ra xử lý và gửi email xác nhận.
- **Quét nhắc lịch bù:** Cron 5 phút kế tiếp (hoặc thao tác bấm "Quét ngay" trên Web) sẽ quét cơ sở dữ liệu Scheduling và gửi email nhắc lịch trước 24 giờ cho lịch vừa đặt.

### Các bước thực nghiệm kiểm chứng
1. **Bước 1 (Dừng service):**
   ```bash
   docker compose stop reminder-service
   ```
2. **Bước 2 (Thực hiện đặt lịch):**
   Truy cập Web hoặc gọi API xác nhận lịch hẹn trong 24 giờ tới. Lịch hẹn được xác nhận thành công.
3. **Bước 3 (Kiểm tra hàng đợi):**
   Truy cập RabbitMQ Management (`http://localhost:15672`), xem queue `reminder.appointment-events`: Có 1 tin nhắn ở trạng thái `Ready`.
4. **Bước 4 (Khởi động lại service):**
   ```bash
   docker compose start reminder-service
   ```
5. **Bước 5 (Kiểm tra kết quả):**
   - Mở Mailpit (`http://localhost:8025`): Thư xác nhận (`CONFIRMATION`) được gửi đến hộp thư bệnh nhân.
   - Bấm nút **Quét ngay** trên giao diện *Nhật ký thông báo* (hoặc đợi 5 phút): Mailpit nhận tiếp thư nhắc lịch (`BEFORE_APPOINTMENT`).
   - Kiểm tra nhật ký: Các bản ghi được đánh dấu trạng thái `SENT` với thời điểm gửi rõ ràng.

---

## 3. Kiểm thử đơn vị (Unit Tests)

Chạy kiểm thử bằng Maven Wrapper:
```bash
./mvnw clean test -pl reminder-service
```
Hiện tại có **29/29 unit tests đạt 100%**, bao gồm:
- `AppointmentEventHandlerTest` (7 tests): xử lý xác nhận, hủy lịch, chống lặp bằng eventId, không có email, lỗi mail server, lỗi Patient Service, và phục hồi sự kiện sau khởi động lại (P5-3).
- `ReminderServiceTest` (10 tests): quét lịch 24h, loại trừ đã nhắc, xử lý lỗi, gửi lại thư FAILED, giới hạn 3 lần retry, bỏ qua lịch đã qua giờ, phục hồi email khi Patient Service sống lại, và quét bù sau khi start (P5-3).
- `ReminderControllerTest` (7 tests): kiểm tra API nhật ký và phân quyền ADMIN/RECEPTIONIST.
- `TemplateControllerTest` (5 tests): kiểm tra API xem/sửa mẫu thư và phân quyền ADMIN.
