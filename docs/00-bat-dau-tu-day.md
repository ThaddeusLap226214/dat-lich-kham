# 00. Bắt đầu từ đây

Đọc file này trước. Mục 1–4 đủ để cài, chạy hệ thống lần đầu và bắt đầu việc của mình. Mục 5 là phần chung đã làm sẵn. Mục 6, 7 là phụ lục: tra cứu khi cần, và tư liệu cho báo cáo.

## Mục lục

1. Bộ tài liệu
2. Cài và chạy lần đầu
3. Các lần sau: bật lại, cập nhật khi có thay đổi
4. Bắt đầu một việc
5. Phần chung đã sẵn sàng
6. Phụ lục tra cứu
7. Phụ lục cho báo cáo

---

## 1. Bộ tài liệu

| File | Đọc khi |
|---|---|
| `00-bat-dau-tu-day.md` (file này) | Ngày đầu: cài, chạy, tìm việc của mình |
| [`01-nghiep-vu-va-dac-ta-chuc-nang.md`](01-nghiep-vu-va-dac-ta-chuc-nang.md) | Code một chức năng: chức năng chạy ra sao (mục 6), theo chính sách nào (mục 7) |
| [`02-co-so-du-lieu.md`](02-co-so-du-lieu.md) | Cần biết bảng, cột, dữ liệu mẫu, kịch bản kiểm thử |
| [`03-hop-dong-api.md`](03-hop-dong-api.md) | Viết hoặc gọi một API, phát / nhận sự kiện |
| [`04-viec-cua-tung-nguoi.md`](04-viec-cua-tung-nguoi.md) | Mình làm việc gì, làm gì trước, chờ ai, khi nào là xong |

---

## 2. Cài và chạy lần đầu

### 2.1. Cần có

- **Docker Desktop** (kèm WSL 2 trên Windows) và **Git**. Không cần cài Java, Node.js hay MySQL: mọi thứ chạy trong Docker.
- Máy 16 GB RAM, ổ đĩa trống 6 GB, có Internet ở lần chạy đầu. Chi tiết phiên bản: mục 6.1.

### 2.2. Cài Docker Desktop

Tải tại docker.com, cài với tùy chọn **Use WSL 2**. Sau khi cài, mở Docker Desktop và chờ góc dưới trái hiện *Engine running*.

> **Lưu ý:** nếu Docker Desktop không chạy được và báo thiếu hoặc cần cập nhật WSL 2 (Windows Subsystem for Linux — phần chạy Linux bên trong Windows), mở PowerShell bằng quyền Administrator, chạy `wsl --install` (hoặc `wsl --update`) rồi khởi động lại máy. Nếu báo *Virtualization support not detected*, bật ảo hóa trong BIOS (mục *Intel VT-x* hoặc *AMD SVM*).

### 2.3. Giới hạn RAM cho Docker (Windows)

Docker Desktop trên Windows chạy trong một máy ảo WSL 2. Mặc định máy ảo này được dùng tới **50% RAM** (khoảng 7,8 GB trên máy 16 GB) và **không tự trả lại bộ nhớ đệm** cho Windows sau khi build, khiến máy có thể lên 95–99% RAM. Tạo file `C:\Users\<tên-tài-khoản>\.wslconfig` với nội dung:

```ini
[wsl2]
memory=5GB

[experimental]
autoMemoryReclaim=dropcache
```

Áp dụng: tắt Docker Desktop, chạy `wsl --shutdown` trong PowerShell, mở lại Docker Desktop. Kiểm tra bằng `docker info --format "{{.MemTotal}}"` — kết quả khoảng 5 000 000 000 byte là đúng.

Mức 5 GB được chọn theo bộ nhớ dùng thật, không phải tổng giới hạn: tổng `mem_limit` của các container trong `docker-compose.yml` là 9 × 512 MB + 2 × 64 MB + 1024 MB (Keycloak) ≈ 5,6 GB, nhưng các container không cùng chạm trần một lúc. Lúc bàn giao phần chung, hệ thống dùng khoảng 2,9 GB (đo bằng `docker stats` ngày 30/10/2026, Keycloak chiếm khoảng 0,7 GB); khi các service hoàn thiện, mức dùng sẽ tăng dần. Cần tăng `memory` nếu:

- nhóm thêm container mới hoặc tăng `mem_limit` của một service;
- một container tự khởi động lại liên tục và `docker inspect <tên-container> --format "{{.State.OOMKilled}}"` trả `true` (container bị tắt vì thiếu bộ nhớ).

### 2.4. Chọn thư mục đặt mã nguồn

Đặt repo ở **đường dẫn không dấu tiếng Việt**, ví dụ `D:\SOA\dat-lich-kham`. Windows mặc định dùng bảng mã cp1252, không biểu diễn được các chữ như "ế, ư, ớ"; Java đọc sai đường dẫn có các chữ này và lệnh `mvnw` báo lỗi `ClassNotFoundException: MavenWrapperMain`.

