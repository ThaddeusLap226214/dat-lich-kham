import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { APP_PAGES } from '../../core/app-pages';
import { ApiErrorService } from '../../shared/ui/api-error.service';
import { StatusBadgeComponent } from '../../shared/ui/status-badge.component';
import { STATUS_STYLES, StatusKind } from '../../shared/ui/status';

interface KitSection {
  id: string;
  title: string;
}

const SECTIONS: KitSection[] = [
  { id: 'page', title: 'Đầu trang' },
  { id: 'buttons', title: 'Nút' },
  { id: 'status', title: 'Nhãn trạng thái' },
  { id: 'cards', title: 'Thẻ, thẻ thống kê' },
  { id: 'form', title: 'Thanh lọc, form' },
  { id: 'table', title: 'Bảng' },
  { id: 'feedback', title: 'Thông báo, trống, đang tải' },
  { id: 'api-error', title: 'Lỗi API' },
  { id: 'tabs', title: 'Tab' },
  { id: 'modal', title: 'Hộp thoại' },
  { id: 'split', title: 'Hai cột: danh sách + chi tiết' },
  { id: 'summary', title: 'Tóm tắt nhãn – giá trị' },
  { id: 'steps', title: 'Các bước' },
  { id: 'choice', title: 'Thẻ lựa chọn' },
  { id: 'chips', title: 'Chip, dải ngày' },
  { id: 'slots', title: 'Lưới khung giờ' },
  { id: 'countdown', title: 'Đếm ngược giữ chỗ' },
  { id: 'timeline', title: 'Dòng thời gian trong ngày' },
  { id: 'compare', title: 'So sánh hai hồ sơ' },
  { id: 'week', title: 'Lưới tuần' },
  { id: 'auth', title: 'Đăng nhập, đăng ký' },
  { id: 'existing', title: 'Thành phần Angular có sẵn' }
];

// Trang nào cần thành phần nào (kiểm kê từ docs/04 và PROMPT-STITCH); thêm trang mới thì thêm dòng ở đây
const COVERAGE: Record<string, string[]> = {
  'login': ['auth', 'form', 'buttons', 'feedback', 'api-error'],
  'register': ['auth', 'form', 'buttons', 'feedback', 'api-error'],
  'doctors': ['page', 'split', 'cards', 'week', 'buttons', 'feedback'],
  'booking': ['page', 'steps', 'choice', 'chips', 'slots', 'countdown', 'summary', 'form', 'buttons', 'feedback', 'api-error'],
  'my-appointments': ['page', 'table', 'status', 'modal', 'form', 'buttons', 'feedback', 'api-error'],
  'my-patients': ['page', 'cards', 'status', 'modal', 'form', 'buttons', 'feedback', 'api-error'],
  'patients': ['page', 'form', 'table', 'status', 'modal', 'existing'],
  'duplicate-queue': ['page', 'tabs', 'split', 'compare', 'status', 'form', 'buttons', 'api-error'],
  'appointments': ['page', 'form', 'timeline', 'status', 'buttons', 'modal', 'api-error'],
  'doctor-agenda': ['page', 'cards', 'timeline', 'status', 'buttons', 'api-error'],
  'admin/catalog': ['page', 'tabs', 'table', 'modal', 'form', 'buttons', 'api-error'],
  'admin/work-schedules': ['page', 'form', 'week', 'modal', 'buttons', 'feedback', 'api-error'],
  'admin/accounts': ['page', 'table', 'status', 'modal', 'form', 'buttons', 'api-error'],
  'admin/reminders': ['page', 'form', 'table', 'status', 'buttons', 'feedback', 'api-error']
};

