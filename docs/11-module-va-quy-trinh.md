# 11. Module hệ thống, quy trình nghiệp vụ và quy trình tin học hóa

Tài liệu trả lời ba câu: hệ thống có **những module nào**, **quy trình nghiệp vụ** của từng module ra sao (làm cái gì, không phụ thuộc máy tính), và **quy trình tin học hóa** tương ứng (làm bằng cách nào trên hệ thống: trang nào, API nào, kiểm tra gì, chuyển trạng thái nào).

Đọc cùng [`10-bo-chung-tu-va-hop-dong-nghiep-vu.md`](10-bo-chung-tu-va-hop-dong-nghiep-vu.md): mã `CT..` là chứng từ, mã `CS..` là chính sách đã chốt. Sơ đồ khối: [`so-do/so-do-tong-quan.png`](so-do/so-do-tong-quan.png).

## Mục lục

1. [Cách đọc — khác gì một hệ ERP](#1-cách-đọc--khác-gì-một-hệ-erp)
2. [Danh sách module](#2-danh-sách-module)
3. [Quy trình nghiệp vụ](#3-quy-trình-nghiệp-vụ)
4. [Quy trình tin học hóa](#4-quy-trình-tin-học-hóa)

---

## 1. Cách đọc — khác gì một hệ ERP

| | Quy trình nghiệp vụ | Quy trình tin học hóa |
|---|---|---|
| **Module khối** (phân hệ) | ① Luồng tổng quát: chuỗi chức năng con, nhận gì từ / giao gì cho module khác — mục 3.1 | ④ Luồng chứng từ trên hệ thống: chứng từ nào sinh chứng từ nào, trạng thái, điểm nối giữa các service — mục 4.3, 4.4 |
| **Module chi tiết** (chức năng con) | ② Từng bước: ai, làm gì, giấy vào, giấy ra, quy tắc, ngoại lệ — mục 3.2, 3.3 | ③ Từng bước trên hệ thống: trang, API, trường, kiểm tra, trạng thái, quyền, đầu ra — mục 4.2 |

Hệ thống xây theo kiến trúc microservices (mỗi dịch vụ một khối nghiệp vụ, chạy riêng), nên có ba chỗ khác một hệ ERP như ERPNext:

| Ở ERP | Ở hệ thống này |
|---|---|
| Các phân hệ đọc, ghi **một cơ sở dữ liệu chung** | Mỗi module khối là **một service giữ database riêng**. Module khác chỉ lấy dữ liệu qua API hoặc sự kiện, không đọc database của nhau (`docs/06` A6) |
| Quy trình tin học hóa là tiền đề cấu hình DocType (loại chứng từ) | Quy trình tin học hóa là tiền đề **thiết kế bảng + API + trang** của từng service. DocType ≈ bảng + API · Field ≈ cột / trường JSON · Role Permission ≈ `X-User-Role` và chặn ở Gateway · Workflow ≈ cột `status` và các API chuyển trạng thái · Link field ≈ mã tham chiếu sang service khác (không có khóa ngoại) · Server Script ≈ kiểm tra trong lớp Service · Notification ≈ Reminder, sự kiện RabbitMQ |
| Chứng từ kế tiếp lấy dữ liệu chứng từ trước trong cùng database | Chứng từ kế tiếp **chép lại** dữ liệu cần dùng (bản chụp), ví dụ lịch hẹn chép giờ và phòng từ ca làm việc. Màn hình cần dữ liệu nhiều service thì Gateway **ghép** lại (API Composition) |

---

## 2. Danh sách module

### 2.1. Module khối

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

### 2.2. Module chi tiết (chức năng con)

Mỗi chức năng con là một việc, một vai trò làm, ra một kết quả rõ. Cột UC là mã use case (ca sử dụng) cũ, để tra lại.

| Mã | Chức năng con | Vai trò làm | Kết quả | UC |
|---|---|---|---|---|
| **M1** | | | | |
| F11 | Quản lý chuyên khoa, phòng, bác sĩ | Quản lý | CT01, CT02, CT03 | UC15 |
| F12 | Lập ca làm việc của bác sĩ | Quản lý | CT04 | UC16 |
| F13 | Xem chuyên khoa, bác sĩ, ca làm việc | Khách, mọi vai trò | danh sách để chọn bác sĩ | UC03 |
| F14 | Cung cấp giờ làm việc trong ngày (nội bộ) | Hệ thống (cho M3) | ca của bác sĩ trong ngày | — |
| F15 | Cung cấp bác sĩ, phòng theo danh sách mã (nội bộ) | Hệ thống (cho Gateway) | tên bác sĩ, phòng | — |
| **M2** | | | | |
| F21 | Khai hồ sơ (bản thân, người nhà) | Bệnh nhân | CT07 | UC04 |
| F22 | Tìm, xem, sửa, tạo hồ sơ tại quầy | Lễ tân (xem: cả bác sĩ) | CT07 | UC08 |
| F23 | Bổ sung giấy tờ cho hồ sơ tạm | Lễ tân | CT09 | UC09 |
| F24 | Dò hồ sơ nghi trùng | Hệ thống (trong F21, F22, F23) | CT20 (đề nghị) | UC21 |
| F25 | Duyệt hàng chờ: gộp hoặc xác nhận không trùng | Lễ tân | CT20 (quyết định), CT21 | UC10 |
| F26 | Tách hồ sơ gộp nhầm, xem lịch sử gộp / tách | Lễ tân | CT23 | UC11 |
| F27 | Cung cấp hồ sơ rút gọn (nội bộ) | Hệ thống (cho Gateway, M4) | tên, ngày sinh, email | — |
| **M3** | | | | |
| F31 | Xem slot trống | Bệnh nhân, Lễ tân | CT10 | UC05 |
| F32 | Giữ chỗ | Bệnh nhân, Lễ tân | CT11 | UC05 |
| F33 | Xác nhận lịch | Người đã giữ chỗ | CT12 | UC05 |
| F34 | Nhả chỗ giữ quá hạn | Hệ thống (định kỳ) | CT11 → hết hạn | UC20 |
| F35 | Hủy lịch (cũng là cách đổi lịch, CS13) | Người đặt, Lễ tân | CT13 | UC06 |
| F36 | Xem lịch khám của tôi | Bệnh nhân | CT12 kèm tên (ghép ở Gateway) | UC07 |
| F37 | Xem danh sách khám trong ngày / lịch hẹn theo bác sĩ | Bác sĩ, Lễ tân | CT16 (ghép ở Gateway) | UC12, UC13 |
| F38 | Đánh dấu đã khám | Bác sĩ, Lễ tân | CT17 | UC14 |
| F39 | Ghi bệnh nhân không đến | Bác sĩ, Lễ tân | CT19 | mới |
| F3A | Chuyển lịch hẹn khi hồ sơ được gộp / tách | Hệ thống (theo sự kiện) | CT22 | UC22 |
| F3B | Cung cấp lịch hẹn (nội bộ) | Hệ thống (cho Gateway, M4) | danh sách lịch | — |
| **M4** | | | | |
| F41 | Quét và gửi nhắc lịch; thử lại thư gửi lỗi | Hệ thống (mỗi 5 phút) | CT15 | UC19 |
| F42 | Xem nhật ký thông báo, quét ngay | Quản lý (xem: cả lễ tân) | nhật ký CT14, CT15, CT24 | UC18 |
| F43 | Gửi email xác nhận khi lịch được xác nhận | Hệ thống (theo sự kiện) | CT14 | mới |
| F44 | Gửi thư báo hủy khi lịch bị hủy | Hệ thống (theo sự kiện) | CT24 | mới |
| F45 | Quản lý mẫu thư | Quản lý | CT25 | mới |
| **M5** | | | | |
| F51 | Đăng ký tài khoản bệnh nhân | Khách | CT06 | UC01 |
| F52 | Đăng nhập, đăng xuất | Mọi vai trò | token đăng nhập | UC02 |
| F53 | Cấp, khóa, mở tài khoản nhân viên | Quản lý | CT05 | UC17 |
| F54 | Xem tài khoản của tôi | Mọi vai trò | thông tin tài khoản | — |

---

## 3. Quy trình nghiệp vụ

Phần này tả **làm cái gì**, đúng cả khi không có máy tính. Bỏ máy tính đi thì bước vẫn còn; bước nào chỉ tồn tại vì có hệ thống thì nằm ở mục 4.

### 3.1. ① Luồng tổng quát của từng module khối

| Module | Chuỗi chức năng con | Nhận từ module khác | Giao cho module khác |
|---|---|---|---|
| M1 Danh mục | F11 lập danh mục → F12 lập ca → F13 công bố cho khách | — | Giờ làm việc → M3 · tên bác sĩ, phòng → màn hình ghép |
| M2 Hồ sơ | F21 / F22 khai hồ sơ → F24 dò trùng → (F23 bổ sung giấy tờ) → F25 duyệt trùng → (F26 tách nếu gộp nhầm) | Tài khoản quản lý hồ sơ ← M5 | Hồ sơ rút gọn → M4, màn hình ghép · việc gộp / tách → M3 |
| M3 Hẹn khám | F31 xem slot → F32 giữ chỗ → F33 xác nhận → (F35 hủy) → ngày khám: F37 xem danh sách → F38 đã khám hoặc F39 không đến | Giờ làm việc ← M1 · hồ sơ được chọn ← M2 · việc gộp / tách ← M2 | Lịch sắp tới, việc xác nhận / hủy → M4 · lịch hẹn → màn hình ghép |
| M4 Thông báo | F45 soạn mẫu thư (làm trước, ít đổi) → khi lịch được xác nhận: F43 gửi thư xác nhận · khi lịch bị hủy: F44 gửi thư báo hủy · trước giờ khám 24 giờ: F41 gửi thư nhắc → F42 xem nhật ký | Lịch sắp tới, việc xác nhận / hủy ← M3 · email ← M2 | — |
| M5 Tài khoản | F51 khách tự đăng ký / F53 quản lý cấp cho nhân viên → F52 đăng nhập | Mã bác sĩ ← M1 (quản lý chọn) | Mã tài khoản, vai trò cho mọi module |

### 3.2. Quy trình xuyên module

Hai quy trình chạy qua nhiều module, dùng làm luồng demo chính.

**Q1 — Từ đăng ký đến khám xong** (khớp kịch bản KB2 ở `docs/10` mục 7)

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

### 3.3. ② Từng bước của các chức năng con chính

Chức năng chỉ thêm / sửa danh mục (F11, F13, F22 phần tìm và sửa, F42, F45, F54) không tả từng bước: một người nhập, hệ thống lưu, không có nhánh nghiệp vụ.

**F12 — Lập ca làm việc**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Quản lý | Chọn bác sĩ, phòng, thứ trong tuần, giờ bắt đầu – kết thúc, thời lượng một lượt, ngày áp dụng | CT03, CT02 → CT04 | Một bác sĩ không có hai ca chồng giờ trong cùng ngày | Chồng giờ → không lập được |
| 2 | Quản lý | Sửa hoặc ngừng ca khi cần | CT04 → CT04 | Lịch đã hẹn giữ nguyên (CS16) | Ngày lễ, nghỉ đột xuất: ngoài phạm vi (CS17) |

**F21, F22, F24 — Khai hồ sơ và dò trùng**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Bệnh nhân (hoặc lễ tân tại quầy) | Khai họ tên, ngày sinh, giới tính, liên hệ, giấy tờ nếu có, người giám hộ | CT08 → CT07 | Dưới 14 tuổi phải có người giám hộ (CS19); số điện thoại chuẩn hóa (CS20) | Không có giấy tờ → hồ sơ tạm |
| 2 | Phòng khám | Đối chiếu với hồ sơ có sẵn | CT07 → CT07 / CT20 | Bảng tình huống `docs/06` B3: cùng giấy tờ và cùng tên, ngày sinh → dùng lại hồ sơ cũ; cùng giấy tờ mà khác thông tin, hoặc không giấy tờ mà trùng tên + ngày sinh + SĐT → hồ sơ tạm và đưa vào hàng chờ | **Không bao giờ tự gộp** |

**F23 — Bổ sung giấy tờ**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Lễ tân | Nhập loại và số giấy tờ bệnh nhân mang tới cho hồ sơ tạm | CT07 tạm, CT08 → CT09 | Chỉ hồ sơ tạm; một hồ sơ một cặp giấy tờ (CS27) | Giấy tờ đã thuộc hồ sơ khác → vẫn tạm, đưa vào hàng chờ |

**F25, F26 — Duyệt trùng, gộp, tách**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Lễ tân | Mở cặp nghi trùng, so sánh hai hồ sơ | CT20 | Điểm giống nhau chỉ để tham khảo | — |
| 2a | Lễ tân | Kết luận cùng người: chọn hồ sơ giữ lại, gộp | CT20 → CT21 | Chỉ lễ tân (CS25); giữ nguyên thông tin hồ sơ giữ lại (CS23) | Cặp đã duyệt → không duyệt lại |
| 2b | Lễ tân | Kết luận khác người | CT20 → CT20 (không trùng) | | |
| 3 | Phòng khám | Lịch hẹn của hồ sơ bị gộp chuyển sang hồ sơ giữ lại | CT21 → CT22 | Chuyển mọi lịch (CS14) | |
| 4 | Lễ tân | Phát hiện gộp nhầm: tách | CT21 → CT23 | Hồ sơ bị gộp về đúng trạng thái trước khi gộp; cặp thành "không trùng" (CS24) | |

**F31–F33 — Đặt lịch**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Bệnh nhân / Lễ tân | Chọn bác sĩ, ngày; xem giờ còn trống | CT04 → CT10 | Đặt trước tối đa 30 ngày (CS05); bệnh nhân không đặt khi còn dưới 2 giờ (CS06) | Bác sĩ không có ca hôm đó → không có giờ |
| 2 | Bệnh nhân / Lễ tân | Chọn giờ, chọn hồ sơ, ghi lý do khám → giữ chỗ | CT10, CT07 → CT11 | Một giờ của một bác sĩ chỉ một người giữ; một hồ sơ không có hai lịch chồng giờ (liền kề thì được, CS12); tối đa 3 chỗ đang giữ mỗi tài khoản (CS03), 5 lịch sắp tới mỗi hồ sơ (CS04) | Người khác giữ trước → chọn giờ khác |
| 3 | Bệnh nhân / Lễ tân | Xác nhận trong thời gian giữ chỗ | CT11 → CT12 | Giữ chỗ 5 phút (CS01, CS02) | Quá hạn → chỗ được nhả, phải giữ lại |

**F35 — Hủy lịch**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Người đặt / Lễ tân | Báo hủy, ghi lý do | CT12 → CT13 | Bệnh nhân hủy khi còn từ 2 giờ trở lên; lễ tân không bị giới hạn (CS07); bác sĩ không hủy (CS08) | Muốn đổi giờ → hủy rồi đặt lại (CS13) |

**F37–F39 — Ngày khám**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Bác sĩ / Lễ tân | Xem danh sách bệnh nhân hẹn trong ngày | CT12 → CT16 | Bác sĩ chỉ xem lịch của mình | — |
| 2a | Bác sĩ (hoặc lễ tân) | Khám xong, đánh dấu | CT16 → CT17 | Từ giờ bắt đầu trở đi (CS09) | — |
| 2b | Bác sĩ (hoặc lễ tân) | Bệnh nhân không đến, ghi nhận | CT16 → CT19 | Sau giờ bắt đầu (CS10) | Không phạt (CS10) |

**F41 — Nhắc lịch**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Phòng khám | Lọc các lịch đã xác nhận trong 24 giờ tới, chưa nhắc | CT12 | 24 giờ (CS28); đặt sát giờ thì nhắc ngay (CS29) | — |
| 2 | Phòng khám | Gửi thư nhắc tới email của hồ sơ, ghi lại đã nhắc | CT07 (email), CT25 → CT15 | Nội dung theo mẫu (CS32); mỗi lịch nhắc một lần | Hồ sơ không email → ghi "bỏ qua" (CS30); gửi lỗi → thử lại lần sau (CS36) |

**F43, F44 — Thư xác nhận, thư báo hủy**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Phòng khám | Lịch vừa được xác nhận → gửi thư xác nhận tới email của hồ sơ | CT12, CT07, CT25 → CT14 | Mỗi lịch một thư (CS36); đặt dưới 24 giờ vẫn nhận thêm thư nhắc (CS35) | Hồ sơ không email → "bỏ qua" |
| 2 | Phòng khám | Lịch vừa bị hủy, ai hủy cũng vậy → gửi thư báo hủy kèm lý do | CT13, CT07, CT25 → CT24 | CS31 | Hết hạn giữ chỗ, đã khám, không đến → không gửi |

**F51, F53 — Cấp tài khoản**

| Bước | Ai | Làm gì | Giấy vào → ra | Quy tắc | Ngoại lệ |
|---|---|---|---|---|---|
| 1 | Khách | Tự đăng ký, thành tài khoản bệnh nhân | — → CT06 | Tên đăng nhập, email không trùng | — |
| 2 | Quản lý | Cấp tài khoản cho lễ tân, bác sĩ, quản lý kèm mật khẩu ban đầu | CT03 → CT05 | Tài khoản bác sĩ phải gắn mã bác sĩ; tạo bác sĩ trước (CS33) | Nhân viên nghỉ → khóa tài khoản (CS34) |

---

## 4. Quy trình tin học hóa

Phần này tả **làm bằng cách nào** trên hệ thống. Bảng ở mục 4.2 là đặc tả để code: đọc cùng `docs/06` (hình dạng request / response) và `docs/03` (cột).

### 4.1. Bước nghiệp vụ thay đổi thế nào khi lên hệ thống

| Trường hợp | Bước |
|---|---|
| **Giữ nguyên** (chỉ đổi giấy sang máy) | khai hồ sơ, lập ca, lập danh mục, hủy lịch, đánh dấu đã khám, ghi không đến |
| **Máy làm thay** | lật sổ tìm giờ trống → bảng slot tự tính (CT10) · gọi điện xác nhận, nhắc, báo hủy → thư tự gửi (CT14, CT15, CT24) · chép sổ hẹn ra danh sách in → danh sách tự ghép (CT16) · dò hồ sơ trùng bằng mắt → máy đề nghị, người quyết (CT20) · gạch lịch hẹn khi hồ sơ trùng → tự chuyển lịch (CT22) |
| **Bỏ** | ghi tay sổ hẹn; gọi lại phòng khám hỏi lịch của mình (bệnh nhân tự xem F36) |
| **Sinh thêm bước mới** | đăng ký, đăng nhập, phân quyền theo vai trò (M5, Gateway) · giữ chỗ 5 phút trước khi xác nhận (chống hai người cùng đặt một giờ) · nhả chỗ quá hạn (F34) · phát sự kiện gộp / tách hồ sơ và sự kiện đã xác nhận / đã hủy lịch giữa các service · soạn mẫu thư (F45) |

### 4.2. ③ Từng chức năng con trên hệ thống

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
| F24 | — (chạy trong F21, F22, F23) | — | — | bảng tình huống `docs/06` B3; tính `score` (`MatchScorer`) chỉ để tham khảo | cặp `PENDING` | Hệ thống | CT20 |
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
| F38 | *Lịch khám trong ngày* (nút "Đã khám"), *Lịch hẹn theo bác sĩ* | `POST /appointments/{id}/complete` | — | bác sĩ khác → 403; trước giờ bắt đầu → 409 `TOO_EARLY_TO_COMPLETE` (CS09); không phải `CONFIRMED` → 409 `INVALID_STATUS` | `COMPLETED` | DOCTOR (lịch của mình), RECEPTIONIST | CT17 |
| F39 | *Lịch khám trong ngày* (nút "Không đến"), *Lịch hẹn theo bác sĩ* | `POST /appointments/{id}/no-show` (**mới**) | — | như F38; trước giờ bắt đầu → 409 `TOO_EARLY_FOR_NO_SHOW` (CS10) | `NO_SHOW` | DOCTOR (lịch của mình), RECEPTIONIST | CT19 |
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

### 4.3. ④ Luồng chứng từ và trạng thái trong từng module

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

### 4.4. ④ Điểm nối giữa các service

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
