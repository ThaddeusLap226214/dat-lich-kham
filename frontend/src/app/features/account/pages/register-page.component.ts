import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { environment } from '../../../../environments/environment';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { Account } from '../models/account.model';
import { AccountService } from '../services/account.service';

function samePassword(group: AbstractControl): ValidationErrors | null {
  return group.get('password')?.value === group.get('confirmPassword')?.value ? null : { mismatch: true };
}

// Khách tự tạo tài khoản bệnh nhân (CT06). Đăng nhập làm ở trang của Keycloak, không ở đây
@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="ui-auth">
      @if (created(); as a) {
        <div class="ui-auth-card">
          <h1 class="ui-page-title">Đăng ký thành công</h1>
          <div class="ui-alert ui-tone-success">
            <span class="material-symbols-outlined">check_circle</span>
            <div>Đã tạo tài khoản <strong>{{ a.username }}</strong> (mã {{ a.id }}). Bạn có thể đăng nhập bằng tài khoản này.</div>
          </div>
          <a class="ui-btn ui-btn-primary" [href]="accountConsoleUrl" target="_blank" rel="noopener">Đăng nhập thử ở Keycloak</a>
          <button type="button" class="ui-btn ui-btn-secondary" (click)="reset()">Tạo tài khoản khác</button>
        </div>
      } @else {
        <form class="ui-auth-card" [formGroup]="form" (ngSubmit)="submit()">
          <h1 class="ui-page-title">Đăng ký</h1>
          <p class="ui-page-subtitle">Tạo tài khoản bệnh nhân để đặt lịch khám</p>

          <div class="ui-field">
            <label class="ui-label is-required" for="username">Tên đăng nhập</label>
            <input id="username" class="ui-input" formControlName="username" autocomplete="username"
                   [class.is-invalid]="invalid('username') || fieldError() === 'username'">
            @if (fieldError() === 'username') {
              <span class="ui-field-error">Tên đăng nhập đã có người dùng</span>
            } @else if (invalid('username')) {
              <span class="ui-field-error">4–50 ký tự: chữ không dấu, số, dấu chấm, gạch ngang, gạch dưới</span>
            }
          </div>
          <div class="ui-field">
            <label class="ui-label is-required" for="fullName">Họ tên</label>
            <input id="fullName" class="ui-input" formControlName="fullName" autocomplete="name"
                   [class.is-invalid]="invalid('fullName')">
            @if (invalid('fullName')) {
              <span class="ui-field-error">Vui lòng nhập họ tên</span>
            }
          </div>
          <div class="ui-field">
            <label class="ui-label" for="email">Email</label>
            <input id="email" class="ui-input" type="email" formControlName="email" autocomplete="email"
                   [class.is-invalid]="invalid('email') || fieldError() === 'email'">
            @if (fieldError() === 'email') {
              <span class="ui-field-error">Email đã được dùng cho tài khoản khác</span>
            } @else if (invalid('email')) {
              <span class="ui-field-error">Email chưa đúng định dạng</span>
            } @else {
              <span class="ui-hint">Dùng để nhận thư nhắc lịch khám</span>
            }
          </div>
          <div class="ui-field">
            <label class="ui-label" for="phone">Số điện thoại</label>
            <input id="phone" class="ui-input" formControlName="phone" autocomplete="tel"
                   [class.is-invalid]="invalid('phone')">
            @if (invalid('phone')) {
              <span class="ui-field-error">Gồm 10 chữ số, bắt đầu bằng 0</span>
            }
          </div>
          <div class="ui-field">
            <label class="ui-label is-required" for="password">Mật khẩu</label>
            <input id="password" class="ui-input" type="password" formControlName="password" autocomplete="new-password"
                   [class.is-invalid]="invalid('password')">
            @if (invalid('password')) {
              <span class="ui-field-error">Ít nhất 6 ký tự</span>
            }
          </div>
          <div class="ui-field">
            <label class="ui-label is-required" for="confirmPassword">Nhập lại mật khẩu</label>
            <input id="confirmPassword" class="ui-input" type="password" formControlName="confirmPassword"
                   autocomplete="new-password" [class.is-invalid]="mismatch()">
            @if (mismatch()) {
              <span class="ui-field-error">Hai mật khẩu chưa giống nhau</span>
            }
          </div>

          @if (error(); as e) {
            <div class="ui-alert ui-tone-danger">
              <span class="material-symbols-outlined">error</span>
              <div>{{ e }}</div>
            </div>
          }
          <button class="ui-btn ui-btn-primary" type="submit" [disabled]="saving()">
            {{ saving() ? 'Đang tạo…' : 'Đăng ký' }}
          </button>
        </form>
      }
    </div>
  `
})
export class RegisterPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly accounts = inject(AccountService);
  private readonly apiError = inject(ApiErrorService);

  readonly accountConsoleUrl = `${environment.keycloakRealmUrl}/account`;

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50), Validators.pattern(/^[a-zA-Z0-9._-]+$/)]],
    fullName: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.email]],
    phone: ['', [Validators.pattern(/^0\d{9}$/)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: samePassword });

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly fieldError = signal<'username' | 'email' | null>(null);
  readonly created = signal<Account | null>(null);

  invalid(name: string): boolean {
    const c = this.form.get(name);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  mismatch(): boolean {
    const c = this.form.get('confirmPassword');
    return !!c && (c.touched || c.dirty) && this.form.hasError('mismatch');
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.fieldError.set(null);
    this.accounts.register({
      username: v.username.trim(),
      password: v.password,
      fullName: v.fullName.trim(),
      email: v.email.trim() || null,
      phone: v.phone.trim() || null
    }).subscribe({
      next: account => {
        this.saving.set(false);
        this.created.set(account);
      },
      error: err => {
        this.saving.set(false);
        if (this.apiError.is(err, 'USERNAME_TAKEN')) {
          this.fieldError.set('username');
        } else if (this.apiError.is(err, 'EMAIL_TAKEN')) {
          this.fieldError.set('email');
        } else {
          this.error.set(this.apiError.parse(err).message);
        }
      }
    });
  }

  reset(): void {
    this.form.reset();
    this.created.set(null);
  }
}