const SNIPPETS: Record<string, string> = {
  page: `<section class="ui-page">
  <header class="ui-page-header">
    <div>
      <h1 class="ui-page-title">Lịch hẹn theo bác sĩ</h1>
      <p class="ui-page-subtitle">Mô tả ngắn một dòng</p>
    </div>
    <div class="ui-page-actions">
      <button class="ui-btn ui-btn-primary">…</button>
    </div>
  </header>
  <!-- nội dung trang -->
</section>`,
  buttons: `<button class="ui-btn ui-btn-primary">
  <span class="material-symbols-outlined">add</span> Thêm hồ sơ
</button>
<button class="ui-btn ui-btn-secondary">Hủy bỏ</button>
<button class="ui-btn ui-btn-danger">Hủy lịch</button>
<button class="ui-btn ui-btn-ghost ui-btn-sm">Chi tiết</button>
<button class="ui-btn ui-btn-ghost ui-btn-icon" title="Sửa">
  <span class="material-symbols-outlined">edit</span>
</button>`,
  status: `<!-- import StatusBadgeComponent từ shared/ui -->
<app-status-badge kind="appointment" [value]="a.status" />
<app-status-badge kind="patient" [value]="p.status" />
<app-status-badge kind="account" [value]="acc.enabled" />

<!-- nhãn không phải trạng thái -->
<span class="ui-badge ui-tone-info ui-badge-plain">Nội tổng quát</span>
<span class="ui-count">3</span>`,
  cards: `<div class="ui-stats">
  <div class="ui-stat">
    <div class="ui-stat-label">Tổng lượt hôm nay</div>
    <div class="ui-stat-value">12</div>
  </div>
</div>

<div class="ui-card-grid">
  <article class="ui-card">
    <div class="ui-card-header">
      <h3 class="ui-card-title">Lê Thị Hoa</h3>
      <app-status-badge kind="patient" value="ACTIVE" />
    </div>
    …
  </article>
</div>`,
  form: `<div class="ui-toolbar">
  <div class="ui-field ui-toolbar-grow">
    <label class="ui-label">Tìm kiếm</label>
    <input class="ui-input" placeholder="Họ tên, số giấy tờ…">
  </div>
  <div class="ui-field">
    <label class="ui-label">Trạng thái</label>
    <select class="ui-select">…</select>
  </div>
</div>

<div class="ui-form-grid">
  <div class="ui-field">
    <label class="ui-label is-required">Họ tên</label>
    <input class="ui-input" [class.is-invalid]="nameInvalid">
    <span class="ui-field-error">Vui lòng nhập họ tên</span>
  </div>
  <div class="ui-field ui-field-full">
    <label class="ui-label">Lý do khám</label>
    <textarea class="ui-textarea"></textarea>
    <span class="ui-hint">Không bắt buộc</span>
  </div>
</div>
<div class="ui-form-actions">…nút…</div>`,
  table: `<div class="ui-table-wrap">
  <table class="ui-table">
    <thead><tr><th>Họ tên</th><th>Điện thoại</th><th>Trạng thái</th><th></th></tr></thead>
    <tbody>
      <tr>                         <!-- class="is-highlight" | "is-muted" -->
        <td>Lê Thị Hoa</td>
        <td class="ui-nowrap">0905 123 456</td>
        <td><app-status-badge kind="patient" value="ACTIVE" /></td>
        <td><div class="ui-actions">…nút ui-btn-icon…</div></td>
      </tr>
    </tbody>
  </table>
</div>
<!-- phân trang: <app-pagination> có sẵn -->`,
  feedback: `<div class="ui-alert ui-tone-warning">
  <span class="material-symbols-outlined">warning</span>
  <div><div class="ui-alert-title">Tiêu đề</div>Nội dung</div>
</div>

<div class="ui-empty">
  <span class="material-symbols-outlined">event_busy</span>
  Chưa có lịch hẹn nào
</div>

<div class="ui-loading"><span class="ui-spinner"></span> Đang tải…</div>`,
  'api-error': `private readonly apiError = inject(ApiErrorService);

this.service.hold(req).subscribe({
  next: a => …,
  error: err => {
    if (this.apiError.is(err, 'SLOT_ALREADY_HELD')) {
      // mã lỗi nghiệp vụ trang tự xử lý
      this.reloadSlots();
    }
    this.apiError.show(err);   // toast: câu tiếng Việt từ backend
  }
});`,
  tabs: `<div class="ui-tabs">
  <button class="ui-tab" [class.is-active]="tab === 'queue'" (click)="tab = 'queue'">Hàng chờ</button>
  <button class="ui-tab" [class.is-active]="tab === 'history'" (click)="tab = 'history'">Lịch sử gộp</button>
</div>`,
  modal: `@if (open) {
  <div class="ui-modal-backdrop" (click)="open = false">
    <div class="ui-modal" (click)="$event.stopPropagation()">   <!-- thêm ui-modal-lg nếu form rộng -->
      <div class="ui-modal-header">
        <h2 class="ui-modal-title">Hủy lịch hẹn</h2>
        <button class="ui-btn ui-btn-ghost ui-btn-icon" (click)="open = false">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      <div class="ui-modal-body">…form…</div>
      <div class="ui-modal-footer">…nút…</div>
    </div>
  </div>
}
<!-- chỉ hỏi Có / Không: dùng <app-confirm-dialog> có sẵn -->`,
  split: `<div class="ui-split">
  <div class="ui-list">
    <button class="ui-list-item is-selected">
      <strong>Lê Thị Hoa ↔ Le Thi Hoa</strong>
      <span class="ui-muted">Cùng giấy tờ nhưng khác thông tin · 92%</span>
    </button>
    <button class="ui-list-item">…</button>
  </div>
  <div class="ui-card">…chi tiết…</div>
</div>`,
  summary: `<dl class="ui-summary">
  <dt>Bác sĩ</dt><dd>ThS.BS Nguyễn Văn An</dd>
  <dt>Thời gian</dt><dd>08:30, Thứ Hai 12/10</dd>
</dl>`,
  steps: `<ol class="ui-steps">
  <li class="ui-step is-done">Chuyên khoa</li>
  <li class="ui-step is-current">Chọn giờ</li>
  <li class="ui-step">Giữ chỗ</li>
</ol>`,
  choice: `<div class="ui-choice-grid">
  <button class="ui-choice" [class.is-selected]="d.id === selectedId" (click)="select(d)">
    <span class="ui-choice-title">ThS.BS Nguyễn Văn An</span>
    <span class="ui-muted">Nội tổng quát</span>
  </button>
</div>`,
  chips: `<div class="ui-chip-row">
  <button class="ui-chip is-selected">T2<small>12/10</small></button>
  <button class="ui-chip">T3<small>13/10</small></button>
  <button class="ui-chip" disabled>CN<small>18/10</small></button>
</div>`,
  slots: `<div class="ui-slot-grid">
  <button class="ui-slot">07:30</button>
  <button class="ui-slot is-selected">07:50</button>
  <button class="ui-slot" disabled>08:10</button>          <!-- đã có người / đã qua -->
  <button class="ui-slot is-locked" disabled>08:30</button> <!-- vừa nhận 409 SLOT_ALREADY_HELD -->
</div>`,
  countdown: `<div class="ui-countdown" [class.is-urgent]="seconds < 60">
  <span class="material-symbols-outlined">timer</span>
  Giữ chỗ còn <strong>04:59</strong>
</div>`,
  timeline: `<div class="ui-timeline">
  <div class="ui-timeline-row is-now">
    <span class="ui-timeline-time">08:10</span>
    <div><strong>Lê Thị Hoa</strong> <span class="ui-muted">· Đau đầu</span></div>
    <app-status-badge kind="appointment" value="CONFIRMED" />
  </div>
  <div class="ui-timeline-row is-empty">
    <span class="ui-timeline-time">08:30</span><div>Trống</div><span></span>
  </div>
</div>`,
  compare: `<table class="ui-compare">
  <thead><tr><th></th><th>Hồ sơ #1</th><th>Hồ sơ #3</th></tr></thead>
  <tbody>
    <tr class="is-diff"><th>Họ tên</th><td>Lê Thị Hoa</td><td>Le Thi Hoa</td></tr>
    <tr class="is-same"><th>Ngày sinh</th><td>12/05/1990</td><td>12/05/1990</td></tr>
  </tbody>
</table>`,
  week: `<div class="ui-week">
  <div class="ui-week-day">
    <div class="ui-week-day-name">Thứ 2</div>
    <div class="ui-week-shift">07:30–11:30<br><span class="ui-muted">P101 · 20 phút</span></div>
    <div class="ui-week-shift is-inactive">13:30–16:30</div>
  </div>
  …7 cột…
</div>`,
  auth: `<div class="ui-auth">
  <form class="ui-auth-card">
    <h1 class="ui-page-title">Đăng nhập</h1>
    <div class="ui-field">
      <label class="ui-label">Tên đăng nhập</label>
      <input class="ui-input">
    </div>
    <div class="ui-alert ui-tone-danger">Sai tên đăng nhập hoặc mật khẩu</div>
    <button class="ui-btn ui-btn-primary">Đăng nhập</button>
  </form>
</div>`,
  existing: `<app-pagination …>        shared/components/pagination
<app-confirm-dialog …>    shared/components/confirm-dialog
ToastService              shared/components/toast — success / error / warning / info`
};

