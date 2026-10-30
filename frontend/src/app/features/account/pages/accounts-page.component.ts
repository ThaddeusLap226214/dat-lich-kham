import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';
import { Account, AccountRole, DoctorOption, ROLE_OPTIONS } from '../models/account.model';
import { AccountService } from '../services/account.service';

// Quản trị viên tạo tài khoản nhân viên kèm mật khẩu ban đầu, khóa / mở tài khoản (CT05, CS33, CS34).
// Đổi vai trò, đặt lại mật khẩu: làm ở trang quản trị Keycloak, không có ở đây
@Component({
  selector: 'app-accounts-page',
  standalone: true,
  imports: [ReactiveFormsModule, StatusBadgeComponent],
  template: `
    <section class="ui-page">
      <header class="ui-page-header">
        <div>
          <h1 class="ui-page-title">Tài khoản</h1>
          <p class="ui-page-subtitle">Tạo tài khoản lễ tân, bác sĩ, quản trị; khóa hoặc mở tài khoản</p>
        </div>
        <div class="ui-page-actions">
          <button class="ui-btn ui-btn-primary" (click)="openCreate()">
            <span class="material-symbols-outlined">person_add</span> Tạo tài khoản
          </button>
        </div>
      </header>

      <div class="ui-toolbar">
        <div class="ui-field">
          <label class="ui-label" for="roleFilter">Vai trò</label>
          <select id="roleFilter" class="ui-select" [value]="roleFilter() ?? ''" (change)="filter($any($event.target).value)">
            <option value="">Tất cả</option>
            @for (r of roles; track r.value) {
              <option [value]="r.value">{{ r.label }}</option>
            }
          </select>
        </div>
      </div>

      @if (loading()) {
        <div class="ui-loading"><span class="ui-spinner"></span> Đang tải…</div>
      } @else if (accounts().length === 0) {
        <div class="ui-empty"><span class="material-symbols-outlined">group_off</span>Không có tài khoản nào</div>
      } @else {
        <div class="ui-table-wrap">
          <table class="ui-table">
            <thead>
              <tr><th>Mã</th><th>Tên đăng nhập</th><th>Họ tên</th><th>Vai trò</th><th>Liên hệ</th><th>Trạng thái</th><th></th></tr>
            </thead>
            <tbody>
              @for (a of accounts(); track a.id) {
                <tr>
                  <td>{{ a.id }}</td>
                  <td class="ui-nowrap"><strong>{{ a.username }}</strong></td>
                  <td>{{ a.fullName }}</td>
                  <td>
                    <app-status-badge kind="role" [value]="a.role" />
                    @if (a.doctorId !== null) {
                      <div class="ui-muted">Bác sĩ mã {{ a.doctorId }}</div>
                    }
                  </td>
                  <td>
                    <div>{{ a.email ?? '—' }}</div>
                    <div class="ui-muted ui-nowrap">{{ a.phone ?? '' }}</div>
                  </td>
                  <td><app-status-badge kind="account" [value]="a.enabled" /></td>
                  <td class="ui-nowrap">
                    @if (a.enabled) {
                      <button class="ui-btn ui-btn-ghost ui-btn-icon" title="Khóa tài khoản" [disabled]="busyId() === a.id"
                              (click)="toggle(a)"><span class="material-symbols-outlined">lock</span></button>
                    } @else {
                      <button class="ui-btn ui-btn-ghost ui-btn-icon" title="Mở khóa" [disabled]="busyId() === a.id"
                              (click)="toggle(a)"><span class="material-symbols-outlined">lock_open</span></button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (creating()) {
        <div class="ui-modal-backdrop" (click)="closeCreate()">
          <form class="ui-modal" [formGroup]="form" (ngSubmit)="save()" (click)="$event.stopPropagation()">
            <div class="ui-modal-header">
              <h2 class="ui-modal-title">Tạo tài khoản</h2>
              <button type="button" class="ui-btn ui-btn-ghost ui-btn-icon" (click)="closeCreate()">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
            <div class="ui-modal-body">
              <div class="ui-form-grid">
                <div class="ui-field">
                  <label class="ui-label is-required" for="role">Vai trò</label>
                  <select id="role" class="ui-select" formControlName="role">
                    @for (r of roles; track r.value) {
                      <option [value]="r.value">{{ r.label }}</option>
                    }
                  </select>
                </div>
                @if (form.controls.role.value === 'DOCTOR') {
                  <div class="ui-field">
                    <label class="ui-label is-required" for="doctorId">Bác sĩ</label>
                    @if (doctors(); as list) {
                      <select id="doctorId" class="ui-select" formControlName="doctorId"
                              [class.is-invalid]="doctorMissing()">
                        <option [ngValue]="null">— Chọn bác sĩ —</option>
                        @for (d of list; track d.id) {
                          <option [ngValue]="d.id">{{ d.fullName }} · {{ d.specialtyName }}</option>
                        }
                      </select>
                    } @else {
                      <input id="doctorId" class="ui-input" type="number" min="1" formControlName="doctorId"
                             [class.is-invalid]="doctorMissing()">
                      <span class="ui-hint">Chưa tải được danh sách bác sĩ, nhập mã bác sĩ bên Provider</span>
                    }
                    @if (doctorMissing()) {
                      <span class="ui-field-error">Tài khoản bác sĩ phải gắn với một bác sĩ</span>
                    }
                  </div>
                }
                <div class="ui-field">
                  <label class="ui-label is-required" for="username">Tên đăng nhập</label>
                  <input id="username" class="ui-input" formControlName="username" [class.is-invalid]="invalid('username')">
                  @if (invalid('username')) {
                    <span class="ui-field-error">4–50 ký tự: chữ không dấu, số, dấu chấm, gạch ngang, gạch dưới</span>
                  }
                </div>
                <div class="ui-field">
                  <label class="ui-label is-required" for="password">Mật khẩu ban đầu</label>
                  <input id="password" class="ui-input" type="text" formControlName="password" [class.is-invalid]="invalid('password')">
                  @if (invalid('password')) {
                    <span class="ui-field-error">Ít nhất 6 ký tự</span>
                  } @else {
                    <span class="ui-hint">Gửi cho nhân viên; đổi mật khẩu sau ở trang Keycloak</span>
                  }
                </div>
                <div class="ui-field ui-field-full">
                  <label class="ui-label is-required" for="fullName">Họ tên</label>
                  <input id="fullName" class="ui-input" formControlName="fullName" [class.is-invalid]="invalid('fullName')">
                  @if (invalid('fullName')) {
                    <span class="ui-field-error">Vui lòng nhập họ tên</span>
                  }
                </div>
                <div class="ui-field">
                  <label class="ui-label" for="email">Email</label>
                  <input id="email" class="ui-input" type="email" formControlName="email" [class.is-invalid]="invalid('email')">
                  @if (invalid('email')) {
                    <span class="ui-field-error">Email chưa đúng định dạng</span>
                  }
                </div>
                <div class="ui-field">
                  <label class="ui-label" for="phone">Số điện thoại</label>
                  <input id="phone" class="ui-input" formControlName="phone" [class.is-invalid]="invalid('phone')">
                  @if (invalid('phone')) {
                    <span class="ui-field-error">Gồm 10 chữ số, bắt đầu bằng 0</span>
                  }
                </div>
              </div>
              @if (formError(); as e) {
                <div class="ui-alert ui-tone-danger">
                  <span class="material-symbols-outlined">error</span>
                  <div>{{ e }}</div>
                </div>
              }
            </div>
            <div class="ui-modal-footer">
              <button type="button" class="ui-btn ui-btn-secondary" (click)="closeCreate()">Đóng</button>
              <button type="submit" class="ui-btn ui-btn-primary" [disabled]="saving()">
                {{ saving() ? 'Đang tạo…' : 'Tạo tài khoản' }}
              </button>
            </div>
          </form>
        </div>
      }
    </section>
  `
})
export class AccountsPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(AccountService);
  private readonly apiError = inject(ApiErrorService);
  private readonly toast = inject(ToastService);

  readonly roles = ROLE_OPTIONS;
  readonly accounts = signal<Account[]>([]);
  readonly loading = signal(false);
  readonly roleFilter = signal<AccountRole | null>(null);
  readonly busyId = signal<number | null>(null);

  readonly creating = signal(false);
  readonly saving = signal(false);
  readonly formError = signal<string | null>(null);
  // null = chưa tải được danh sách bác sĩ (API Provider chưa làm hoặc lỗi) → cho nhập mã bằng tay
  readonly doctors = signal<DoctorOption[] | null>(null);

  readonly form = this.fb.group({
    role: this.fb.nonNullable.control<AccountRole>('RECEPTIONIST', Validators.required),
    doctorId: this.fb.control<number | null>(null),
    username: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(4), Validators.maxLength(50),
      Validators.pattern(/^[a-zA-Z0-9._-]+$/)]),
    password: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]),
    fullName: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    email: this.fb.nonNullable.control('', Validators.email),
    phone: this.fb.nonNullable.control('', Validators.pattern(/^0\d{9}$/))
  });

  ngOnInit(): void {
    this.load();
  }

  filter(value: string): void {
    this.roleFilter.set(value ? value as AccountRole : null);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.list(this.roleFilter()).subscribe({
      next: list => {
        this.accounts.set(list);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.apiError.show(err);
      }
    });
  }

  toggle(account: Account): void {
    const action = account.enabled ? 'khóa' : 'mở khóa';
    if (!confirm(`Xác nhận ${action} tài khoản ${account.username}?`)) {
      return;
    }
    this.busyId.set(account.id);
    const call = account.enabled ? this.service.lock(account.id) : this.service.unlock(account.id);
    call.subscribe({
      next: updated => {
        this.busyId.set(null);
        this.accounts.update(list => list.map(a => a.id === updated.id ? updated : a));
        this.toast.success('Đã cập nhật', `Đã ${action} tài khoản ${updated.username}`);
      },
      error: err => {
        this.busyId.set(null);
        this.apiError.show(err);
      }
    });
  }

  openCreate(): void {
    this.form.reset({ role: 'RECEPTIONIST', doctorId: null, username: '', password: '', fullName: '', email: '', phone: '' });
    this.formError.set(null);
    this.creating.set(true);
    if (this.doctors() === null) {
      this.service.doctors().subscribe({
        next: list => this.doctors.set(list),
        error: () => this.doctors.set(null)
      });
    }
  }

  closeCreate(): void {
    this.creating.set(false);
  }

  invalid(name: string): boolean {
    const c = this.form.get(name);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  doctorMissing(): boolean {
    const c = this.form.controls.doctorId;
    return this.form.controls.role.value === 'DOCTOR' && c.touched && !c.value;
  }

  save(): void {
    this.form.markAllAsTouched();
    const v = this.form.getRawValue();
    if (this.form.invalid || (v.role === 'DOCTOR' && !v.doctorId)) {
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    this.service.create({
      username: v.username.trim(),
      password: v.password,
      fullName: v.fullName.trim(),
      email: v.email.trim() || null,
      phone: v.phone.trim() || null,
      role: v.role,
      doctorId: v.role === 'DOCTOR' ? Number(v.doctorId) : null
    }).subscribe({
      next: account => {
        this.saving.set(false);
        this.creating.set(false);
        this.toast.success('Đã tạo tài khoản', `${account.username} (mã ${account.id}) đăng nhập được ngay`);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.formError.set(this.apiError.parse(err).message);
      }
    });
  }
}
