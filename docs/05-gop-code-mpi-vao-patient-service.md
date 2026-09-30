# 05. Báo cáo gộp mã nguồn MPI vào Patient Service

Tài liệu ghi lại việc đưa mã nguồn của repo MPI (Master Patient Index — chỉ mục bệnh nhân chính) vào hệ thống: phần nào đã gộp và thay đổi ra sao, phần nào không gộp và lý do, đối chiếu dữ liệu cũ – mới, và những nội dung Patient Service cần triển khai tiếp.

| Mục | Giá trị |
|---|---|
| Nguồn | https://github.com/nhugray/MPI-Master-Patient-Index, nhánh `feature/import-patient` |
| Mốc lấy mã nguồn | Commit `6362a13` "update feature import patient" (28/09/2026 10:47) |
| Ngày gộp | 28/09/2026 |
| Phụ trách tiếp | P1 — Patient Service (Như) |

Từ mốc trên, việc phát triển tiếp diễn ra trên repo hệ thống. Các commit đẩy lên repo MPI sau mốc không tự có trong hệ thống. Repo MPI vẫn giữ nguyên trên GitHub; các phần "không gộp" dưới đây lấy lại được khi cần.

## Mục lục theo nhu cầu

| Nhu cầu | Đọc mục |
|---|---|
| Code MPI nào đã có trong hệ thống | [1. Backend đã gộp](#1-backend--phần-đã-gộp-vào-patient-service) · [4. Frontend](#4-frontend) · [4.1. Trang Hồ sơ bệnh nhân](#41-trang-hồ-sơ-bệnh-nhân-sau-khi-sửa-theo-hợp-đồng) |
| Code MPI nào bị bỏ và lý do | [2. Backend không gộp](#2-backend--phần-không-gộp) |
| Cột dữ liệu cũ ứng với cột mới nào | [3. Đối chiếu dữ liệu cũ → mới](#3-đối-chiếu-dữ-liệu-cũ--mới) |
| P1 còn phải làm gì | [5. Nội dung cần triển khai tiếp](#5-nội-dung-patient-service-cần-triển-khai-tiếp) |

---

## 1. Backend — phần đã gộp vào `patient-service`

Mã nguồn được **viết lại theo bảng dữ liệu mới** (`docs/03` mục 3.3), giữ nguyên thuật toán.

| Mã nguồn cũ (repo MPI) | Vị trí mới | Thay đổi |
|---|---|---|
| `PatientMatchingServiceImpl.normalizeVietnamese` | `patient/NameNormalizer.java` | Giữ cách chuẩn hóa (bỏ dấu, thay chữ Đ, viết thường, gộp khoảng trắng). Dùng để tính cột `normalized_name` |
| `PatientMatchingServiceImpl.calculateMatchScore`, `calculateStringSimilarity` | `duplicate/MatchScorer.java` | Giữ trọng số 40 / 25 / 20 / 10 / 5 (giấy tờ / họ tên / ngày sinh / điện thoại / giới tính). "Trùng CCCD" thành "trùng loại + số giấy tờ". Sửa lỗi tên rỗng được 0,85 điểm (chuỗi rỗng luôn "nằm trong" chuỗi khác) |
| `PatientMatchingServiceImpl.getAutoDecision` (≥ 85 điểm → `AUTO_APPROVED`) | **Bỏ** | Quy tắc của hệ thống: không bao giờ tự gộp. Điểm chỉ ghi vào `duplicate_candidates.score` để lễ tân tham khảo |
| `PatientSpecification` | `patient/PatientSpecification.java` | Nhiều ô lọc gộp thành **một ô `keyword`** khớp họ tên (có dấu hoặc không dấu), số giấy tờ, số điện thoại; thêm lọc theo `status` |
| `PatientServiceImpl.search` | `PatientService.search` | Trả `PageResponse` thay cho `ResultPagination`; sắp xếp mới nhất trước; `size` tối đa 100 |
| `PatientServiceImpl.getById` | `PatientService.get` | Thêm kiểm tra quyền trên bản ghi: tài khoản `PATIENT` chỉ xem hồ sơ mình quản lý (403) — giáo trình trang 43, mục 1.14.1 |
| `PatientServiceImpl.update` | `PatientService.update` | Không đổi giấy tờ ở đây (đổi giấy tờ qua `POST /patients/{id}/identity` để áp quy tắc chống trùng); hồ sơ `MERGED` không sửa được (409 `PATIENT_MERGED`); tự cập nhật `normalized_name` |

**Kiểm chứng:**

| Kiểm tra | Kết quả |
|---|---|
| 8 unit test (`patient-service/src/test/`): chuẩn hóa tên, tính điểm | Đạt |
| `GET /api/v1/patients` qua Gateway | 200, đủ 3 hồ sơ mẫu |
| Tìm `le thi hoa` và `Lê Thị` | Cùng trả hồ sơ 1 và 3 |
| Tìm theo một phần số CCCD `0481900` | Hồ sơ 1 |
| Lọc `status=TEMPORARY` | Hồ sơ 2 và 3 |
| Tài khoản bệnh nhân 5 xem hồ sơ 3 (không thuộc mình) | 403 |
| Sửa hồ sơ 2 kèm CCCD mới | 200; tên được chuẩn hóa; CCCD bị bỏ qua đúng thiết kế; `version` tăng |

## 2. Backend — phần không gộp

| Phần | Lý do |
|---|---|
| `PatientServiceImpl.create` (tạo theo hệ thống nguồn + mã bệnh nhân cục bộ) | Hồ sơ được tạo theo **bảng tình huống 1–3** (`docs/02` mục 4.1); không có hệ thống nguồn. Viết mới trong `PatientService.create` |
| `PatientServiceImpl.delete` | Không xóa hồ sơ; hồ sơ trùng được **gộp** (`MERGED`) để giữ lịch sử và lịch hẹn |
| `PatientMaster`, `PatientMasterServiceImpl` (hồ sơ tổng hợp hai tầng, mã `EMPI-…`) | Không có nhiều nguồn nên không cần tầng tổng hợp; thay bằng `status = MERGED` + `merged_into_id` trên bảng `patients` |
| `Facility`, `SourceSystem` (controller, service, specification) | Ngoài phạm vi: hệ thống phục vụ một phòng khám |
| `ImportJob`, `ImportJobDetail`, `ImportController`, các service đọc / kiểm tra / lưu file (phần lớn chưa hoàn thiện ở repo MPI) | Ngoài phạm vi: không nhập dữ liệu từ bệnh viện khác |
| Entity `User` | Tài khoản thuộc Auth Service (bảng `accounts`); Patient chỉ lưu mã tài khoản (`owner_account_id`, `reviewed_by_account_id`) — Identity Provider tập trung (giáo trình trang 40–41) |
| `ApiResponse`, `ResultPagination` (bọc kết quả trong `{statusCode, message, data}`) | Hệ thống trả thẳng dữ liệu, lỗi theo dạng `ApiError` thống nhất cho mọi service (`docs/06` mục A3–A4) |
| `application.yaml` | Mỗi service dùng cấu hình của hệ thống; mật khẩu lấy từ biến môi trường |
| Các file ghi chú `.md` ở thư mục gốc repo MPI và `patient-import-generated.html` | Ghi chú cho thiết kế MPI cũ, không còn khớp thiết kế hiện tại |

Đưa lại một phần (ví dụ nhập file) vào hệ thống đòi hỏi thêm bảng và API vào `docs/03`, `docs/06`, nên cần thống nhất trước.

## 3. Đối chiếu dữ liệu cũ → mới

| Bảng cũ `patient` | Bảng mới `patients` |
|---|---|
| `full_name` | `full_name` + `normalized_name` (tự tính) |
| `date_of_birth`, `gender`, `address` | Giữ nguyên |
| `phone_number` | `phone` |
| `national_id` | `id_type = 'CCCD'` + `id_number` |
| `health_insurance_no` | `id_type = 'BHYT'` + `id_number` (mỗi hồ sơ **một** giấy tờ) |
| `source_system_id`, `local_patient_code` | Bỏ |
| `master_patient_id` | `merged_into_id` — nghĩa khác: trỏ tới **hồ sơ được giữ lại** khi gộp |
| `match_status` (`PENDING`, `MATCHED`, `NEW_MASTER`, `REJECTED`) | `status` (`TEMPORARY`, `ACTIVE`, `MERGED`) |
| — | Mới: `owner_account_id`, `created_by_account_id`, người giám hộ (`guardian_*`), `email`, `version` |

| Bảng cũ `match_candidate` | Bảng mới `duplicate_candidates` |
|---|---|
| `patient_id` → `candidate_master_id` (hồ sơ tổng hợp) | `patient_id` → `candidate_patient_id` (hồ sơ thường) |
| `match_score`, `score_breakdown` | `score` |
| `decision` (`PENDING`, `AUTO_APPROVED`, `MANUAL_APPROVED`, `REJECTED`) | `status` (`PENDING`, `MERGED`, `NOT_DUPLICATE`) + `reason` (tình huống 2, 3 hoặc 5) |
| `reviewed_by` (khóa ngoại tới `users`) | `reviewed_by_account_id` (mã tài khoản, không khóa ngoại) |

## 4. Frontend

Toàn bộ thư mục `fe/` (Angular 18.2) được chép vào `frontend/` làm nền cho web của hệ thống. Các thay đổi đã làm trên nền này:

| Thay đổi | Lý do |
|---|---|
| Thêm khung chung: `core/` (vai trò thử, interceptor gắn header danh tính, danh sách trang theo phần việc), trang chủ, trang khung; menu và thanh đầu trang theo vai trò | Web phục vụ cả 5 phần việc và 5 vai trò, không riêng MPI |
| 5 trang `dashboard`, `patient-master`, `patient-import`, `facility`, `source-system` **ẩn khỏi menu**, giữ nguyên mã và route | Không có API tương ứng trong hệ thống hiện tại |
| Tạo lại `package-lock.json` (`npm install --package-lock-only`) | File khóa của repo MPI lệch `package.json`, lệnh `npm ci` khi build Docker bị dừng. Phiên bản Angular giữ nguyên 18.2.14 |
| Thêm `Dockerfile`, `nginx.conf`; tiêu đề trang "Đặt lịch khám đa khoa" | Web chạy trong Docker Compose ở cổng 4200 |

Tình trạng các trang MPI so với hợp đồng API hiện tại:

| Tính năng (`src/app/features/`) | Tình trạng |
|---|---|
| `patient` (trang *Hồ sơ bệnh nhân*, còn trong menu của lễ tân và bác sĩ) | **Đã sửa theo hợp đồng mới** (mục 4.1): hiện danh sách, tìm kiếm, lọc trạng thái, sửa hồ sơ. Nút *Thêm bệnh nhân mới* báo 501 tới khi P1-2 xong |
| `patient-master` | Không có API `/patient-masters`; service gọi đường dẫn tương đối `/api/patient-masters` không qua `environment.apiUrl`. Có thể chuyển thành trang hàng chờ duyệt |
| `facility`, `source-system`, `patient-import`, `dashboard` | Không có API tương ứng |
| `shared` (phân trang, hộp xác nhận, thông báo, kiểm tra số điện thoại) | Dùng lại được cho mọi trang |

### 4.1. Trang *Hồ sơ bệnh nhân* sau khi sửa theo hợp đồng

Trước khi sửa, trang gọi đúng `/api/v1/patients` nhưng đọc sai dạng dữ liệu trả về (chờ kết quả bọc trong `ApiResponse.data`, trong khi Patient Service trả thẳng `PageResponse`), gửi tham số lọc cũ (`fullName`, `nationalId`, `matchStatus`…) và dùng các trường cũ (`sourceSystemId`, `localPatientCode`, `nationalId`, `matchStatus`), nên luôn hiển thị "Không tìm thấy bệnh nhân nào". Việc sửa chỉ nằm trong `frontend/src/app/features/patient/`, giữ bố cục của giao diện MPI (tiêu đề, 3 thẻ thống kê, thanh lọc, bảng, phân trang, hộp thoại form).

| File | Thay đổi |
|---|---|
| `models/patient.model.ts` | `Patient` theo `PatientResponse` (giấy tờ `idType` + `idNumber`, người giám hộ, `status`, `mergedIntoId`); `PatientRequest` dùng chung cho tạo và sửa; `CreatePatientResponse` (`outcome`, `duplicateQueued`); `SpringPage<T>` (dạng trang backend trả về, `page` đếm từ 0) |
| `models/patient.constants.ts` | Nhãn trạng thái `TEMPORARY` (Hồ sơ tạm), `ACTIVE` (Đã xác minh), `MERGED` (Đã gộp); nhãn loại giấy tờ |
| `models/patient-search-params.model.ts` | Tham số tìm kiếm còn `keyword`, `status`, `page`, `size` — đúng các tham số API nhận |
| `services/patient.service.ts` | Đọc dữ liệu trả thẳng; sửa bằng `PUT /api/v1/patients/{id}`; bỏ `delete` |
| `components/patient-list/*` | Cột mới: giấy tờ, người giám hộ, trạng thái (hồ sơ đã gộp hiện "→ ID-hồ sơ chính", nút sửa bị khóa); bộ lọc trạng thái có mục "Tất cả"; bỏ bộ lọc giới tính và hệ thống nguồn (API không có tham số tương ứng); bỏ nút xóa. Ô tìm kiếm gửi `keyword` — tìm được họ tên có dấu hoặc không dấu, số giấy tờ, số điện thoại |
| Thẻ thống kê | *Tổng hồ sơ*, *Đã xác minh*, *Hồ sơ tạm*. Mỗi thẻ gọi API một lần với `size=1` và đọc `totalElements`, nên con số tính trên toàn bộ dữ liệu. Mã cũ đếm trên trang đang xem nên sai khi có nhiều trang |
| `components/patient-form/*` | Trường mới: loại + số giấy tờ, email, người giám hộ (tên, quan hệ, số điện thoại); ngày sinh bắt buộc (khớp `@NotNull @Past` ở backend). Khi sửa, hai ô giấy tờ bị khóa vì backend bỏ qua giấy tờ trong `PUT` (đổi giấy tờ phải qua luồng bổ sung giấy tờ, P1-3). Khi tạo, thông báo theo `outcome` (dùng lại hồ sơ cũ / tạo hồ sơ đã xác minh / tạo hồ sơ tạm) |
| `pages/patients-page.*` | Bỏ hộp thoại xác nhận xóa. Hệ thống không xóa hồ sơ bệnh nhân; hồ sơ trùng được gộp (`docs/02` mục 4.1) |
| `components/patient-filter-panel/*` | Không trang nào dùng; chỉ đổi sang trạng thái mới để không còn tham chiếu kiểu cũ |

Kết quả khi chạy với dữ liệu mẫu (vai trò lễ tân): bảng hiện đủ 10 hồ sơ; thẻ thống kê 10 / 5 / 4; tìm "le thi hoa" ra 2 hồ sơ (Lê Thị Hoa, Le Thi Hoa); tìm "048058" ra 2 hồ sơ (7, 8); lọc *Hồ sơ tạm* ra hồ sơ 2, 3, 5, 8; sửa hồ sơ 2 thành công (200); sửa hồ sơ 10 (đã gộp) bị từ chối 409 kèm thông điệp; thêm mới báo 501 "Chưa làm: tạo hồ sơ…".

## 5. Nội dung Patient Service cần triển khai tiếp

Chi tiết từng việc, tiêu chí hoàn thành và mục giáo trình: `docs/04` mục 3.

| Việc | Nội dung |
|---|---|
| P1-1 | `listMine`, `findSummaries` — API nội bộ mà Gateway và Reminder đang chờ |
| P1-2, P1-3 | Tạo hồ sơ theo tình huống 1–3 (dùng `NameNormalizer`, ghi `score` bằng `MatchScorer`), bổ sung giấy tờ theo tình huống 4–5 |
| P1-4 | Hàng chờ duyệt: danh sách, đếm, xác nhận không trùng |
| P1-5, P1-6 | Gộp / tách hồ sơ, lịch sử, phát sự kiện RabbitMQ sau khi commit (Saga Choreography, giao dịch bù trừ) |
| P1-7 | Trang *Hồ sơ bệnh nhân* đã sửa theo hợp đồng mới (mục 4.1); còn trang *Hồ sơ của tôi*, *Hàng chờ duyệt*; quyết định giữ hay bỏ mã của 5 trang MPI đang ẩn |
