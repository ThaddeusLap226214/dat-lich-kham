# ANGULAR CODING RULES — Chuẩn đi làm thực tế

> Đây không phải lý thuyết, đây là những rule các công ty thật sự áp dụng qua code review. Vi phạm rule nào là bị reviewer bắt bẻ ngay.

---

## 1. QUY TẮC ĐẶT TÊN (Naming Convention)

### File & Folder

```
✅ ĐÚNG                              ❌ SAI
patient-list.component.ts            PatientList.component.ts
patient-list.component.html          patientList.Component.ts
patient.service.ts                   PatientService.ts
patient-search-params.model.ts       patientSearchParams.ts
```

Rule: **kebab-case** cho tên file, luôn có suffix mô tả loại (`.component.ts`, `.service.ts`, `.model.ts`, `.guard.ts`, `.pipe.ts`, `.directive.ts`).

### Class, Interface, Component

```typescript
// ✅ Class/Interface/Component: PascalCase
export class PatientListComponent {}
export interface PatientSearchParams {}
export class PatientService {}

// ❌ Không viết
export class patientListComponent {}
export class Patient_Service {}
```

### Biến, thuộc tính, hàm

```typescript
// ✅ camelCase, tên có nghĩa, không viết tắt vô nghĩa
patientList: Patient[] = [];
isLoading: boolean = false;
totalElements: number = 0;

loadPatients(): void {}
onSubmitForm(): void {}

// ❌ Sai
PL: Patient[] = [];        // viết tắt vô nghĩa
data: any = [];            // tên không rõ nghĩa là data gì
flag: boolean = false;     // "flag" không nói lên nó check cái gì
tmp2: Patient[] = [];      // tên tạm bợ, đây là dấu hiệu code bẩn kinh điển
```

### Selector Component

```typescript
// ✅ Luôn có prefix riêng của project (tránh trùng tên với thư viện khác)
@Component({ selector: 'app-patient-list' })      // prefix "app" (mặc định CLI)
@Component({ selector: 'hms-patient-list' })       // hoặc prefix riêng công ty, ví dụ Hospital Management System

// ❌ Không đặt selector trùng tên HTML tag có sẵn hoặc quá chung chung
@Component({ selector: 'list' })
@Component({ selector: 'button' })
```

### Boolean — luôn có tiền tố is/has/can/should

```typescript
// ✅
isLoading, isValid, hasError, canEdit, shouldRefresh

// ❌
loading, valid, error, edit  // không rõ đây là boolean hay object/hàm
```

### Observable — luôn hậu tố `$`

```typescript
// ✅
patients$: Observable<Patient[]>;
loading$ = new BehaviorSubject<boolean>(false);

// ❌ - nhìn vào không biết đây là Observable hay giá trị thường, dễ quên subscribe
patients: Observable<Patient[]>;
```

---

## 2. CẤU TRÚC THƯ MỤC — RULE BẮT BUỘC

### Nguyên tắc: **Feature-based**, không phải **Type-based**

```
❌ SAI (Type-based — chia theo loại file, dự án lớn sẽ hỗn loạn)
src/app/
├── components/
│   ├── patient-list.component.ts
│   ├── doctor-list.component.ts
│   ├── appointment-list.component.ts
├── services/
│   ├── patient.service.ts
│   ├── doctor.service.ts
└── models/
    ├── patient.model.ts
    ├── doctor.model.ts
```

```
✅ ĐÚNG (Feature-based — chia theo nghiệp vụ, dễ maintain, dễ lazy-load, dễ xóa cả feature)
src/app/
├── core/                    # Chỉ chứa thứ dùng 1 lần toàn app (singleton)
├── shared/                  # Component/pipe/directive dùng LẶP LẠI ở nhiều feature
└── features/
    ├── patient/
    │   ├── models/
    │   ├── services/
    │   ├── components/
    │   └── patient.routes.ts
    ├── doctor/
    └── appointment/
```

**Rule kiểm tra:** nếu bạn cần xóa tính năng "Patient" khỏi app, bạn chỉ cần xóa 1 folder `features/patient/`. Nếu xóa xong mà app vẫn còn sót file liên quan rải rác ở `services/`, `models/` khác → cấu trúc bạn đang sai.

### `core/` chỉ chứa gì?

- Service singleton dùng 1 lần (AuthService, TokenStorage)
- Interceptor, Guard
- **KHÔNG BAO GIỜ** import `CoreModule` 2 lần ở nhiều nơi — chỉ import ở `AppModule` duy nhất

### `shared/` chỉ chứa gì?

- Component/pipe/directive **không chứa business logic riêng của 1 feature**, dùng lại được ở ≥ 2 nơi (PaginationComponent, ConfirmDialogComponent, ngày-tháng pipe...)
- Rule: nếu component có gọi thẳng `PatientService` bên trong → nó KHÔNG thuộc `shared/`, nó là component của feature `patient`.

