# 00a. Những gì đã sẵn sàng — đọc trước khi code

File này liệt kê phần chung đã làm xong trước khi nhóm bắt đầu: hạ tầng, đăng nhập, Auth Service, giao diện chung, các API nội bộ mà nhiều người cần, và tài liệu thiết kế. Mỗi dòng có cách tự kiểm, để ai cũng tự xác nhận được trên máy mình.

Đọc xong file này thì đọc [`00b-phan-viec.md`](00b-phan-viec.md) (ai làm gì).

## 1. Việc đầu tiên trên máy mình

1. `git pull` nhánh `main`.
2. **Chạy một lần** `docker compose down -v`: hệ thống có thêm container Keycloak và database `keycloak_db`, dữ liệu cũ trên máy phải xóa để nạp lại từ đầu.
3. `docker compose up -d --build` → chờ đủ **12 container** healthy (lần đầu vài phút).
4. Làm theo các mục "Cách kiểm" ở bảng dưới. Chỗ nào không đúng thì báo trên nhóm, kèm ảnh chụp.

Cài đặt chi tiết, chế độ phát triển (mở Swagger từng service): [`docs/01`](01-huong-dan-cai-dat-va-chay.md).

## 2. Đã sẵn sàng

Cột "Trạng thái": ✅ đã chạy "Cách kiểm" và đạt (30/10/2026, trên máy sạch: 12 container dựng từ volume trống) · ⬜ chưa kiểm được hết, lý do ghi trong ô.

| # | Thành phần | Đã có gì | Cách kiểm | Trạng thái |
|---|---|---|---|---|
| 1 | Hạ tầng | 12 container: 5 service nghiệp vụ, Auth, Gateway, Eureka, MySQL (6 database), RabbitMQ, Mailpit, Keycloak, web | `docker compose ps` đủ 12 dòng `healthy`; trang chủ web `http://localhost:4200` báo các thành phần UP | ✅ 12/12 `healthy`, 6 database |
| 2 | Đăng nhập Keycloak | Realm (vùng tài khoản) nạp sẵn 11 tài khoản mẫu khớp `auth_db`, 4 vai trò; token có `accountId`, `role`, `doctorId` | Mở `http://localhost:8180/realms/dat-lich-kham/account`, đăng nhập bằng `benhnhan` / `123456`; trang quản trị `http://localhost:8180/admin` (`admin` / `admin_pw`) → realm `dat-lich-kham` → *Users* đủ 11 | ✅ 11 người dùng; token mẫu của `benhnhan` có `accountId` 5, `role` PATIENT; `bs.an` có `doctorId` 1 |
| 3 | Auth Service (trọn phần) | Đăng ký; tài khoản của tôi; danh sách, tạo, khóa / mở tài khoản nhân viên (gọi Keycloak Admin API, có bù trừ khi một bên lỗi); trang *Đăng ký* và trang quản trị *Tài khoản* | Trang *Đăng ký* tạo tài khoản mới → đăng nhập Keycloak được bằng tài khoản đó; chọn vai trò `admin`, trang *Tài khoản* tạo được tài khoản bác sĩ | ⬜ Phần API đã kiểm đạt (đăng ký, trùng tên / email, tạo bác sĩ, khóa / mở, Keycloak tắt thì không còn dòng thừa); hai trang build được nhưng chưa bấm thử trên trình duyệt |
| 4 | Giao diện chung | `frontend/src/styles/ui.css` (lớp `ui-*`), `shared/ui/` (nhãn trạng thái, hiển thị lỗi API), trang mẫu `/ui-kit` | `http://localhost:4200/ui-kit` hiển thị mẫu và quy tắc dùng | ⬜ Đã có trong code, `ng build` qua; chưa mở trình duyệt xem |
| 5 | Lỗi chung | Mọi service trả 404, 405 đúng dạng `ApiError` (`docs/06` A4); Gateway trả 503 đủ trường khi service tắt | Gọi một đường dẫn không tồn tại ở bất kỳ service → 404 dạng `ApiError` | ✅ 404, 405, `/internal` từ ngoài → 404, API ghép → 501, Provider tắt → 503 có `path` |
| 6 | Timeout chung | Một giá trị chờ tối đa cho mọi lời gọi service → service: `clinic.client-timeout = 3s` trong `application.yml` của Scheduling, Reminder, Gateway | `docker compose pause provider-service` → `GET /api/v1/slots` báo lỗi sau đúng khoảng timeout, không treo | ⬜ Đã cấu hình; chưa kiểm được vì `GET /slots` còn trả 501 trước khi gọi Provider — kiểm khi xong P2-3 |
| 7 | Correlation ID (mã truy vết request) | Gateway sinh `X-Correlation-Id`, mọi service ghi mã này vào log và chuyển tiếp khi gọi service khác | Gọi một API qua Gateway → log Gateway và log service có cùng một mã (`docker compose logs api-gateway auth-service`, mã nằm trong `[...]`) | ✅ Gateway và Auth cùng mã. Phần chuyển tiếp sang service thứ hai kiểm khi có API gọi nhau chạy được (P2-3) |
| 8 | API nội bộ giờ làm việc (Provider) | `GET /internal/v1/doctors/{id}/working-hours?date=` | Bác sĩ 1, một ngày thứ Hai → 2 ca (07:30–11:30 và 13:30–16:30, 20 phút); Chủ nhật → `shifts` rỗng. Gọi từ trong mạng Docker: `docker exec scheduling-service curl -s "http://provider-service:8082/internal/v1/doctors/1/working-hours?date=2026-11-02"` | ✅ |
| 9 | API nội bộ bác sĩ, phòng theo mã (Provider) | `GET /internal/v1/doctors?ids=`, `GET /internal/v1/rooms?ids=` | `?ids=1,2` trả đúng 2 bác sĩ kèm tên chuyên khoa (gọi như dòng 8) | ✅ |
| 10 | Chế độ thử | Web cho chọn vai trò thay đăng nhập và tự gắn header `X-User-*` | Ô chọn vai trò ở đầu trang; chọn `letan` → menu lễ tân | ✅ có từ trước |
| 11 | Dữ liệu mẫu | 11 tài khoản, 10 hồ sơ (đủ tình huống trùng, 1 vụ gộp), 14 lịch hẹn tính theo ngày chạy, 5 dòng nhắc lịch | [`docs/03` mục 5](03-thiet-ke-csdl.md) | ✅ có từ trước |
| 12 | Tài liệu thiết kế đã chốt | Xem mục 3 | Mở các file trong `docs/` | ✅ Đủ file, mọi liên kết giữa các file trỏ đúng |
| 13 | Dữ liệu cho trạng thái "không đến" | Bảng `appointments` có trạng thái `NO_SHOW`, cột `no_show_at`; lịch `NO_SHOW` vẫn giữ khóa slot (`docs/10` CS10). API ghi nhận là việc P2-10 | Trên `scheduling_db`: `UPDATE appointments SET status='NO_SHOW', no_show_at=NOW() WHERE id=6` chạy được | ✅ Lịch 6 thành `NO_SHOW`, vẫn giữ `slot_lock` |

