# 09. Nguồn gốc mã nguồn: hệ thống dựng trên Spring Petclinic Microservices

Phần này trình bày hệ thống đặt lịch khám **lấy gì** từ dự án mã nguồn mở Spring Petclinic Microservices, **giống** và **khác** ở đâu, đã **bỏ** những gì và vì sao. Mọi điểm đều đối chiếu được trong mã nguồn (cột "Kiểm ở đâu").

## 1. Petclinic là gì

| Mục | Nội dung |
|---|---|
| Dự án | [spring-petclinic-microservices](https://github.com/spring-petclinic/spring-petclinic-microservices), dự án mẫu mã nguồn mở do **cộng đồng** Spring Petclinic duy trì, phát triển từ ứng dụng mẫu **chính thức** [spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic) (bản một khối), minh họa kiến trúc microservices bằng Spring Cloud |
| Độ phổ biến (GitHub, 19/10/2026) | 2.223 sao, 2.977 fork, 41 người đóng góp, có từ 11/2016, cập nhật gần nhất 07/10/2026. Là bản demo để học kiến trúc, không phải sản phẩm vận hành thật |
| Bản dùng | Nhánh `main`, commit `aefaf7f` (23/09/2026) |
| Giấy phép | Apache-2.0: được dùng lại và sửa đổi, với điều kiện giữ thông báo bản quyền (repo giữ file `LICENSE` và đầu hai file Java có nguồn gốc Petclinic) |
| Nghiệp vụ | Phòng khám **thú y**: chủ nuôi, thú cưng, bác sĩ thú y, lượt khám |
| Kiến trúc | 3 service nghiệp vụ (customers, vets, visits) + chatbot AI, đứng sau API Gateway; hạ tầng Spring Cloud: Config Server, Eureka, bộ giám sát |
| Công nghệ | Java 17, Spring Boot 4.0.1, Spring Cloud 2025.1.0; giao diện AngularJS 1.8 nằm **bên trong** Gateway; database HSQLDB trong bộ nhớ |

**Vì sao chọn Petclinic làm nền:** Hướng 4 của đề tài yêu cầu API Gateway, các service độc lập và Docker Compose (`detai.pdf` trang 2–3). Petclinic có sẵn đúng **bộ khung kiến trúc** đó và đã được cộng đồng kiểm chứng. Nhóm giữ khung, thay toàn bộ nghiệp vụ.

## 2. Tóm tắt một hình

```
                 Petclinic                                 Hệ thống đặt lịch khám
 ┌──────────────────────────────────────┐      ┌────────────────────────────────────────────┐
 │ GIỮ    Eureka (danh bạ service)       │ ───► │ discovery-server         (gần như nguyên bản)│
 │ GIỮ    khung Gateway: WebClient gọi   │ ───► │ api-gateway: + route /api/v1, + API ghép    │
 │        theo tên + Circuit Breaker     │      │   my-appointments, doctor-agenda, + JWT     │
 │ GIỮ    Maven Wrapper, LICENSE         │ ───► │ (nguyên bản)                                │
 │ GIỮ ý  compose có healthcheck         │ ───► │ docker-compose.yml viết lại theo cùng ý     │
 │ THAY   customers / vets / visits      │ ───► │ Patient / Provider / Scheduling (viết mới)  │
 │ THÊM   —                              │ ───► │ Auth, Reminder, RabbitMQ, MySQL, Keycloak   │
 │ BỎ     Config Server, chatbot AI,     │      │                                            │
 │        giám sát, giao diện AngularJS  │      │                                            │
 └──────────────────────────────────────┘      └────────────────────────────────────────────┘
```

## 3. Dùng lại những gì

| Thành phần | Dùng lại thế nào | Kiểm ở đâu |
|---|---|---|
| **Discovery Server (Eureka)** | Lấy nguyên, chỉ đổi tên gói; còn thông báo bản quyền Petclinic ở đầu file | `discovery-server/.../DiscoveryServerApplication.java` |
| **Khung API Gateway** | Giữ hai ý chính: bean `WebClient` có `@LoadBalanced` (gọi service theo tên, Eureka trả địa chỉ thật) và cấu hình Circuit Breaker Resilience4j (bộ ngắt mạch) bọc mọi route | `api-gateway/.../ApiGatewayApplication.java` (còn thông báo bản quyền) |
| **Mẫu API Composition** | Petclinic có một API ghép (`/api/gateway/owners/{id}`: chủ nuôi + lượt khám). Nhóm học cách làm đó để viết hai API ghép mới, có dự phòng khi một service lỗi | So `ApiGatewayController` của Petclinic với `api-gateway/.../composite/CompositeController.java` |
| **Maven Wrapper, `LICENSE`** | Chép nguyên | `mvnw`, `mvnw.cmd`, `.mvn/`, `LICENSE` |
| **Cách bố trí Docker Compose** | Giữ ý: mỗi container có healthcheck, container sau chỉ bật khi container trước đã khỏe (`depends_on … service_healthy`) | `docker-compose.yml` |

Phần code **chép trực tiếp** từ Petclinic nhỏ: chỉ 2 file Java còn mang bản quyền Petclinic. Năm service nghiệp vụ, toàn bộ bảng dữ liệu, API, giao diện web và tài liệu là **viết mới**. Riêng phần MPI của Patient Service lấy từ repo MPI của thành viên nhóm (`docs/05`).

## 4. Giống nhau ở đâu

Những nguyên tắc kiến trúc của Petclinic mà hệ thống giữ nguyên:

| Nguyên tắc | Petclinic | Hệ thống đặt lịch khám |
|---|---|---|
| Cổng vào duy nhất | Trình duyệt chỉ gọi Gateway | Giống |
| Gọi service theo tên qua Eureka | `lb://customers-service` | `lb://provider-service`… |
| Mỗi service một database | Có (chế độ mặc định) | Có, và chặt hơn (mục 5) |
| Lưu mã thực thể của service khác, **không khóa ngoại** | `visits.pet_id` chỉ là số | `appointments.patient_id`, `doctor_id`… chỉ là số |
| Chịu lỗi tại Gateway | Circuit Breaker + kết quả dự phòng | Giống, tách riêng từng route (đã chốt) |
| API ghép vẫn trả dữ liệu khi một nguồn lỗi | Visits lỗi → vẫn trả chủ nuôi | Provider lỗi → vẫn trả lịch hẹn, phần tên bác sĩ để trống |

Đối chiếu nghiệp vụ, theo **vai trò** chứ không phải theo code:

| Petclinic | Hệ thống | Ghi chú |
|---|---|---|
| `customers-service` (chủ nuôi, thú cưng) | Patient Service | Thay bằng hồ sơ bệnh nhân + MPI (chỉ mục bệnh nhân chính) |
| `vets-service` (bác sĩ, chuyên khoa) | Provider Service | Thêm phòng khám và ca làm việc của bác sĩ |
| `visits-service` (lượt khám) | Scheduling Service | Thêm giờ khám, slot, giữ chỗ, chống đặt trùng |

## 5. Khác nhau ở đâu

| Khía cạnh | Petclinic | Hệ thống đặt lịch khám | Vì sao đổi |
|---|---|---|---|
| Phiên bản | Java 17, Boot 4.0.1, Cloud 2025.1.0 | Java 25, Boot 4.0.7, Cloud 2025.1.3 | Bản mới nhất lúc dựng; khớp JDK nhóm dùng (`pom.xml`) |
| Database | HSQLDB trong bộ nhớ, tắt là mất dữ liệu. Chế độ MySQL thì cả 3 service **dùng chung một database**, có khóa ngoại chéo | MySQL 8.4: một container, **5 database + 5 tài khoản**, mỗi tài khoản chỉ có quyền trên database của mình | Chế độ MySQL của Petclinic vi phạm Database per Service; nhóm sửa bằng phân quyền MySQL (giáo trình trang 88) |
| Đường dẫn API | `/api/customer/**`, Gateway cắt tiền tố (`StripPrefix=2`) | `/api/v1/<tài-nguyên>`, chuyển nguyên; API nội bộ `/internal/v1` **không** có route qua Gateway | Có số phiên bản; tách rõ API công khai và nội bộ (`docs/06` A1) |
| Giao diện | AngularJS 1.8 nằm trong Gateway | Angular 18, container riêng (nginx) | Tách giao diện khỏi cổng API; AngularJS đã ngừng hỗ trợ |
| Cấu hình | Config Server đọc từ một repo GitHub khác | Mỗi service tự giữ `application.yml` + biến môi trường | Bớt một container; cấu hình nằm cùng repo, dễ sửa |
| Đóng gói | Compose kéo image dựng sẵn trên Docker Hub | `Dockerfile` build nhiều giai đoạn từ chính mã nguồn | Code nhóm sửa phải được build lại |
| Giao tiếp | Chỉ HTTP đồng bộ | HTTP + **RabbitMQ** (sự kiện gộp / tách hồ sơ, có hàng đợi tin lỗi DLQ) | Saga kiểu Choreography cho luồng gộp hồ sơ |
| Đăng nhập | Không có | Keycloak cấp JWT, Gateway xác minh và gắn danh tính (đang triển khai) | Đề tài có 4 vai trò người dùng |
| Tác vụ định kỳ | Không có | Nhả chỗ giữ quá hạn; quét nhắc lịch mỗi 5 phút | Nghiệp vụ đặt lịch cần |
| Thời gian chờ của Circuit Breaker | 10 giây; tự thử lại `POST` khi gặp 503 | 5 giây; không tự thử lại | Thử lại `POST` có thể tạo trùng (`POST` không lũy đẳng — `docs/06` A2) |

## 6. Đã bỏ gì

| Thành phần Petclinic | Lý do bỏ |
|---|---|
| Config Server | Thêm một container và một repo cấu hình riêng mà hệ thống nhỏ không cần |
| Chatbot AI (`genai-service`) | Ngoài phạm vi đề tài; cần khóa API trả phí |
| Bộ giám sát: Zipkin, admin-server, Prometheus, Grafana | Tốn RAM (máy thành viên giới hạn khoảng 5 GB cho Docker). Thay bằng cách nhẹ hơn: Correlation ID (mã truy vết) ghi vào log của Gateway và mọi service |
| Giao diện AngularJS | Thay bằng Angular 18 riêng |
| Nghiệp vụ thú y (chủ nuôi, thú cưng, loài) | Thay bằng nghiệp vụ khám bệnh |

## 7. Petclinic thiếu gì mà nhóm phải tự xây

1. **Đăng nhập, phân quyền:** Keycloak, xác minh JWT ở Gateway, header danh tính `X-User-*`.
2. **Message broker:** RabbitMQ, sự kiện gộp / tách hồ sơ, chống xử lý lặp (`processed_events`).
3. **Lập lịch thật:** slot theo ca làm việc, giữ chỗ có hạn, chống hai người đặt trùng bằng ràng buộc `UNIQUE`.
4. **Tác vụ định kỳ:** Reminder gửi email nhắc, không nhắc lặp.
5. **Định danh bệnh nhân (MPI):** phát hiện hồ sơ trùng, hàng chờ duyệt, gộp / tách.
6. **Database per Service đúng nghĩa:** tách database và tài khoản MySQL.

## 8. Tự kiểm chứng

| Muốn kiểm | Làm gì |
|---|---|
| File nào còn mang bản quyền Petclinic | `git grep -l "Copyright 2002"` trong repo → đúng 2 file `DiscoveryServerApplication.java`, `ApiGatewayApplication.java` |
| Phiên bản | Mở `pom.xml`: `spring-boot-starter-parent` 4.0.7, `java.version` 25, `spring-cloud.version` 2025.1.3 |
| Không còn Config Server, chatbot, giám sát | Danh sách thư mục gốc repo: chỉ có 7 module Java + `frontend` |
| Database tách thật | Đăng nhập MySQL bằng `provider_user`, chạy `SELECT * FROM patient_db.patients` → bị từ chối (lỗi 1142) |
| Phân tích Petclinic đầy đủ | Tài liệu tìm hiểu riêng của nhóm (`KienTruc-Petclinic.md`) |