---

## 3. RULE VỀ COMPONENT

### Một component chỉ nên làm MỘT việc (Single Responsibility)

```typescript
// ❌ SAI - component vừa gọi API, vừa validate, vừa xử lý export Excel, vừa show toast
export class PatientListComponent {
  loadPatients() { /* gọi API */ }
  validatePhone(phone: string) { /* validate thủ công */ }
  exportToExcel() { /* xử lý export */ }
  showToast(msg: string) { /* tự viết logic toast */ }
}
```

```typescript
// ✅ ĐÚNG - tách riêng, component chỉ điều phối (orchestrate)
export class PatientListComponent {
  constructor(
    private patientService: PatientService,      // gọi API
    private exportService: ExportService,          // xử lý export
    private toastService: ToastService              // xử lý thông báo
  ) {}

  loadPatients(): void {
    this.patientService.search(this.params).subscribe(res => this.patients = res.content);
  }
}
```

**Rule đo lường thực tế:** component `.ts` quá **300 dòng** là dấu hiệu phải tách nhỏ. Nếu 1 component có > 5 method xử lý logic nghiệp vụ (không phải UI) → đẩy logic đó xuống Service.

### Không xử lý logic phức tạp trong Template

```html
<!-- ❌ SAI - logic tính toán nằm trong template, khó test, khó đọc -->
<p>{{ (patient.age >= 18 && patient.gender === 'MALE') ? 'Nam trưởng thành' : 'Khác' }}</p>

<!-- ✅ ĐÚNG - đẩy vào getter hoặc method trong .ts -->
<p>{{ patientLabel }}</p>
```

```typescript
get patientLabel(): string {
  return this.patient.age >= 18 && this.patient.gender === 'MALE' ? 'Nam trưởng thành' : 'Khác';
}
```

### Luôn dùng `OnPush` Change Detection cho component thực tế

```typescript
@Component({
  selector: 'app-patient-card',
  changeDetection: ChangeDetectionStrategy.OnPush   // BẮT BUỘC cho dự án chuẩn production
})
```

Rule đi kèm: khi dùng `OnPush`, KHÔNG được mutate object/array trực tiếp — luôn tạo instance mới.

```typescript
// ❌ SAI - với OnPush, mutate thế này Angular sẽ KHÔNG re-render
this.patients.push(newPatient);

// ✅ ĐÚNG - luôn tạo mảng/object mới
this.patients = [...this.patients, newPatient];
```

---

## 4. RULE VỀ SERVICE

### Service KHÔNG được biết về UI

```typescript
// ❌ SAI - Service không được inject Router, không được biết về logic điều hướng/UI
@Injectable({ providedIn: 'root' })
export class PatientService {
  constructor(private http: HttpClient, private router: Router) {}

  delete(id: number) {
    this.http.delete(`...`).subscribe(() => {
      this.router.navigate(['/patients']); // ❌ Service không nên điều hướng
      alert('Xóa thành công');              // ❌ Service không nên biết UI hiển thị gì
    });
  }
}
```

```typescript
// ✅ ĐÚNG - Service chỉ trả Observable, để component tự quyết định làm gì tiếp theo
@Injectable({ providedIn: 'root' })
export class PatientService {
  constructor(private http: HttpClient) {}

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

// Component xử lý điều hướng/UI
this.patientService.delete(id).subscribe(() => {
  this.router.navigate(['/patients']);
  this.toastService.success('Xóa thành công');
});
```

### Luôn định nghĩa kiểu trả về rõ ràng, không dùng `any`

```typescript
// ❌ SAI
search(params: any): Observable<any> {}

// ✅ ĐÚNG
search(params: PatientSearchParams): Observable<PageResponse<Patient>> {}
```

**Rule tuyệt đối:** nếu bạn dùng `any` quá 2 lần trong 1 file, dừng lại — đó là dấu hiệu bạn đang né việc định nghĩa type đúng. `any` chỉ chấp nhận được khi làm việc với thư viện third-party không có type, và phải comment giải thích rõ vì sao.

---

## 5. RULE VỀ RXJS — TRÁNH MEMORY LEAK

### Luôn unsubscribe

```typescript
// ✅ Cách 1: dùng async pipe trong template (Angular tự lo)
<div>{{ (patients$ | async)?.length }}</div>

// ✅ Cách 2: dùng takeUntilDestroyed (Angular 16+, cách hiện đại nhất)
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export class PatientListComponent {
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.patientService.search(this.params).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(res => this.patients = res.content);
  }
}

// ✅ Cách 3 (cũ hơn nhưng vẫn dùng nhiều): Subject destroy$
private destroy$ = new Subject<void>();

ngOnInit(): void {
  this.patientService.search(this.params).pipe(
    takeUntil(this.destroy$)
  ).subscribe(res => this.patients = res.content);
}

ngOnDestroy(): void {
  this.destroy$.next();
  this.destroy$.complete();
}
```