### 2.5. Khởi chạy

Mở Docker Desktop và chờ góc dưới trái hiện *Engine running*, sau đó chạy:

```powershell
git clone <địa chỉ repo> D:\SOA\dat-lich-kham
cd D:\SOA\dat-lich-kham
docker compose up -d --build
```

| Lần chạy | Thời gian đo được | Việc diễn ra |
|---|---|---|
| Lần đầu | Khoảng 2,5 phút + thời gian tải image nền (1–2 GB, tùy mạng) | Build 7 module Java và web, tạo cơ sở dữ liệu, nạp dữ liệu mẫu |
| Các lần sau (`docker compose up -d`) | Khoảng 30 giây | Chỉ khởi động container |

Kiểm tra trạng thái:

```powershell
docker compose ps
```

Hệ thống sẵn sàng khi **cả 12 container** có cột STATUS dạng `Up … (healthy)`:

```
mysql, rabbitmq, mailpit, keycloak, discovery-server, api-gateway,
auth-service, provider-service, patient-service, scheduling-service, reminder-service, frontend
```

Thứ tự khởi động được điều khiển bởi `depends_on … condition: service_healthy`: MySQL, RabbitMQ, Eureka trước → Keycloak, 5 service → Gateway → web. Keycloak lần đầu mất khoảng 1,5 phút (tạo bảng trong `keycloak_db`, nạp realm).

### 2.6. Địa chỉ truy cập

| Địa chỉ | Thành phần | Dùng để |
|---|---|---|
| **http://localhost:4200** | Web | Giao diện người dùng |
| http://localhost:8080 | API Gateway | Cổng vào duy nhất của mọi API (`/api/v1/...`) |
| http://localhost:8180/admin | Keycloak (quản trị) | Xem người dùng, vai trò của realm `dat-lich-kham`; khóa tài khoản, đổi vai trò, đặt lại mật khẩu. Tài khoản `admin` / `admin_pw` (đổi trong `.env`) |
| http://localhost:8180/realms/dat-lich-kham/account | Keycloak (tài khoản cá nhân) | Đăng nhập thử bằng tài khoản mẫu, ví dụ `benhnhan` / `123456` |
| http://localhost:8761 | Eureka | Xem các service đã đăng ký vào danh bạ |
| http://localhost:15672 | RabbitMQ Management | Xem exchange, hàng đợi, tin nhắn. Tài khoản `clinic` / `clinic_pw` |
| http://localhost:8025 | Mailpit | Xem email nhắc lịch đã gửi |
| `localhost:3307` | MySQL | Kết nối bằng công cụ xem CSDL (mục 6.5) |

Tài khoản mẫu (mật khẩu chung `123456`): `admin`, `letan`, `bs.an`, `bs.binh`, `benhnhan`; đủ 11 tài khoản ở [`02-co-so-du-lieu.md`](02-co-so-du-lieu.md) mục 5.

### 2.7. Kiểm tra trên web

Mở http://localhost:4200. Trang chủ gọi thật một API của mỗi thành phần qua Gateway và hiển thị kết quả:

| Kết quả | Ý nghĩa |
|---|---|
| **200 — đã chạy** | Phần xử lý đã viết, service trả dữ liệu thật |
| **501 — chưa làm** | Service đang chạy, request đi đúng đường qua Gateway, phần xử lý bên trong chưa viết (xem `04-viec-cua-tung-nguoi.md`) |
| **503 / không phản hồi** | Service chưa bật xong hoặc đã tắt |

Thành phần nào đã có phần xử lý thì báo 200, phần còn chờ người phụ trách viết thì báo 501.

**Chế độ thử (chọn vai trò).**

Keycloak đã chạy và giữ 11 tài khoản mẫu, nhưng web chưa chuyển sang trang đăng nhập Keycloak và Gateway chưa xác minh token (việc P4-4, P4-9). Trong lúc đó, ô **Chế độ thử** trên thanh đầu trang cho phép chọn một trong các tài khoản mẫu. Web tự gắn header `X-User-Id`, `X-User-Role`, `X-Doctor-Id` vào mọi request — đúng các header mà Gateway sẽ gắn sau khi xác minh token (xem `03-hop-dong-api.md` mục A5). Menu bên trái thay đổi theo vai trò.

Mọi vai trò đều thấy *Trang chủ* và *Chuyên khoa & bác sĩ*; ngoài ra:

| Vai trò | Tài khoản mẫu | Trang riêng trong menu |
|---|---|---|
| Khách | — | Đăng nhập, Đăng ký |
| Bệnh nhân | `benhnhan` | Đặt lịch, Lịch khám của tôi, Hồ sơ bệnh nhân của tôi |
| Lễ tân | `letan` | Đặt lịch, Hồ sơ bệnh nhân, Hàng chờ duyệt hồ sơ trùng, Lịch hẹn theo bác sĩ |
| Bác sĩ | `bs.an` | Hồ sơ bệnh nhân, Lịch khám trong ngày |
| Quản trị | `admin` | Chuyên khoa – bác sĩ – phòng, Lịch làm việc, Tài khoản, Nhật ký nhắc lịch |