@Component({
  selector: 'app-ui-kit-page',
  standalone: true,
  imports: [StatusBadgeComponent],
  templateUrl: './ui-kit-page.component.html',
  styleUrls: ['./ui-kit-page.component.css']
})
export class UiKitPageComponent {
  private readonly http = inject(HttpClient);
  private readonly apiError = inject(ApiErrorService);

  readonly sections = SECTIONS;
  readonly snippets = SNIPPETS;
  readonly statusKinds = Object.keys(STATUS_STYLES) as StatusKind[];
  readonly statusValues = (kind: StatusKind) => Object.keys(STATUS_STYLES[kind]);

  readonly coverage = APP_PAGES.map(p => ({
    path: p.path,
    title: p.title,
    part: p.part,
    sections: (COVERAGE[p.path] ?? []).map(id => SECTIONS.find(s => s.id === id)?.title ?? `?${id}`)
  }));
  readonly uncovered = computed(() => this.coverage.filter(c => c.sections.length === 0).map(c => c.title));
  readonly unknownIds = Object.values(COVERAGE).flat().filter(id => !SECTIONS.some(s => s.id === id));

  goTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  usedBy(id: string): string {
    return this.coverage.filter(c => (COVERAGE[c.path] ?? []).includes(id)).map(c => c.title).join(', ') || '—';
  }

  readonly tab = signal<'a' | 'b'>('a');
  readonly modalOpen = signal(false);
  readonly calling = signal(false);

  callNotImplemented(): void {
    this.calling.set(true);
    this.http.get(`${environment.apiUrl}/reminders/logs`).subscribe({
      next: () => this.calling.set(false),
      error: err => {
        this.calling.set(false);
        this.apiError.show(err);
      }
    });
  }

  simulate(status: number): void {
    const samples: Record<number, object | null> = {
      409: { status: 409, error: 'SLOT_ALREADY_HELD', message: 'Khung giờ này vừa có người giữ', path: '/api/v1/appointments/hold' },
      403: { status: 403, error: 'FORBIDDEN', message: 'Bạn không có quyền thực hiện thao tác này' },
      0: null
    };
    this.apiError.show(new HttpErrorResponse({ status, error: samples[status] ?? null }));
  }
}