```typescript
// ❌ SAI TUYỆT ĐỐI - subscribe trực tiếp trong component mà không cleanup
// Nếu user rời trang trước khi API trả về, callback vẫn chạy trên component đã hủy → memory leak, bug khó tìm
ngOnInit(): void {
  this.patientService.search(this.params).subscribe(res => this.patients = res.content);
}
```

**Rule đo lường:** MỌI `.subscribe()` viết trong component (không phải service) đều phải có cơ chế cleanup đi kèm, trừ khi Observable đó tự complete ngay lập tức (ví dụ 1 request HTTP đơn — vẫn nên cleanup cho an toàn và nhất quán).

### Không nest subscribe (callback hell kiểu RxJS)

```typescript
// ❌ SAI - subscribe lồng subscribe, y hệt callback hell
this.patientService.getById(id).subscribe(patient => {
  this.doctorService.getById(patient.doctorId).subscribe(doctor => {
    this.roomService.getById(doctor.roomId).subscribe(room => {
      // ...
    });
  });
});
```

```typescript
// ✅ ĐÚNG - dùng switchMap để nối chuỗi
this.patientService.getById(id).pipe(
  switchMap(patient => this.doctorService.getById(patient.doctorId)),
  switchMap(doctor => this.roomService.getById(doctor.roomId))
).subscribe(room => {
  // ...
});

// Hoặc nếu cần gọi song song và cần dữ liệu cả 3
forkJoin({
  patient: this.patientService.getById(id),
  doctor: this.doctorService.getById(doctorId),
  room: this.roomService.getById(roomId)
}).subscribe(({ patient, doctor, room }) => { /* ... */ });
```

---

## 6. RULE VỀ TYPESCRIPT / STRICT MODE

### Bật `strict: true` trong `tsconfig.json` — BẮT BUỘC cho dự án chuẩn

```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true
  }
}
```

Rule: nếu dự án tắt `strict` để "code cho nhanh", đó chính là nợ kỹ thuật (technical debt) sẽ trả giá về sau bằng bug runtime khó debug.

### Không dùng `!` (non-null assertion) bừa bãi

```typescript
// ❌ SAI - ép TypeScript tin là không null, nhưng runtime vẫn có thể null → crash
const name = patient!.fullName;

// ✅ ĐÚNG - check thật sự hoặc dùng optional chaining
const name = patient?.fullName ?? 'Không rõ';

if (patient) {
  const name = patient.fullName; // TypeScript tự hiểu patient chắc chắn tồn tại ở đây
}
```

`!` chỉ chấp nhận được ở field có `@Input()` bắt buộc (`@Input() patient!: Patient;`) vì Angular đảm bảo binding trước `ngOnInit`, hoặc ở nơi bạn code chắc chắn 100% qua kiểm tra logic ngay phía trên.

---

## 7. RULE FORM

- Luôn dùng **Reactive Forms**, không dùng Template-driven Forms cho dự án thật (khó test, khó validate phức tạp)
- Luôn tách interface riêng cho form value, không dùng `any`
- Custom validator tách file riêng trong `shared/validators/`, không viết inline lặp lại nhiều nơi

```typescript
// ✅ Tách riêng, tái sử dụng được
// shared/validators/phone.validator.ts
export function phoneValidator(control: AbstractControl): ValidationErrors | null {
  const valid = /^[0-9]{10}$/.test(control.value);
  return valid ? null : { invalidPhone: true };
}
```

---

## 8. RULE ĐẶT TÊN COMMIT & CODE REVIEW (áp dụng thực tế đi làm)

```
✅ Commit message chuẩn (Conventional Commits - hầu hết công ty dùng)
feat(patient): thêm chức năng search theo tên
fix(auth): sửa lỗi refresh token bị gọi lặp lại
refactor(patient-service): tách logic search ra riêng
chore(deps): update Angular lên v18

❌ Commit bẩn
"fix bug"
"update"
"asdasd"
"code lại tí"
```

### Checklist tự review trước khi tạo Pull Request

- [ ] Không còn `console.log` sót lại
- [ ] Không còn code comment lại (dead code) — xóa hẳn, git đã lưu lịch sử rồi
- [ ] Không có `any` không giải thích được
- [ ] Mọi subscribe đều có cleanup
- [ ] Tên biến/hàm đọc hiểu ngay không cần đoán
- [ ] Component không vượt quá ~300 dòng, nếu vượt → cân nhắc tách nhỏ
- [ ] Đã chạy `ng lint` sạch, không warning

