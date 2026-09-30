# Màn hình Nhập Dữ liệu Bệnh nhân (Patient Import)

## Tổng quan

Màn hình này cho phép nhập hàng loạt dữ liệu bệnh nhân từ file CSV/Excel vào hệ thống MPI (Master Patient Index).

## Các tính năng đã triển khai

### 1. **Upload File**
- ✅ Drag & Drop file CSV/Excel
- ✅ Browse file từ máy tính
- ✅ Validate file type (CSV, XLSX, XLS)
- ✅ Validate file size (tối đa 500MB)
- ✅ Hiển thị tiến trình upload

### 2. **File Validation**
- ✅ Tự động phát hiện cột
- ✅ Validate dữ liệu theo schema
- ✅ Hiển thị lỗi validation
- ✅ Hiển thị số lượng dòng và cột

### 3. **Configuration Panel**
- ✅ Chọn hệ thống nguồn (Source System)
- ✅ Tự động ánh xạ cột (Auto column mapping)
- ✅ Tùy chọn xử lý trùng lặp tự động
- ✅ Điều chỉnh ngưỡng trùng lặp

### 4. **Active Jobs Queue**
- ✅ Hiển thị danh sách công việc đang chạy
- ✅ Real-time progress tracking
- ✅ Tự động poll status mỗi 5 giây
- ✅ Hiển thị lịch sử import

### 5. **UI/UX**
- ✅ Material Design 3
- ✅ Responsive design (mobile & desktop)
- ✅ Dark mode support (theo system preference)
- ✅ Animations & transitions mượt mà
- ✅ Loading states
- ✅ Empty states

## Cấu trúc thư mục

```
fe/src/app/features/patient/
├── models/
│   └── import.model.ts              # Type definitions
├── pages/
│   ├── patient-import-page.component.ts
│   ├── patient-import-page.component.html
│   └── patient-import-page.component.css
└── services/
    └── import.service.ts            # API service
```

## API Endpoints được sử dụng

### 1. Upload File
```
POST /api/v1/imports/upload
Content-Type: multipart/form-data

Body:
- file: File
- sourceSystemId: number

Response: FileValidationResponse
```

### 2. Preview Import Data
```
GET /api/v1/imports/{jobId}/preview?page=0&size=10

Response: Page<PreviewRow>
```

### 3. Start Import
```
POST /api/v1/imports/start

Body: StartImportRequest {
  sourceSystemId: number
  fileToken: string
  columnMappings: Record<string, string>
  skipDuplicates: boolean
  duplicateThreshold: number
}

Response: ImportJob
```

### 4. Get Import Job Status
```
GET /api/v1/imports/{jobId}

Response: ImportJob
```

### 5. Search Import Jobs
```
GET /api/v1/imports?page=0&size=10

Response: Page<ImportJob>
```

## Models

### FileValidationResponse
```typescript
{
  fileToken: string           // Token để bắt đầu import
  fileName: string           // Tên file
  fileSize: number           // Kích thước file (bytes)
  rowCount: number           // Số dòng
  columnCount: number        // Số cột
  detectedColumns: string[]  // Danh sách cột phát hiện được
  validationErrors: ValidationError[]
  isValid: boolean           // File có hợp lệ không
}
```

### ImportJob
```typescript
{
  id: number
  fileName: string
  sourceSystemId: number
  status: 'PENDING' | 'VALIDATING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  totalRows: number
  processedRows: number
  successCount: number
  errorCount: number
  warningCount: number
  skipCount: number
  startedAt: string
  completedAt?: string
}
```

## Routing

Màn hình có thể truy cập qua:
- URL: `/patient-import`
- Sidebar menu: "Nhập dữ liệu"

## Cách sử dụng

### 1. Chọn hệ thống nguồn
- Chọn hệ thống nguồn từ dropdown "Hệ thống Nguồn"