Mật khẩu chung của các tài khoản mẫu là `123456` (lưu ở Keycloak). Mỗi trang chưa hoàn thiện hiển thị phần việc phụ trách, danh sách việc cần làm, API chính và nút **Gọi thử** để xem phản hồi thật của backend.

**Xong khi:** `docker compose ps` đủ 12 container `healthy`; trang chủ http://localhost:4200 mở được; chọn vai trò `letan` thấy menu lễ tân.

---

## 3. Các lần sau: bật lại, cập nhật khi có thay đổi

### 3.1. Bật lại, tắt

Mở Docker Desktop, chờ góc dưới trái hiện *Engine running*, rồi chạy trong thư mục dự án:

| Việc | Lệnh | Ghi chú |
|---|---|---|
| Bật lại | `docker compose up -d` | Khoảng 30 giây; kiểm bằng `docker compose ps` đủ 12 container `healthy` |
| Tắt, giữ dữ liệu | `docker compose down` | Dữ liệu còn trong volume `dat-lich-kham_mysql-data` |
| Tắt và xóa sạch dữ liệu | `docker compose down -v` | Lần bật sau nạp lại dữ liệu mẫu |

`up -d` **không build lại code**: chỉ dùng khi code trên máy không đổi so với lần bật trước.

### 3.2. Cập nhật khi có thay đổi

| Trường hợp | Làm gì |
|---|---|
| Có code mới trên `main` (vừa có PR được merge) | `git checkout main` → `git pull` → `docker compose up -d --build`. Đang làm dở trên nhánh của mình: commit trước, rồi trên nhánh đó chạy `git merge main` để lấy code mới |
| Mình vừa sửa code một service | `docker compose up -d --build <tên-service>` (ví dụ `scheduling-service`): chỉ build lại service đó |
| PR vừa merge có ghi "cần chạy `docker compose down -v`" (có sửa cấu trúc bảng), hoặc máy từng chạy bản trước khi có Keycloak | `docker compose down -v` → `docker compose up -d --build`. Lý do: `schema.sql` không tự thêm cột vào bảng đã có, script tạo database chỉ chạy khi volume còn trống. Mất dữ liệu thử trên máy mình; dữ liệu mẫu tự nạp lại |
| Dữ liệu mẫu đã cũ (ngày lịch hẹn tính theo lần khởi tạo đầu, lâu ngày không còn quanh hôm nay) | Như dòng trên |
| Sửa web, muốn trang tự tải lại khi lưu code | Mục 6.3.3 |

---

## 4. Bắt đầu một việc

1. Mở [`04-viec-cua-tung-nguoi.md`](04-viec-cua-tung-nguoi.md): mục 1 (mình phụ trách service, trang nào), **mục 2.x của mình** (đang ở đâu, bảng việc — làm việc ghi "Làm trước tiên" trước), mục 4 (issue → nhánh → PR).
2. Mỗi việc trong bảng có mã chức năng `F..` và chính sách `CS..`. Tra theo mã:

   | Cần biết | Mở |
   |---|---|
   | Trang nào, API nào, kiểm tra gì, chuyển trạng thái nào, ai được làm | `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 6.1, dòng `F..` của việc |
   | Giới hạn, con số, mã lỗi nghiệp vụ | `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 7, bảng chính sách của service mình |
   | Đường dẫn, request, response, mã lỗi | `03-hop-dong-api.md`: A1–A6 đọc một lần; mục B của service mình; dùng RabbitMQ thì A7 |
   | Bảng, cột; dữ liệu mẫu để hiểu các con số trong *Xong khi* | `02-co-so-du-lieu.md` mục 3.x (database của mình), mục 5 |

3. Tìm chỗ còn trả 501 trong service: `04-viec-cua-tung-nguoi.md` mục 5.1. Phải chờ việc của người khác: dùng bản giả (`04-viec-cua-tung-nguoi.md` mục 3).
4. Thử API trực tiếp bằng Swagger: mục 6.3, 6.4.

