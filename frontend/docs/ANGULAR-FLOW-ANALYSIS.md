# Phân Tích Luồng Xử Lý Ứng Dụng Angular — Từ Khởi Động Đến Search

> Tài liệu dành cho developer đã biết Java Spring Boot, chưa quen với Angular. Mỗi phần sẽ so sánh với Spring để dễ hiểu.

---

## Mục Lục

1. [Khởi động ứng dụng — như Spring Boot](#1-khởi-động-ứng-dụng) `main()`
2. [Dependency Injection — như Spring](#2-dependency-injection) `@Autowired` [nhưng mạnh hơn](#2-dependency-injection)
3. [Signal — khái niệm không có trong Spring](#3-signal-reactivity-trong-angular)
4. [Service — như](#4-service-gọi-api) `@Service` [trong Spring](#4-service-gọi-api)
5. [Router — như Controller + DispatcherServlet](#5-router-điều-hướng)
6. [Component — như View + Controller ghép vào](#6-component-template)
7. [Change Detection — cách Angular biết khi nào render lại](#7-change-detection)
8. [Luồng search toàn bộ — từ đầu đến cuối](#8-luồng-search-toàn-bộ)
9. [So sánh Spring vs Angular nhanh](#9-so-sánh-spring-vs-angular)

---



## 1. Khởi Động Ứng Dụng



### Spring Boot

```
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        // Tomcat start, scan @Component, khởi tạo Bean...
    }
}
```



### Angular — file `main.ts`

```typescript:main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
```

**Giải thích:**

- `bootstrapApplication` tương đương `SpringApplication.run()` — nó khởi động toàn bộ ứng dụng
- Tham số 1: **root component** — component đầu tiên được render (như `@GetMapping("/")` trả về view mặc định)
- Tham số 2: **appConfig** — cấu hình toàn app (router, HTTP client, zone)

```typescript:app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withFetch())
  ]
};
```

Tương đương với `@EnableWebMvc` trong Spring — kích hoạt các module cần thiết:

- `provideRouter(routes)` → kích hoạt routing (thay thế DispatcherServlet)
- `provideHttpClient()` → kích hoạt HttpClient (thay thế RestTemplate/WebClient)

---



## 2. Dependency Injection



### Spring Boot

```java
@Service
public class PatientService {
    @Autowired  // inject bean
    private PatientRepository repository;
}
```



### Angular — `inject()` hoặc constructor

**Cách 1: Constructor injection (tương đương** `@Autowired`**)**

```typescript
export class PatientListComponent {
  constructor(
    private readonly patientService: PatientService,
    private readonly toastService: ToastService
  ) {}
}
```

**Cách 2:** `inject()` **function (Angular 14+, linh hoạt hơn)**

```typescript
export class PatientListComponent {
  private readonly patientService = inject(PatientService);
  private readonly toastService = inject(ToastService);
}
```



### Injector — hai cấp độ

```
┌─────────────────────────────────────────────┐
│  Root Injector (providedIn: 'root')          │  ← TOÀN APP CHỈ CÓ 1 INSTANCE
│  ├── SearchService                          │     Giống @Scope("singleton")
│  ├── ToastService                           │     Dùng ở đâu cũng là 1 object
│  └── PatientService                        │
├─────────────────────────────────────────────┤
│  Component Injector (per component)         │  ← MỖI COMPONENT 1 INSTANCE
│  └── Mỗi PatientListComponent mới có       │
│      instance riêng của component đó        │
└─────────────────────────────────────────────┘
```

```typescript
@Injectable({ providedIn: 'root' })  // ← singleton toàn app, như Spring Bean
export class SearchService {}

@Injectable({ providedIn: 'root' })  // ← singleton toàn app
export class PatientService {}
```

**Quan trọng:** `SearchService` là **singleton toàn app** — HeaderComponent và PatientListComponent cùng trỏ đến 1 instance. Đây là cách search từ header ảnh hưởng được list phía dưới.

---



## 3. Signal — Reactivity Trong Angular



### Signal là gì? (Không có trong Spring)

Trong Spring, bạn dùng `@Transactional`, `@Cacheable`, reactive streams (`Mono/Flux`) để quản lý trạng thái. Angular có **Signal** — một primitive mới cho reactivity.

```typescript
// Tạo signal — như tạo 1 Observable/Subject nhưng đơn giản hơn
searchQuery = signal<string>('');

// Đọc giá trị — gọi như function ()
const query = this.searchService.searchQuery(); // → string

// Ghi giá trị — gọi .set() hoặc .update()
this.searchService.searchQuery.set('Nguyễn');   // ghi đè
this.searchQuery.update(q => q + ' Văn');       // transform

// Computed — giá trị tính toán từ signal khác (như derived field)
readonly fullNameDisplay = computed(() =>
  `Bệnh nhân: ${this.name()}`
);
```

**So sánh với Spring:**

- Signal ≈ `@Cacheable` field + notification mechanism gộp lại
- Signal thay đổi → Angular **tự động** theo dõi và cập nhật UI
- Không cần `ApplicationEventPublisher` hay `@EventListener`



### Tại sao dùng Signal?

```typescript
// ❌ Cách cũ — dùng BehaviorSubject như Spring Reactor
private searchQuery$ = new BehaviorSubject<string>('');

// ✅ Cách mới — dùng Signal, code sạch hơn nhiều
searchQuery = signal<string>('');
```

---



## 4. Service Gọi API



### Spring Controller

```java
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> search(
            @RequestParam(required = false) String fullName,
            Pageable pageable) {
        // ... xử lý
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```



### Angular Service — `PatientService`

```typescript:fe/src/app/features/patient/services/patient.service.ts
@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);  // ← RestTemplate/WebClient thuần
  private readonly apiUrl = environment.apiUrl;

  search(params: PatientSearchParams): Observable<ApiResponse<PageResponse<Patient>>> {
    let httpParams = new HttpParams();

    if (params.fullName) {
      httpParams = httpParams.set('fullName', params.fullName);
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    // ...

    return this.http.get<ApiResponse<PageResponse<Patient>>>(
      `${this.apiUrl}${this.basePath}`,
      { params: httpParams }
    );
  }
}
```

**Điểm tương ứng:**


| Spring                           | Angular                      |
| -------------------------------- | ---------------------------- |
| `@RestController`                | Service class + `HttpClient` |
| `@GetMapping`                    | `this.http.get()`            |
| `@RequestParam`                  | `HttpParams.set()`           |
| `Pageable`                       | `page` + `size` params       |
| `ResponseEntity<ApiResponse<T>>` | `Observable<ApiResponse<T>>` |


**Lưu ý quan trọng về** `Observable`**:**

- `Observable` trong Angular tương đương `Mono/Flux` trong Spring WebFlux
- `http.get()` trả về `Observable` — **chưa gọi API** cho đến khi `.subscribe()`
- Giống `restTemplate.getForObject()` nhưng **không blocking**

---



## 5. Router — Điều Hướng



### Spring DispatcherServlet + Controller

```java
@GetMapping("/patients")
public String patientsPage(Model model) {
    return "patients"; // → forward sang view patients.jsp
}
```



### Angular Router — `app.routes.ts`

```typescript:fe/src/app/app.routes.ts
export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,  // layout chứa sidebar + header
    children: [
      {
        path: '',
        redirectTo: 'patients',
        pathMatch: 'full'
      },
      {
        path: 'patients',
        loadComponent: () => import('./features/patient/pages/patients-page.component')
          .then(m => m.PatientsPageComponent)
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/pages/dashboard-page.component')
          .then(m => m.DashboardPageComponent)
      }
    ]
  }
];
```

**Giải thích:**

- `path: 'patients'` → khi URL là `http://localhost:4200/patients`
- `loadComponent()` → **lazy loading** — chỉ load code khi user vào trang đó (giống `@PreAuthorize` nhưng cho route)
- `LayoutComponent` chứa `<app-sidebar>` và `<app-header>` — luôn hiển thị



### HTML của Layout

```html:layout.component.html
<div class="layout">
  <app-sidebar></app-sidebar>       <!-- Component sidebar -->
  <app-header></app-header>        <!-- Component header (chứa ô search) -->
  <main class="main-content">
    <router-outlet></router-outlet> <!--  Nơi patients/dashboard được render -->
  </main>
</div>
```

`<router-outlet>` giống `<jsp:include>` hoặc Thymeleaf `th:replace` — chỗ này sẽ render component tương ứng với URL hiện tại.

---



## 6. Component — Template



### Angular Component = Controller + View

**File** `.ts` (logic — như `@Controller`):

```typescript:patient-list.component.ts (lines 30-64)
export class PatientListComponent implements OnInit {
  // Properties (state)
  patients = signal<Patient[]>([]);
  isLoading = signal(false);
  private searchParams: PatientSearchParams = { page: 0, size: 10 };

  // Lifecycle hook — chạy khi component được tạo
  ngOnInit(): void {
    this.loadPatients();
  }

  // Method
  loadPatients(): void {
    this.isLoading.set(true);
    this.patientService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.patients.set(response.data.result);  // ← cập nhật signal
        }
      }
    });
  }
}
```

**File** `.html` (giao diện — như JSP/Thymeleaf):

```html:patient-list.component.html (lines 67-143)
<table class="patient-table">
  <tbody>
    @if (isLoading()) {
      <tr><td colspan="8">Đang tải...</td></tr>
    } @else {
      @for (patient of patients(); track patient.id) {
        <tr>
          <td>{{ patient.fullName }}</td>  <!-- {{ }} như ${} trong JSP -->
          <td>{{ patient.dateOfBirth | date:'dd/MM/yyyy' }}</td>  <!-- | date = pipe -->
        </tr>
      }
    }
  </tbody>
</table>
```

**So sánh:**


| Spring/JSP                 | Angular                  |
| -------------------------- | ------------------------ |
| `@Controller` class        | `.ts` component          |
| View file (JSP/Thymeleaf)  | `.html` template         |
| `${patient.fullName}`      | `{{ patient.fullName }}` |
| `<c:forEach>`              | `@for`                   |
| `<c:if>`                   | `@if`                    |
| JSTL pipe `fmt:formatDate` | `                        |


---



## 7. Change Detection



### Vấn đề: Angular biết khi nào cập nhật UI?

Trong Spring MVC, request → response → render xong → done. Nhưng Angular là **Single Page Application** — UI cập nhật mà không reload trang. Angular cần biết **khi nào state thay đổi** để re-render.

### Default Strategy: Check everything

Angular kiểm tra **tất cả** component sau mỗi sự kiện (click, input, setTimeout, HTTP response). Tốn performance.

### `OnPush` Strategy — Nên dùng

```typescript
@Component({
  selector: 'app-patient-list',
  changeDetection: ChangeDetectionStrategy.OnPush  // ← chỉ check khi CÓ thay đổi
})
```

**Khi nào Angular check (với OnPush)?**

1. `@Input()` thay đổi (từ component cha truyền vào)
2. **Signal** thay đổi (`.set()`, `.update()`)
3. Event handler chạy (click, keyup...)
4. Async pipe nhận value mới (`| async`)
5. Manual `markForCheck()` được gọi

**Đây là lý do dùng** `signal`**:**

```typescript
// ✅ ĐÚNG — OnPush + signal: Angular tự biết cần re-render
this.patients.set(response.data.result);

// ❌ SAI — gán thẳng: OnPush không detect được (vì tham chiếu không đổi)
this.patients = response.data.result;
```

---



## 8. Luồng Search Toàn Bộ

Dưới đây là luồng từ lúc user gõ tên vào ô search ở header → đến khi bảng bệnh nhân cập nhật kết quả.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  BƯỚC 1: User gõ "Nguyễn" vào ô search + nhấn Enter                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  HEADER COMPONENT — header.component.ts                                     │
│                                                                              │
│  HTML: <input [(ngModel)]="searchQuery" (keyup.enter)="onSearch()">        │
│         └── [(ngModel)] = two-way binding nhập → biến tự cập nhật           │
│                                                                              │
│  TS:                                                                        │
│    onSearch(): void {                                                        │
│      this.searchService.setQuery(this.searchQuery);  // "Nguyễn"           │
│    }                                                                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ searchService.setQuery("Nguyễn")
                                     │ (signal write)
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  SEARCH SERVICE — search.service.ts (SINGLETON toàn app)                    │
│                                                                              │
│    searchQuery = signal<string>('');  // ← signal value = "Nguyễn"        │
│                                                                              │
│    setQuery(query: string): void {                                           │
│      this.searchQuery.set(query);   // signal được update                   │
│    }                                                                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ signal change notification
                                     │ (SearchService là singleton → cùng object instance)
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  PATIENT LIST COMPONENT — patient-list.component.ts                         │
│                                                                              │
│  Constructor:                                                                │
│    private readonly searchService = inject(SearchService);                 │
│    // Header và List cùng inject → cùng 1 instance SearchService ✓          │
│                                                                              │
│  effect(() => {                                                             │
│    const query = this.searchService.searchQuery();  // "Nguyễn"            │
│    this.searchParams = { ...this.searchParams, fullName: query, page: 0 };  │
│    this.loadPatients();                                                     │
│  }, { allowSignalWrites: true });                                           │
│                                                                              │
│  // effect() tự động chạy khi searchQuery signal thay đổi                   │
│  // allowSignalWrites: true vì loadPatients() ghi signal (isLoading.set)     │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ gọi loadPatients()
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  LOAD PATIENTS — patient-list.component.ts                                  │
│                                                                              │
│    loadPatients(): void {                                                    │
│      this.isLoading.set(true);          // ← signal write                    │
│                                                                              │
│      this.patientService.search(this.searchParams).pipe(                   │
│        takeUntilDestroyed(this.destroyRef)                                   │
│      ).subscribe({                                                           │
│        next: (response) => {                                                 │
│          if (response.statusCode === 200) {                                 │
│            this.patients.set(response.data.result);  // ← signal write      │
│            this.pageMeta.set(response.data.meta);    // ← signal write      │
│          }                                                                  │
│          this.isLoading.set(false);                 // ← signal write      │
│        },                                                                   │
│        error: (err) => {                                                     │
│          this.toastService.error(...);                                      │
│          this.isLoading.set(false);                                         │
│        }                                                                    │
│      });                                                                     │
│    }                                                                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ HTTP GET /api/v1/patients?fullName=Nguy%E1%BB%85n&page=0&size=10
                                     │ (RestTemplate/WebClient bên trong HttpClient)
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  BACKEND SPRING — PatientController.java                                    │
│                                                                              │
│    @GetMapping("/patients")                                                  │
│    public ResponseEntity<ApiResponse<ResultPagination>> search(            │
│            @ParameterObject PatientSearchRequest search,                     │
│            @ParameterObject Pageable pageable) {                            │
│        ResultPagination result = patientService.search(search, pageable);  │
│        return ResponseEntity.ok(ApiResponse.success("OK", result));         │
│    }                                                                         │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ Response JSON
                                     │ { statusCode: 200, data: { result: [...], meta: {...} } }
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  SIGNAL CẬP NHẬT → CHANGE DETECTION (OnPush)                               │
│                                                                              │
│  patients signal thay đổi → Angular mark component cần re-render            │
│  Angular chạy template với patients() = [...] (đã filtered theo tên)       │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  TEMPLATE RENDER — patient-list.component.html                             │
│                                                                              │
│  @for (patient of patients(); track patient.id) {                          │
│    <tr>                                                                      │
│      <td>{{ patient.fullName }}</td>  ← "Nguyễn Văn A" hiển thị            │
│      ...                                                                    │
│    </tr>                                                                    │
│  }                                                                          │
│                                                                              │
│  ✅ User thấy danh sách bệnh nhân tên "Nguyễn"                              │
└──────────────────────────────────────────────────────────────────────────────┘
```



### Tóm tắt các thành phần tham gia


| Thành phần                   | Vai trò                                            |
| ---------------------------- | -------------------------------------------------- |
| `HeaderComponent`            | Gọi `SearchService.setQuery()` khi user nhấn Enter |
| `SearchService`              | Lưu search query dưới dạng `signal` (singleton)    |
| `PatientListComponent`       | `effect()` theo dõi `searchQuery` signal → gọi API |
| `PatientService`             | Gọi HTTP GET `/api/v1/patients?fullName=...`       |
| `PatientController` (Spring) | Xử lý search, trả về filtered results              |
| Template HTML                | Hiển thị `patients()` signal ra bảng               |


---



## 9. So Sánh Spring vs Angular


| Khái niệm           | Spring Boot                         | Angular                                    |
| ------------------- | ----------------------------------- | ------------------------------------------ |
| Entry point         | `main()` + `@SpringBootApplication` | `bootstrapApplication()` trong `main.ts`   |
| DI                  | `@Autowired`, `@Service`, `@Bean`   | `inject()` hoặc constructor                |
| Cấu hình module     | `@EnableWebMvc`                     | `provideRouter()`, `provideHttpClient()`   |
| Gọi HTTP            | `RestTemplate`, `WebClient`         | `HttpClient`                               |
| Dữ liệu bất đồng bộ | `Mono<T>`, `Flux<T>`                | `Observable<T>` (RxJS)                     |
| Reactive state      | `ApplicationEventPublisher`         | `Signal<T>`                                |
| Điều hướng          | `@GetMapping`, `@RequestMapping`    | Router + `<router-outlet>`                 |
| View                | JSP, Thymeleaf, FreeMarker          | Component template (HTML + `{{ }}`)        |
| Tự động cập nhật UI | Server-side render lại trang        | Client-side re-render khi signal thay đổi  |
| Caching             | `@Cacheable`                        | `computed()` signal                        |
| Xử lý sự kiện       | `@EventListener`, `@Transactional`  | `effect()`, lifecycle hooks                |
| Cleanup             | Container quản lý lifecycle bean    | `takeUntilDestroyed()` (hoặc `async pipe`) |




### So sánh filter flow

```
Spring:
  Request (search param)
    → DispatcherServlet
      → HandlerMapping (tìm Controller)
        → Controller method (@GetMapping)
          → Service (xử lý logic)
            → Repository (JPA)
              → Database
            → Trả JSON về client
          → Browser nhận JSON → JavaScript render

Angular (trong trường hợp này):
  User gõ input
    → HeaderComponent.onSearch()
      → SearchService (signal update)
        → PatientListComponent (effect triggered)
          → PatientService.search() → HTTP GET
            → Backend Spring trả JSON
          → Signal cập nhật (patients.set)
            → Angular OnPush detect thay đổi
              → Template re-render với data mới
```

---



## 10. Các Anti-patterns Quan Trọng Cần Tránh



### ❌ Không subscribe mà không cleanup

```typescript
// ❌ Memory leak tiềm tàng
ngOnInit(): void {
  this.patientService.search(params).subscribe(res => this.patients = res);
}

// ✅ An toàn
ngOnInit(): void {
  this.patientService.search(params).pipe(
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(res => this.patients = res);
}
```

**Tương tự Spring:** Như để `InputStream` không `.close()` — rò rỉ resource.

### ❌ Không mutate khi dùng OnPush

```typescript
// ❌ Angular OnPush không re-render
this.patients.push(newPatient);

// ✅ Tạo array mới
this.patients.update(list => [...list, newPatient]);
```

**Tương tự Spring:** Như modify entity mà không `save()` — changes không được persist.

### ❌ Service không được biết về UI

```typescript
// ❌ Service không nên alert/redirect
delete(id: number): Observable<void> {
  return this.http.delete(`${url}/${id}`).pipe(
    tap(() => {
      alert('Xóa thành công');    // ❌ UI logic
      this.router.navigate(['/']); // ❌ Navigation
    })
  );
}

// ✅ Chỉ trả Observable, component xử lý
delete(id: number): Observable<void> {
  return this.http.delete(`${url}/${id}`);
}

// Component xử lý
this.patientService.delete(id).subscribe(() => {
  this.toast.success('Xóa thành công'); // ✅ Đúng
  this.router.navigate(['/patients']);   // ✅ Đúng
});
```

**Tương tự Spring:** Service layer không nên forward/request sang view — chỉ trả response, Controller quyết định view.

---



## 11. Cấu Trúc Thư Mục — Feature-Based

```
fe/src/app/
├── app.component.ts          ← Root component (chỉ chứa <router-outlet>)
├── app.config.ts              ← Cấu hình app (router, http, zone)
├── app.routes.ts              ← Định nghĩa routes
│
├── layout/                    ← Layout chung (sidebar + header)
│   ├── layout.component.ts
│   ├── header/
│   └── sidebar/
│
├── shared/                    ← Dùng lại ở NHIỀU feature
│   ├── components/
│   │   ├── pagination/        ← Phân trang (dùng cho patient, doctor...)
│   │   ├── confirm-dialog/    ← Dialog xác nhận (dùng cho nhiều feature)
│   │   └── toast/             ← Thông báo (dùng toàn app)
│   ├── services/
│   │   └── search.service.ts  ← Search signal shared
│   └── pipes/
│       └── date-format.pipe.ts
│
├── features/                  ← Chia theo nghiệp vụ, KHÔNG chia theo loại file
│   └── patient/
│       ├── models/            ← Interfaces, enums, constants
│       │   ├── patient.model.ts
│       │   ├── patient-search-params.model.ts
│       │   └── patient.constants.ts
│       ├── services/          ← Gọi API, như @Service trong Spring
│       │   └── patient.service.ts
│       ├── components/        ← UI components
│       │   ├── patient-list/
│       │   ├── patient-form/
│       │   └── patient-filter-panel/
│       └── pages/             ← Page component (orchestrate các component con)
│           └── patients-page.component.ts
│
└── environments/              ← Cấu hình môi trường (dev/prod)
    ├── environment.ts
    └── environment.prod.ts
```

**So sánh:**


| Spring                              | Angular Feature-based          |
| ----------------------------------- | ------------------------------ |
| `@Controller` cho từng entity       | Component cho từng entity      |
| `src/main/java/com/.../controller/` | `features/patient/components/` |
| `src/main/java/com/.../service/`    | `features/patient/services/`   |
| `src/main/java/com/.../repository/` | Backend giữ nguyên             |


---

> **Tổng kết:** Angular là framework **client-side** (chạy trên trình duyệt), tập trung vào UI và interactivity. Spring Boot là **server-side** (chạy trên server), tập trung vào business logic và data. Cả hai dùng DI nhưng Angular dùng `inject()`/constructor, Spring dùng `@Autowired`. Điểm khác biệt lớn nhất: Angular tự cập nhật UI khi state thay đổi (qua Signal/Change Detection), trong khi Spring cần server render lại trang.