### 2. Upload file
- **Cách 1**: Kéo và thả file vào vùng upload
- **Cách 2**: Click vào vùng upload và chọn file

### 3. Xác thực file
- Hệ thống sẽ tự động validate file
- Hiển thị các cột được phát hiện
- Hiển thị lỗi nếu có

### 4. Bắt đầu import
- Click nút "Bắt đầu Nhập dữ liệu"
- Theo dõi tiến trình trong "Hàng đợi Đang xử lý"

## Column Mapping (Auto-detect)

Hệ thống tự động ánh xạ các cột dựa trên tên cột:

| Tên cột trong file | Ánh xạ đến field |
|-------------------|------------------|
| name, tên | fullName |
| dob, birth, sinh | dateOfBirth |
| gender, sex, giới | gender |
| phone, điện | phoneNumber |
| national, cccd, cmnd | nationalId |
| insurance, bhyt | healthInsuranceNo |
| address, địa | address |
| code, mrn, mã | localPatientCode |

## Yêu cầu Backend

### 1. File Upload Endpoint cần hỗ trợ:
- Multipart form data
- Xác thực file type
- Xác thực file size
- Trả về jobId trong FileValidationResponse (để load preview)

### 2. Websocket/SSE (tùy chọn):
Để có real-time updates tốt hơn, có thể implement:
- Websocket để push status updates
- Server-Sent Events cho progress tracking

## Cải tiến trong tương lai

### Phase 2:
- [ ] Preview data table với pagination
- [ ] Manual column mapping interface
- [ ] Duplicate resolution UI
- [ ] Export error report
- [ ] Batch cancel/retry operations

### Phase 3:
- [ ] Template management (save/load mapping templates)
- [ ] Scheduled imports
- [ ] Email notifications
- [ ] Advanced filtering trong preview

## Testing

### Manual Testing Checklist:
- [ ] Upload CSV file nhỏ (< 1MB)
- [ ] Upload Excel file (XLSX)
- [ ] Upload file lớn (> 100MB)
- [ ] Upload file không đúng định dạng
- [ ] Kéo thả file
- [ ] Cancel file sau khi upload
- [ ] Start import với file hợp lệ
- [ ] Theo dõi progress của multiple jobs
- [ ] Refresh page khi có job đang chạy

### Browser Testing:
- [ ] Chrome
- [ ] Firefox
- [ ] Safari
- [ ] Edge

### Responsive Testing:
- [ ] Mobile (< 768px)
- [ ] Tablet (768px - 1024px)
- [ ] Desktop (> 1024px)

## Troubleshooting

### File upload fails
- Kiểm tra file size < 500MB
- Kiểm tra file type (CSV, XLSX, XLS)
- Kiểm tra network connection

### Import không bắt đầu
- Kiểm tra đã chọn source system
- Kiểm tra file đã được validate thành công
- Kiểm tra backend logs

### Progress không cập nhật
- Kiểm tra polling có hoạt động (mỗi 5 giây)
- Kiểm tra API `/imports/{jobId}` có trả về status đúng
- Mở DevTools Network tab để debug

## Dependencies

```json
{
  "@angular/common": "^17.x",
  "@angular/core": "^17.x",
  "@angular/forms": "^17.x",
  "rxjs": "^7.x"
}
```

## Browser Support

- Chrome/Edge: Latest 2 versions
- Firefox: Latest 2 versions
- Safari: Latest 2 versions

## Performance

- File upload: Chunked upload cho file > 50MB
- Polling interval: 5 seconds (có thể điều chỉnh)
- Lazy loading: Component được lazy load
- CSS: Scoped styles, không ảnh hưởng global

## Security Considerations

- File type validation (client & server side)
- File size limit: 500MB
- CSRF protection (Angular HttpClient built-in)
- XSS prevention (Angular sanitization)
- No sensitive data in URL params

## Contact

Nếu có vấn đề hoặc câu hỏi, vui lòng liên hệ team development.