## 3. Tài liệu — đọc theo thứ tự này

| Thứ tự | File | Để làm gì |
|---|---|---|
| 1 | [`00b-phan-viec.md`](00b-phan-viec.md) | Mình làm việc gì, làm gì trước, chờ ai |
| 2 | [`02-mo-ta-de-tai-va-he-thong.md`](02-mo-ta-de-tai-va-he-thong.md) + [`so-do/so-do-tong-quan.png`](so-do/so-do-tong-quan.png) | Hệ thống gồm những khối nào |
| 3 | [`11-module-va-quy-trinh.md`](11-module-va-quy-trinh.md) | Module, quy trình nghiệp vụ, quy trình tin học hóa — **mục 4.2 là đặc tả từng chức năng** (trang, API, kiểm tra, trạng thái, quyền) |
| 4 | [`10-bo-chung-tu-va-hop-dong-nghiep-vu.md`](10-bo-chung-tu-va-hop-dong-nghiep-vu.md) | Dữ liệu phải lưu; **mục 4 là chính sách đã chốt** (giữ chỗ bao lâu, hủy trước bao lâu…) — gặp câu "trường hợp này xử lý sao" thì tra ở đây trước |
| 5 | [`06-danh-sach-api-hop-dong-giua-cac-service-can-tuan-thu.md`](06-danh-sach-api-hop-dong-giua-cac-service-can-tuan-thu.md) | Hợp đồng API: đường dẫn, request, response, mã lỗi |
| 6 | [`03-thiet-ke-csdl.md`](03-thiet-ke-csdl.md) | Bảng, cột của database mình |
| 7 | [`04-cac-viec-can-trien-khai.md`](04-cac-viec-can-trien-khai.md) | Lớp nào cần viết, gợi ý dùng AI, liên hệ giáo trình |
| 8 | [`07-quy-tac-ket-noi.md`](07-quy-tac-ket-noi.md) | Quy tắc kết nối giữa web, Gateway, service, Keycloak |
| Khi cần | [`01`](01-huong-dan-cai-dat-va-chay.md) cài đặt · [`05`](05-gop-code-mpi-vao-patient-service.md) code MPI · [`09`](09-nguon-goc-petclinic.md) nguồn gốc Petclinic | |

## 4. Có thể tin vào đâu

- **Hợp đồng `docs/06` là chuẩn** giữa các phần. Code theo đúng hợp đồng thì không cần chờ người khác làm xong mới viết được.
- **Chính sách ở `docs/10` mục 4 đã chốt.** Không phải hỏi lại nhóm; muốn đổi thì sửa file đó trong PR và ghi lý do.
- **Phần đã sẵn sàng ở mục 2 không cần sửa.** Thấy lỗi thì mở issue, không sửa thẳng trong PR của phần mình.
