# 01. Hướng dẫn cài đặt và chạy hệ thống

Tài liệu mô tả các yêu cầu về phần cứng, phần mềm và các bước để khởi chạy toàn bộ hệ thống đặt lịch khám trên một máy cá nhân, kết nối vào cơ sở dữ liệu và chạy thử các chức năng đang có. Toàn bộ hệ thống (gateway, 5 service, cơ sở dữ liệu, message broker, web) được đóng gói bằng **Docker Compose** (công cụ khởi chạy nhiều container từ một file cấu hình), nên máy chạy không cần cài Java, Node.js hay MySQL.

Các số liệu thời gian và bộ nhớ trong tài liệu được đo trên máy Windows 11, 16 GB RAM, CPU 20 luồng, ngày 28/09/2026.

## Mục lục theo nhu cầu

| Nhu cầu | Đọc mục |
|---|---|
| Cài đặt và chạy lần đầu | [1. Yêu cầu](#1-yêu-cầu) · [2. Chuẩn bị máy](#2-chuẩn-bị-máy-làm-một-lần) · [3. Khởi chạy hệ thống](#3-khởi-chạy-hệ-thống) |
| Kiểm tra hệ thống chạy được | [4. Địa chỉ truy cập](#4-địa-chỉ-truy-cập) · [5. Chạy thử](#5-chạy-thử) |
| Viết code | [6. Kết nối cơ sở dữ liệu](#6-kết-nối-cơ-sở-dữ-liệu) · [7. Làm việc khi phát triển](#7-làm-việc-khi-phát-triển) |
| Tắt hệ thống, gặp lỗi | [8. Dừng hệ thống](#8-dừng-hệ-thống) · [9. Xử lý sự cố](#9-xử-lý-sự-cố) |

---

## 1. Yêu cầu

### 1.1. Phần cứng

| Thành phần | Tối thiểu | Ghi chú |
|---|---|---|
| RAM | 16 GB | Toàn hệ thống khi chạy dùng khoảng **2,2 GB** trong Docker; lúc build dùng thêm khoảng 1–1,5 GB. Phần còn lại dành cho Windows, IDE, trình duyệt |
| Ổ đĩa trống | 6 GB | Các image nền (Maven, JDK, MySQL, RabbitMQ, Node, nginx) khoảng 2,5 GB, image của hệ thống khoảng 1,5 GB |
| Mạng | Có Internet ở lần chạy đầu | Tải image và thư viện; các lần sau chạy không cần mạng |

### 1.2. Phần mềm

| Phần mềm | Bắt buộc khi | Phiên bản đã kiểm thử | Kiểm tra |
|---|---|---|---|
| **Docker Desktop** (kèm WSL 2 trên Windows) | Luôn luôn | 29.8 | `docker version` |
| **Git** | Luôn luôn | — | `git --version` |
| JDK | Sửa và build backend ngoài Docker | Temurin 25 | `java -version` |
| Node.js | Sửa và chạy frontend ngoài Docker | 22 trở lên (đã thử 24.14) | `node --version` |
| Công cụ xem CSDL (DBeaver, MySQL Workbench) | Xem dữ liệu | — | — |

### 1.3. Công nghệ bên trong hệ thống

| Lớp | Công nghệ | Vai trò |
|---|---|---|
| Ngôn ngữ, khung backend | Java 25, Spring Boot 4.0.7 | 5 service nghiệp vụ |
| Hạ tầng microservices | Spring Cloud 2025.1.3: Gateway, Netflix Eureka, LoadBalancer, Circuit Breaker Resilience4j | Cổng vào, danh bạ service, gọi nhau theo tên, chống lỗi lan truyền |
| Cơ sở dữ liệu | MySQL 8.4 | Một máy chủ, 5 cơ sở dữ liệu tách biệt |
| Message broker (hệ thống trung gian chuyển tin nhắn) | RabbitMQ 4 | Sự kiện gộp / tách hồ sơ bệnh nhân |
| Frontend | Angular 18.2, phục vụ bằng nginx | Web cho 5 vai trò người dùng |
| Hộp thư giả lập | Mailpit | Nhận email nhắc lịch khi chạy thử |
| Tài liệu API | springdoc-openapi (Swagger UI) | Trang thử API của từng service |

---

## 2. Chuẩn bị máy (làm một lần)

### 2.1. Cài Docker Desktop

Tải tại docker.com, cài với tùy chọn **Use WSL 2**. Sau khi cài, mở Docker Desktop và chờ góc dưới trái hiện *Engine running*.

> **Lưu ý:** nếu Docker Desktop không chạy được và báo thiếu hoặc cần cập nhật WSL 2 (Windows Subsystem for Linux — phần chạy Linux bên trong Windows), mở PowerShell bằng quyền Administrator, chạy `wsl --install` (hoặc `wsl --update`) rồi khởi động lại máy. Nếu báo *Virtualization support not detected*, bật ảo hóa trong BIOS (mục *Intel VT-x* hoặc *AMD SVM*).

### 2.2. Giới hạn RAM cho Docker (Windows)

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

### 2.3. Chọn thư mục đặt mã nguồn

Đặt repo ở **đường dẫn không dấu tiếng Việt**, ví dụ `D:\SOA\dat-lich-kham`. Windows mặc định dùng bảng mã cp1252, không biểu diễn được các chữ như "ế, ư, ớ"; Java đọc sai đường dẫn có các chữ này và lệnh `mvnw` báo lỗi `ClassNotFoundException: MavenWrapperMain`.

---

## 3. Khởi chạy hệ thống

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

**Máy đã từng chạy hệ thống trước khi có Keycloak** phải xóa dữ liệu cũ **một lần**, vì script tạo database (`docker/mysql/init`) chỉ chạy khi volume MySQL còn trống:

```powershell
docker compose down -v
docker compose up -d --build
```

Lệnh `down -v` xóa dữ liệu thử trên máy mình; dữ liệu mẫu tự nạp lại khi bật.

### Các lần chạy sau

Mở Docker Desktop, rồi chạy trong thư mục dự án:

| Việc | Lệnh |
|---|---|
| Bật hệ thống | `docker compose up -d` |
| Tắt hệ thống (giữ dữ liệu) | `docker compose down` |

Sau khi `git pull` code mới, bật bằng `docker compose up -d --build` để build lại. Lệnh `up -d` không build lại nên vẫn chạy code cũ. Muốn tắt và xóa sạch dữ liệu thì xem mục 8.

---

## 4. Địa chỉ truy cập

| Địa chỉ | Thành phần | Dùng để |
|---|---|---|
| **http://localhost:4200** | Web | Giao diện người dùng |
| http://localhost:8080 | API Gateway | Cổng vào duy nhất của mọi API (`/api/v1/...`) |
| http://localhost:8180/admin | Keycloak (quản trị) | Xem người dùng, vai trò của realm `dat-lich-kham`; khóa tài khoản, đổi vai trò, đặt lại mật khẩu. Tài khoản `admin` / `admin_pw` (đổi trong `.env`) |
| http://localhost:8180/realms/dat-lich-kham/account | Keycloak (tài khoản cá nhân) | Đăng nhập thử bằng tài khoản mẫu, ví dụ `benhnhan` / `123456` |
| http://localhost:8761 | Eureka | Xem các service đã đăng ký vào danh bạ |
| http://localhost:15672 | RabbitMQ Management | Xem exchange, hàng đợi, tin nhắn. Tài khoản `clinic` / `clinic_pw` |
| http://localhost:8025 | Mailpit | Xem email nhắc lịch đã gửi |
| `localhost:3307` | MySQL | Kết nối bằng công cụ xem CSDL (mục 6) |

---

## 5. Chạy thử

### 5.1. Trang chủ — tình trạng các thành phần

Mở http://localhost:4200. Trang chủ gọi thật một API của mỗi thành phần qua Gateway và hiển thị kết quả:

| Kết quả | Ý nghĩa |
|---|---|
| **200 — đã chạy** | Phần xử lý đã viết, service trả dữ liệu thật |
| **501 — chưa làm** | Service đang chạy, request đi đúng đường qua Gateway, phần xử lý bên trong chưa viết (xem `docs/04`) |
| **503 / không phản hồi** | Service chưa bật xong hoặc đã tắt |

Ở thời điểm bàn giao khung: Patient Service trả 200 (phần tìm kiếm hồ sơ đã chuyển từ code MPI), các thành phần còn lại trả 501.

### 5.2. Chế độ thử (chọn vai trò)

Keycloak đã chạy và giữ 11 tài khoản mẫu, nhưng web chưa chuyển sang trang đăng nhập Keycloak và Gateway chưa xác minh token (việc P4-4, P4-9). Trong lúc đó, ô **Chế độ thử** trên thanh đầu trang cho phép chọn một trong các tài khoản mẫu. Web tự gắn header `X-User-Id`, `X-User-Role`, `X-Doctor-Id` vào mọi request — đúng các header mà Gateway sẽ gắn sau khi xác minh token (xem `docs/06`, mục A5). Menu bên trái thay đổi theo vai trò.

Mọi vai trò đều thấy *Trang chủ* và *Chuyên khoa & bác sĩ*; ngoài ra:

| Vai trò | Tài khoản mẫu | Trang riêng trong menu |
|---|---|---|
| Khách | — | Đăng nhập, Đăng ký |
| Bệnh nhân | `benhnhan` | Đặt lịch, Lịch khám của tôi, Hồ sơ bệnh nhân của tôi |
| Lễ tân | `letan` | Đặt lịch, Hồ sơ bệnh nhân, Hàng chờ duyệt hồ sơ trùng, Lịch hẹn theo bác sĩ |
| Bác sĩ | `bs.an` | Hồ sơ bệnh nhân, Lịch khám trong ngày |
| Quản trị | `admin` | Chuyên khoa – bác sĩ – phòng, Lịch làm việc, Tài khoản, Nhật ký nhắc lịch |

Mật khẩu chung của các tài khoản mẫu là `123456` (lưu ở Keycloak). Mỗi trang chưa hoàn thiện hiển thị phần việc phụ trách, danh sách việc cần làm, API chính và nút **Gọi thử** để xem phản hồi thật của backend.

### 5.3. Swagger — thử API trực tiếp

Chạy ở chế độ phát triển (mục 7.1), sau đó mở trang Swagger của từng service:

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

---

## 6. Kết nối cơ sở dữ liệu

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

Các mật khẩu trên là giá trị mặc định chỉ dùng khi chạy trên máy cá nhân; có thể đổi bằng file `.env` (mẫu ở `.env.example`). Cấu trúc bảng và dữ liệu mẫu: `docs/03`.

---

## 7. Làm việc khi phát triển

### 7.1. Chế độ phát triển

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

File `docker-compose.dev.yml` mở thêm cổng riêng của 5 service (8081–8085) để thử API trên Swagger mà không qua Gateway. Khi chạy chính thức (demo, nộp bài) chỉ dùng `docker-compose.yml`, các service không mở cổng ra ngoài.

### 7.2. Các lệnh thường dùng

| Việc | Lệnh |
|---|---|
| Build lại và chạy lại một service sau khi sửa code | `docker compose up -d --build scheduling-service` |
| Xem log trực tiếp của một service | `docker compose logs -f scheduling-service` |
| Build thử backend trên máy (cần JDK 25) | `.\mvnw.cmd -pl scheduling-service package -DskipTests` |
| Chạy unit test một service | `.\mvnw.cmd -pl patient-service test` |
| Tìm các chỗ còn chờ viết trong một service | `Get-ChildItem .\scheduling-service\src -Recurse -Filter *.java \| Select-String "NotImplementedYetException\("` |

### 7.3. Chạy frontend ở chế độ phát triển

Chế độ này tự tải lại trang mỗi khi sửa code. Container `frontend` đang chiếm cổng 4200 nên cần tắt trước:

```powershell
docker compose stop frontend
cd frontend
npm install
npm start          # http://localhost:4200
```

---

## 8. Dừng hệ thống

| Việc | Lệnh | Dữ liệu |
|---|---|---|
| Dừng, giữ dữ liệu | `docker compose down` | Còn nguyên trong volume `dat-lich-kham_mysql-data` |
| Dừng và xóa sạch dữ liệu | `docker compose down -v` | Lần chạy sau nạp lại dữ liệu mẫu |

---

## 9. Xử lý sự cố

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Vừa bật, gọi API nhận 503 | Service vừa khởi động, chưa đăng ký xong với Eureka | Chờ tới khi `docker compose ps` báo đủ 12 container `healthy` (khoảng 30 giây; lần đầu Keycloak lâu hơn) |
| Máy chậm, RAM Windows trên 90% | Máy ảo Docker giữ bộ nhớ đệm | Làm mục 2.2; xả ngay: `docker run --rm --privileged alpine sh -c "sync; echo 3 > /proc/sys/vm/drop_caches"` |
| `mvnw` báo `ClassNotFoundException: MavenWrapperMain` | Đường dẫn chứa chữ có dấu | Chuyển repo sang thư mục không dấu (mục 2.3) |
| `port is already allocated` | Cổng 3307, 8080, 4200… đang bị chương trình khác dùng | Tắt chương trình đó, hoặc đổi số cổng bên trái trong `ports:` của `docker-compose.yml` |
| Sửa `schema.sql` nhưng bảng không đổi | `CREATE TABLE IF NOT EXISTS` bỏ qua bảng đã có | `docker compose down -v` rồi `docker compose up -d --build` (mất dữ liệu trên máy mình) |
| Một container báo `unhealthy` hoặc tự khởi động lại liên tục | Lỗi khi khởi động service | `docker compose logs <tên-service>` và tìm dòng `ERROR` / `Caused by` |
| Build frontend báo `npm ci … not in sync` | `package-lock.json` lệch `package.json` | Trong `frontend/`: `npm install --package-lock-only`, rồi build lại |