Chưa cần đọc khi mới bắt đầu: `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 2–5 (bức tranh chung), `02-co-so-du-lieu.md` mục 7–8, mục 5–7 của file này.

---

## 5. Phần chung đã sẵn sàng

Cột "Trạng thái": ✅ đã chạy "Cách kiểm" và đạt (30/10/2026, trên máy sạch: 12 container dựng từ volume trống) · ⬜ chưa kiểm được hết, lý do ghi trong ô.

| # | Thành phần | Đã có gì | Cách kiểm | Trạng thái |
|---|---|---|---|---|
| 1 | Hạ tầng | 12 container: 5 service nghiệp vụ, Auth, Gateway, Eureka, MySQL (6 database), RabbitMQ, Mailpit, Keycloak, web | `docker compose ps` đủ 12 dòng `healthy`; trang chủ web `http://localhost:4200` báo các thành phần UP | ✅ 12/12 `healthy`, 6 database |
| 2 | Đăng nhập Keycloak | Realm (vùng tài khoản) nạp sẵn 11 tài khoản mẫu khớp `auth_db`, 4 vai trò; token có `accountId`, `role`, `doctorId` | Mở `http://localhost:8180/realms/dat-lich-kham/account`, đăng nhập bằng `benhnhan` / `123456`; trang quản trị `http://localhost:8180/admin` (`admin` / `admin_pw`) → realm `dat-lich-kham` → *Users* đủ 11 | ✅ 11 người dùng; token mẫu của `benhnhan` có `accountId` 5, `role` PATIENT; `bs.an` có `doctorId` 1 |
| 3 | Auth Service (trọn phần) | Đăng ký; tài khoản của tôi; danh sách, tạo, khóa / mở tài khoản nhân viên (gọi Keycloak Admin API, có bù trừ khi một bên lỗi); trang *Đăng ký* và trang quản trị *Tài khoản* | Trang *Đăng ký* tạo tài khoản mới → đăng nhập Keycloak được bằng tài khoản đó; chọn vai trò `admin`, trang *Tài khoản* tạo được tài khoản bác sĩ | ⬜ Phần API đã kiểm đạt (đăng ký, trùng tên / email, tạo bác sĩ, khóa / mở, Keycloak tắt thì không còn dòng thừa); hai trang build được nhưng chưa bấm thử trên trình duyệt |
| 4 | Giao diện chung | `frontend/src/styles/ui.css` (lớp `ui-*`), `shared/ui/` (nhãn trạng thái, hiển thị lỗi API), trang mẫu `/ui-kit` | `http://localhost:4200/ui-kit` hiển thị mẫu và quy tắc dùng | ⬜ Đã có trong code, `ng build` qua; chưa mở trình duyệt xem |
| 5 | Lỗi chung | Mọi service trả 404, 405 đúng dạng `ApiError` (`03-hop-dong-api.md` A4); Gateway trả 503 đủ trường khi service tắt | Gọi một đường dẫn không tồn tại ở bất kỳ service → 404 dạng `ApiError` | ✅ 404, 405, `/internal` từ ngoài → 404, API ghép → 501, Provider tắt → 503 có `path` |
| 6 | Timeout chung | Một giá trị chờ tối đa cho mọi lời gọi service → service: `clinic.client-timeout = 3s` trong `application.yml` của Scheduling, Reminder, Gateway | `docker compose pause provider-service` → `GET /api/v1/slots` báo lỗi sau đúng khoảng timeout, không treo | ⬜ Đã cấu hình; chưa kiểm được vì `GET /slots` còn trả 501 trước khi gọi Provider — kiểm khi xong P2-3 |
| 7 | Correlation ID (mã truy vết request) | Gateway sinh `X-Correlation-Id`, mọi service ghi mã này vào log và chuyển tiếp khi gọi service khác | Gọi một API qua Gateway → log Gateway và log service có cùng một mã (`docker compose logs api-gateway auth-service`, mã nằm trong `[...]`) | ✅ Gateway và Auth cùng mã. Phần chuyển tiếp sang service thứ hai kiểm khi có API gọi nhau chạy được (P2-3) |
| 8 | API nội bộ giờ làm việc (Provider) | `GET /internal/v1/doctors/{id}/working-hours?date=` | Bác sĩ 1, một ngày thứ Hai → 2 ca (07:30–11:30 và 13:30–16:30, 20 phút); Chủ nhật → `shifts` rỗng. Gọi từ trong mạng Docker: `docker exec scheduling-service curl -s "http://provider-service:8082/internal/v1/doctors/1/working-hours?date=2026-11-02"` | ✅ |
| 9 | API nội bộ bác sĩ, phòng theo mã (Provider) | `GET /internal/v1/doctors?ids=`, `GET /internal/v1/rooms?ids=` | `?ids=1,2` trả đúng 2 bác sĩ kèm tên chuyên khoa (gọi như dòng 8) | ✅ |
| 10 | Chế độ thử | Web cho chọn vai trò thay đăng nhập và tự gắn header `X-User-*` | Ô chọn vai trò ở đầu trang; chọn `letan` → menu lễ tân | ✅ có từ trước |
| 11 | Dữ liệu mẫu | 11 tài khoản, 24 hồ sơ (đủ tình huống trùng, 8 lần gộp, 2 lần tách), 24 lịch hẹn tính theo ngày chạy, 17 dòng nhật ký thông báo, 3 mẫu thư; mọi bảng sinh ra khi thao tác có ít nhất 10 dòng | [`02-co-so-du-lieu.md`](02-co-so-du-lieu.md) mục 5 | ⬜ Bổ sung 30/10, chờ kiểm trên volume trống |
| 12 | Tài liệu | 5 file trong `docs/` (bảng "Bộ tài liệu" ở đầu file này) | Mở từng file trên GitHub, bấm thử các liên kết | ✅ Đủ 5 file, mọi liên kết giữa các file trỏ đúng |
| 13 | Dữ liệu cho trạng thái "không đến" | Bảng `appointments` có trạng thái `NO_SHOW`, cột `no_show_at`; lịch `NO_SHOW` vẫn giữ khóa slot (CS10, `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 7). API ghi nhận là việc P2-10 | Trên `scheduling_db`: `UPDATE appointments SET status='NO_SHOW', no_show_at=NOW() WHERE id=6` chạy được | ✅ Lịch 6 thành `NO_SHOW`, vẫn giữ `slot_lock` |
| 14 | Bảng của Reminder | `message_templates` (3 mẫu nạp sẵn), `processed_events`; `reminder_logs` nhận 3 loại thư. Code dùng các bảng là việc của Hiếu | Trên `reminder_db`: `SELECT COUNT(*) FROM message_templates` → 3; `SELECT COUNT(*) FROM processed_events` → 11 | ⬜ Lần kiểm trước (9 sự kiện, 14 nhật ký) đạt; dữ liệu bổ sung 30/10 chờ kiểm lại |

**Có thể tin vào đâu:**

- **Hợp đồng `03-hop-dong-api.md` là chuẩn** giữa các phần. Code theo đúng hợp đồng thì không cần chờ người khác làm xong mới viết được.
- **Chính sách ở `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 7 đã chốt.** Không phải hỏi lại nhóm; muốn đổi thì sửa file đó trong PR và ghi lý do.
- **Phần đã sẵn sàng ở bảng trên không cần sửa.** Thấy lỗi thì mở issue, không sửa thẳng trong PR của phần mình.

---

## 6. Phụ lục tra cứu

### 6.1. Yêu cầu máy và phần mềm

Số liệu thời gian và bộ nhớ đo trên máy Windows 11, 16 GB RAM, CPU 20 luồng.

**Phần cứng**

| Thành phần | Tối thiểu | Ghi chú |
|---|---|---|
| RAM | 16 GB | Toàn hệ thống khi chạy dùng khoảng **2,2 GB** trong Docker; lúc build dùng thêm khoảng 1–1,5 GB. Phần còn lại dành cho Windows, IDE, trình duyệt |
| Ổ đĩa trống | 6 GB | Các image nền (Maven, JDK, MySQL, RabbitMQ, Node, nginx) khoảng 2,5 GB, image của hệ thống khoảng 1,5 GB |
| Mạng | Có Internet ở lần chạy đầu | Tải image và thư viện; các lần sau chạy không cần mạng |

**Phần mềm**

| Phần mềm | Bắt buộc khi | Phiên bản đã kiểm thử | Kiểm tra |
|---|---|---|---|
| **Docker Desktop** (kèm WSL 2 trên Windows) | Luôn luôn | 29.8 | `docker version` |
| **Git** | Luôn luôn | — | `git --version` |
| JDK | Sửa và build backend ngoài Docker | Temurin 25 | `java -version` |
| Node.js | Sửa và chạy frontend ngoài Docker | 22 trở lên (đã thử 24.14) | `node --version` |
| Công cụ xem CSDL (DBeaver, MySQL Workbench) | Xem dữ liệu | — | — |

### 6.2. Công nghệ bên trong hệ thống

| Lớp | Công nghệ | Vai trò |
|---|---|---|
| Ngôn ngữ, khung backend | Java 25, Spring Boot 4.0.7 | 5 service nghiệp vụ |
| Hạ tầng microservices | Spring Cloud 2025.1.3: Gateway, Netflix Eureka, LoadBalancer, Circuit Breaker Resilience4j | Cổng vào, danh bạ service, gọi nhau theo tên, chống lỗi lan truyền |
| Cơ sở dữ liệu | MySQL 8.4 | Một máy chủ, 5 cơ sở dữ liệu tách biệt |
| Message broker (hệ thống trung gian chuyển tin nhắn) | RabbitMQ 4 | Sự kiện gộp / tách hồ sơ bệnh nhân |
| Frontend | Angular 18.2, phục vụ bằng nginx | Web cho 5 vai trò người dùng |
| Hộp thư giả lập | Mailpit | Nhận email nhắc lịch khi chạy thử |
| Tài liệu API | springdoc-openapi (Swagger UI) | Trang thử API của từng service |

### 6.3. Làm việc khi phát triển

#### 6.3.1. Chế độ phát triển

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

File `docker-compose.dev.yml` mở thêm cổng riêng của 5 service (8081–8085) để thử API trên Swagger mà không qua Gateway. Khi chạy chính thức (demo, nộp bài) chỉ dùng `docker-compose.yml`, các service không mở cổng ra ngoài.

#### 6.3.2. Các lệnh thường dùng

| Việc | Lệnh |
|---|---|
| Build lại và chạy lại một service sau khi sửa code | `docker compose up -d --build scheduling-service` |
| Xem log trực tiếp của một service | `docker compose logs -f scheduling-service` |
| Build thử backend trên máy (cần JDK 25) | `.\mvnw.cmd -pl scheduling-service package -DskipTests` |
| Chạy unit test một service | `.\mvnw.cmd -pl patient-service test` |
| Tìm các chỗ còn chờ viết trong một service | `Get-ChildItem .\scheduling-service\src -Recurse -Filter *.java \| Select-String "NotImplementedYetException\("` |

#### 6.3.3. Chạy frontend ở chế độ phát triển

Chế độ này tự tải lại trang mỗi khi sửa code. Container `frontend` đang chiếm cổng 4200 nên cần tắt trước:

```powershell
docker compose stop frontend
cd frontend
npm install
npm start          # http://localhost:4200
```

### 6.4. Thử API bằng Swagger

Chạy ở chế độ phát triển (mục 6.3.1), sau đó mở trang Swagger của từng service:

| Service | Swagger |
|---|---|
| auth-service | http://localhost:8081/swagger-ui.html |
| provider-service | http://localhost:8082/swagger-ui.html |
| patient-service | http://localhost:8083/swagger-ui.html |
| scheduling-service | http://localhost:8084/swagger-ui.html |
| reminder-service | http://localhost:8085/swagger-ui.html |

Các API cần danh tính yêu cầu header `X-User-Id` và `X-User-Role`; nhập trực tiếp trong Swagger (ví dụ `5` và `PATIENT`).

Ví dụ gọi qua Gateway bằng PowerShell:

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/patients?keyword=le thi hoa" -Headers @{ "X-User-Id" = "2"; "X-User-Role" = "RECEPTIONIST" }
```

Kết quả gồm 2 hồ sơ (mã 1 và 3). PowerShell 5.1 có thể hiển thị sai dấu tiếng Việt (ví dụ `LÃª Thá»‹ Hoa`) do tự đoán bảng mã của phản hồi; dữ liệu vẫn đúng, xem trên trình duyệt hoặc Swagger sẽ hiển thị chuẩn.

### 6.5. Kết nối cơ sở dữ liệu

Một máy chủ MySQL chứa 6 cơ sở dữ liệu (5 service + `keycloak_db` của Keycloak — chỉ Keycloak dùng, không ai đọc trực tiếp). Mỗi service có tài khoản riêng và **chỉ có quyền trên cơ sở dữ liệu của mình** (tài khoản của service này truy vấn bảng của service khác sẽ bị MySQL từ chối — lỗi 1142).

| Thông số | Giá trị |
|---|---|
| Host / Port | `localhost` / `3307` |
| Tài khoản quản trị | `root` / `root_pw` (thấy cả 5 cơ sở dữ liệu) |

| Cơ sở dữ liệu | Tài khoản / mật khẩu | Service sở hữu |
|---|---|---|
| `auth_db` | `auth_user` / `auth_user_pw` | Auth Service |
| `provider_db` | `provider_user` / `provider_user_pw` | Provider Service |
| `patient_db` | `patient_user` / `patient_user_pw` | Patient Service |
| `scheduling_db` | `scheduling_user` / `scheduling_user_pw` | Scheduling Service |
| `reminder_db` | `reminder_user` / `reminder_user_pw` | Reminder Service |
| `keycloak_db` | `keycloak_user` / `keycloak_user_pw` | Keycloak |

Các mật khẩu trên là giá trị mặc định chỉ dùng khi chạy trên máy cá nhân; có thể đổi bằng file `.env` (mẫu ở `.env.example`). Cấu trúc bảng và dữ liệu mẫu: `02-co-so-du-lieu.md`.

### 6.6. Xử lý sự cố

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Vừa bật, gọi API nhận 503 | Service vừa khởi động, chưa đăng ký xong với Eureka | Chờ tới khi `docker compose ps` báo đủ 12 container `healthy` (khoảng 30 giây; lần đầu Keycloak lâu hơn) |
| Máy chậm, RAM Windows trên 90% | Máy ảo Docker giữ bộ nhớ đệm | Làm mục 2.3; xả ngay: `docker run --rm --privileged alpine sh -c "sync; echo 3 > /proc/sys/vm/drop_caches"` |
| `mvnw` báo `ClassNotFoundException: MavenWrapperMain` | Đường dẫn chứa chữ có dấu | Chuyển repo sang thư mục không dấu (mục 2.4) |
| `port is already allocated` | Cổng 3307, 8080, 4200… đang bị chương trình khác dùng | Tắt chương trình đó, hoặc đổi số cổng bên trái trong `ports:` của `docker-compose.yml` |
| Sửa `schema.sql` nhưng bảng không đổi | `CREATE TABLE IF NOT EXISTS` bỏ qua bảng đã có | `docker compose down -v` rồi `docker compose up -d --build` (mất dữ liệu trên máy mình) |
| Một container báo `unhealthy` hoặc tự khởi động lại liên tục | Lỗi khi khởi động service | `docker compose logs <tên-service>` và tìm dòng `ERROR` / `Caused by` |
| Build frontend báo `npm ci … not in sync` | `package-lock.json` lệch `package.json` | Trong `frontend/`: `npm install --package-lock-only`, rồi build lại |

---

## 7. Phụ lục cho báo cáo

Tư liệu dùng khi viết báo cáo phần chung: đề tài yêu cầu gì, hệ thống đáp ứng ở đâu, khái niệm giáo trình nào thể hiện ở đâu, mã nguồn lấy từ đâu.

### 7.1. Yêu cầu của đề tài

#### 7.1.1. Yêu cầu riêng của Hướng 4

| Yêu cầu | Nội dung |
|---|---|
| Trọng tâm kỹ thuật | Thuật toán tìm khoảng thời gian trống, giải quyết đụng độ lịch (conflict resolution), tác vụ chạy ngầm theo thời gian (cron job) |
| Provider/Resource Service | Quản lý bên cung cấp dịch vụ (bác sĩ) và cấu hình khung giờ khả dụng |
| Consumer Service | Quản lý thông tin người đặt lịch (bệnh nhân) |
| Scheduling Service | Logic lõi kiểm tra slot trống và giữ chỗ |
| Reminder/Cron Service | Dịch vụ độc lập, định kỳ quét cơ sở dữ liệu để gửi nhắc trước X giờ |
| Trọng tâm báo cáo | Cách thiết kế API tại API Gateway để **tổng hợp dữ liệu (API Composition)**: khi hiển thị lịch trình, Gateway gọi Provider Service lấy tên bác sĩ, gọi Scheduling Service lấy thời gian, rồi ghép lại trả cho frontend |

#### 7.1.2. Tiêu chí đánh giá chung

1. Có **1 API Gateway** đứng trước toàn bộ hệ thống.
2. Triển khai tối thiểu **4 Core Microservices**.
3. Dùng **Docker Compose** để khởi chạy cục bộ toàn bộ hệ thống.

### 7.2. Đối chiếu yêu cầu của đề tài

| Yêu cầu | Đáp ứng |
|---|---|
| 1 API Gateway trước toàn hệ thống | `api-gateway` — `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 2 |
| Tối thiểu 4 Core Microservices | 5 service: Auth, Provider, Patient, Scheduling, Reminder |
| Docker Compose chạy toàn hệ thống | `docker-compose.yml`: 12 container — file này |
| Hướng 4: Provider, Consumer, Scheduling, Reminder | Provider Service, Patient Service, Scheduling Service, Reminder Service |
| Hướng 4: tìm slot trống, đụng độ lịch | Scheduling Service — `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 5.1, luồng 1, 2 |
| Hướng 4: tác vụ chạy ngầm định kỳ | Reminder Service — `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 5.1, luồng 5 |
| Hướng 4: API Composition tại Gateway | `01-nghiep-vu-va-dac-ta-chuc-nang.md` mục 5.1, luồng 3 |

### 7.3. Đối chiếu với giáo trình

Giáo trình: `LectureNotesSOA-new.pdf`.

| Khái niệm trong giáo trình | Trang | Thể hiện trong hệ thống |
|---|---|---|
| Standardized Service Contract, Service Versioning | 11, 21–24 | `03-hop-dong-api.md`; đường dẫn có phiên bản `/api/v1/`; Swagger mỗi service |
| Loose Coupling, Service Autonomy | 12–13 | Không dùng chung cơ sở dữ liệu, không dùng chung code giữa các service; mỗi service tự build, tự chạy |
| Service Statelessness | 14, 55 | Service không giữ phiên đăng nhập; danh tính đi theo từng request (JWT → header) |
| Service Discoverability, Service Registry / Discovery | 14, 34–35 | Eureka (`discovery-server`), gọi nhau bằng `lb://ten-service` |
| Service Composability, API Composition | 14, 89–91, 108 | `/api/v1/composite/my-appointments`, `/api/v1/composite/doctor-agenda` tại Gateway |
| Phân rã theo Business Capability, tránh Nano Service | 17–18, 87 | 5 service ứng với 5 năng lực nghiệp vụ; MPI nằm gọn trong Patient Service thay vì tách thành nhiều service nhỏ |
| Database per Service, ERD phân mảnh, entity chỉ giữ mã | 88–89 | `02-co-so-du-lieu.md` |
| Saga (Choreography) và giao dịch bù trừ | 48, 92–93 | Gộp hồ sơ → Scheduling chuyển lịch hẹn; tách hồ sơ → hoàn lại (bù trừ) |
| Eventual Consistency (nhất quán sau cùng) | 48, 86 | Lịch hẹn được chuyển sang hồ sơ mới sau khi sự kiện gộp tới Scheduling |
| Pub/Sub qua Message Broker, Dead Letter Queue | 103–107 | Exchange `patient.events` (hàng đợi `scheduling.patient-events`), `appointment.events` (hàng đợi `reminder.appointment-events`), mỗi hàng đợi có DLQ |
| Idempotency (lũy đẳng) | 46, 56 | Bảng `processed_events` (không xử lý một sự kiện hai lần), `reminder_logs` (không nhắc hai lần) |
| Timeout, Retry, Circuit Breaker, Fallback | 45–46, 109, 111 | Resilience4j tại Gateway; Reminder thử lại lần quét sau |
| JWT, API Gateway làm lớp bảo mật đầu tiên, 401 / 403 | 41–43, 73–77 | Auth Service cấp JWT; Gateway xác minh, gắn danh tính; service kiểm tra quyền |
| Kiểm tra quyền trên từng bản ghi (lỗi "không kiểm tra Authorization") | 43 | Bệnh nhân chỉ xem được hồ sơ, lịch hẹn của chính mình (403) |
| CORS | 79–80 | Bộ lọc CORS tại Gateway cho web ở cổng 4200 |
| Correlation ID (truy vết request qua nhiều service) | 85 | Gateway sinh `X-Correlation-Id`, mọi service ghi vào log và chuyển tiếp khi gọi nhau (đã làm) |
| Containerization, Dockerfile | 112–115 | `Dockerfile` build nhiều giai đoạn, `docker-compose.yml` |

### 7.4. Phần lấy từ Spring Petclinic

Hạ tầng của hệ thống dựng trên [Spring Petclinic Microservices](https://github.com/spring-petclinic/spring-petclinic-microservices) (dự án mẫu mã nguồn mở minh họa microservices bằng Spring Cloud, nghiệp vụ phòng khám thú y), nhánh `main`, commit `aefaf7f`, giấy phép Apache-2.0. Giấy phép cho dùng lại và sửa đổi với điều kiện giữ thông báo bản quyền: repo giữ file `LICENSE` và phần đầu hai file Java có nguồn gốc Petclinic.

| Phần của Petclinic | Áp dụng ở đâu trong hệ thống | Mức dùng lại |
|---|---|---|
| Discovery Server (Eureka) | `discovery-server/`: danh bạ service, mỗi service tự đăng ký, gọi nhau theo tên | Lấy nguyên, đổi tên gói; `DiscoveryServerApplication.java` còn thông báo bản quyền |
| Khung API Gateway: `WebClient` gắn `@LoadBalanced` (gọi service theo tên), Circuit Breaker Resilience4j bọc route | `api-gateway/`: `ApiGatewayApplication.java` còn thông báo bản quyền. Nhóm thêm route `/api/v1`, mỗi route một Circuit Breaker, Correlation ID, xác minh token | Giữ khung, viết thêm |
| Mẫu API Composition (`/api/gateway/owners/{id}`: chủ nuôi + lượt khám, vẫn trả khi một nguồn lỗi) | `api-gateway/.../composite/CompositeController.java`: `my-appointments`, `doctor-agenda` (việc P4-6) | Học cách làm, viết mới |
| Maven Wrapper, `LICENSE` | `mvnw`, `mvnw.cmd`, `.mvn/`, `LICENSE` | Chép nguyên |
| Docker Compose có healthcheck, container sau chỉ bật khi container trước đã khỏe (`depends_on … service_healthy`) | `docker-compose.yml` | Giữ ý, viết lại |
| Cách chia service theo vai trò: customers / vets / visits | Patient / Provider / Scheduling Service | Chỉ giữ cách chia; nghiệp vụ, bảng, API viết mới |

Không dùng: Config Server, chatbot AI, bộ giám sát (Zipkin, Prometheus, Grafana), giao diện AngularJS, database HSQLDB, nghiệp vụ thú y. Petclinic không có, nhóm tự xây: đăng nhập Keycloak, RabbitMQ, tính slot và giữ chỗ, tác vụ định kỳ, MPI, tách database và tài khoản MySQL cho từng service.

Tự kiểm: `git grep -l "Copyright 2002"` → đúng 2 file `DiscoveryServerApplication.java`, `ApiGatewayApplication.java`.

### 7.5. Tài liệu tham chiếu

| Nguồn | Nội dung |
|---|---|
| Đề tài và tiêu chí chấm (`detai.pdf`) | Mục 7.1 |
| Giáo trình `LectureNotesSOA-new.pdf` | Mục 7.3 (số trang ghi trong bảng) |
| Luật Căn cước số 26/2023/QH15, hiệu lực 01/07/2024 | Tuổi bắt buộc làm thẻ căn cước (14 tuổi) — tình huống 3 |
| Spring Petclinic Microservices (Apache-2.0), commit `aefaf7f` | Mã nguồn nền của hạ tầng |

| Repo [MPI-Master-Patient-Index](https://github.com/nhugray/MPI-Master-Patient-Index), commit `6362a13` | Nguồn của phần chuẩn hóa tên, tính điểm giống nhau, tìm kiếm hồ sơ trong Patient Service và nền giao diện `frontend/` |