---

## 9. LINT & FORMAT — Công cụ bắt buộc phải có

```bash
# ESLint cho Angular (thay TSLint đã deprecated)
ng add @angular-eslint/schematics

# Prettier - format code tự động, tránh cãi nhau về style trong team
npm install --save-dev prettier
```

```json
// .prettierrc — cấu hình chuẩn hay dùng
{
  "singleQuote": true,
  "printWidth": 100,
  "trailingComma": "none",
  "tabWidth": 2
}
```

**Rule:** cấu hình **Husky + lint-staged** để tự động chạy lint/format trước mỗi commit — không cho phép code bẩn lọt vào repo dù người review có bỏ sót.

```bash
npm install --save-dev husky lint-staged
npx husky init
```

```json
// package.json
"lint-staged": {
  "*.ts": ["eslint --fix", "prettier --write"],
  "*.html": ["prettier --write"]
}
```

---

## 10. TỔNG KẾT — 10 RULE KHÔNG BAO GIỜ ĐƯỢC VI PHẠM

1. **Không** dùng `any` nếu không giải thích được vì sao
2. **Không** subscribe mà không có cơ chế cleanup (unsubscribe/takeUntilDestroyed/async pipe)
3. **Không** để Service biết về UI (Router, alert, toast)
4. **Không** mutate array/object trực tiếp khi dùng `OnPush`
5. **Không** viết logic nghiệp vụ phức tạp trong Template
6. **Không** đặt tên biến/hàm mơ hồ (`data`, `flag`, `tmp`, `handleClick2`)
7. **Không** để component vượt quá ~300 dòng mà không tách nhỏ
8. **Không** nest subscribe lồng nhau — dùng `switchMap`/`forkJoin`
9. **Không** commit code còn `console.log`, code chết (dead code), hoặc TSLint/ESLint warning
10. **Không** tổ chức thư mục theo loại file (Type-based) — luôn theo **Feature-based**

> Nắm chắc 10 rule này, code của bạn sẽ pass code review ở hầu hết công ty làm Angular chuyên nghiệp. Nếu muốn, mình có thể review lại code bạn đã làm ở các bài tập trước theo đúng checklist này.


cấu trúc thư mục tổng thể tương tự cho các features khác
src/
├── app/
│   ├── core/                          # Singleton services, chỉ import 1 lần
│   │   ├── auth/
│   │   │   ├── auth.service.ts
│   │   │   ├── auth.guard.ts
│   │   │   ├── auth.interceptor.ts
│   │   │   └── token-storage.service.ts
│   │   ├── interceptors/
│   │   │   └── error.interceptor.ts
│   │   └── models/
│   │       ├── user.model.ts
│   │       └── pagination.model.ts
│   │
│   ├── shared/                        # Component/pipe/directive dùng chung
│   │   ├── components/
│   │   │   ├── pagination/
│   │   │   │   ├── pagination.component.ts
│   │   │   │   ├── pagination.component.html
│   │   │   │   └── pagination.component.scss
│   │   │   └── confirm-dialog/
│   │   │       ├── confirm-dialog.component.ts
│   │   │       ├── confirm-dialog.component.html
│   │   │       └── confirm-dialog.component.scss
│   │   ├── pipes/
│   │   └── shared.module.ts
│   │
│   ├── features/
│   │   └── patient/                   # Module tính năng Patient
│   │       ├── models/
│   │       │   ├── patient.model.ts
│   │       │   └── patient-search-params.model.ts
│   │       ├── services/
│   │       │   └── patient.service.ts
│   │       ├── components/
│   │       │   ├── patient-list/
│   │       │   │   ├── patient-list.component.ts
│   │       │   │   ├── patient-list.component.html
│   │       │   │   └── patient-list.component.scss
│   │       │   ├── patient-form/
│   │       │   │   ├── patient-form.component.ts
│   │       │   │   ├── patient-form.component.html
│   │       │   │   └── patient-form.component.scss
│   │       │   ├── patient-detail/
│   │       │   │   ├── patient-detail.component.ts
│   │       │   │   ├── patient-detail.component.html
│   │       │   │   └── patient-detail.component.scss
│   │       │   └── patient-search/
│   │       │       ├── patient-search.component.ts
│   │       │       ├── patient-search.component.html
│   │       │       └── patient-search.component.scss
│   │       ├── patient-routing.module.ts
│   │       └── patient.module.ts
│   │
│   ├── layout/
│   │   ├── header/
│   │   ├── sidebar/
│   │   └── layout.component.ts
│   │
│   ├── app-routing.module.ts
│   ├── app.component.ts
│   └── app.module.ts
│
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
└── assets/