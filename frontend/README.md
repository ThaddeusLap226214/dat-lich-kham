# Frontend (Angular 18)

Web của hệ thống đặt lịch khám, dựng trên nền giao diện của repo MPI (commit `6362a13`, `docs/05` mục 4).

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `src/app/core/` | Danh sách trang theo phần việc (`app-pages.ts`), vai trò thử và interceptor gắn header danh tính, dịch vụ gọi thử API |
| `src/app/features/home/` | Trang chủ: tình trạng các thành phần, lối vào các trang theo vai trò |
| `src/app/features/skeleton/` | Trang khung dùng chung cho các trang chưa làm |
| `src/app/features/patient/` | Trang hồ sơ bệnh nhân (từ repo MPI) |
| `src/app/features/{dashboard,patient-master,patient-import,facility,source-system}/` | Trang MPI cũ, ẩn khỏi menu |
| `src/app/layout/` | Khung trang: menu bên trái theo vai trò, thanh đầu trang có ô Chế độ thử |

Mỗi trang mới thay trang khung tương ứng: tạo component trong `src/app/features/<tên>/`, khai báo route trong `app.routes.ts` và đặt `implemented: true` cho trang đó trong `core/app-pages.ts`.

## Chạy

Trong Docker Compose: `docker compose up -d --build` → http://localhost:4200.

Chế độ phát triển (tự tải lại khi sửa code, cần Node.js 22 trở lên):

```powershell
docker compose stop frontend
cd frontend
npm install
npm start          # http://localhost:4200
```

## Quy ước

- Web chỉ gọi API qua Gateway: `environment.apiUrl = http://localhost:8080/api/v1`.
- Backend trả thẳng dữ liệu, không bọc `{statusCode, message, data}`; lỗi có dạng `ApiError` với trường `error` là mã lỗi (`docs/06` mục A3–A4).
- Việc cần làm của từng trang: `docs/04`.
