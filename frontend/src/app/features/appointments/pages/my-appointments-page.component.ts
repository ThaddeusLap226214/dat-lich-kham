import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppointmentAgendaService } from '../services/appointment-agenda.service';
import { AppointmentView } from '../models/appointment-agenda.model';
import { IdentityService } from '../../../core/identity.service';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';

@Component({
  selector: 'app-my-appointments-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent],
  template: `
    <section class="ui-page">
      <header class="ui-page-header">
        <div>
          <h1 class="ui-page-title">Lịch khám của tôi</h1>
          <p class="ui-page-subtitle">Theo dõi các lịch tự đặt và lịch được lễ tân đặt hộ cho hồ sơ của bạn</p>
        </div>
        <div class="ui-page-actions">
          <button type="button" class="ui-btn ui-btn-outline" (click)="loadAppointments()" [disabled]="loading">
            <span class="material-symbols-outlined">refresh</span> Tải lại
          </button>
          <a class="ui-btn ui-btn-primary" routerLink="/booking">
            <span class="material-symbols-outlined">event_available</span> Đặt lịch mới
          </a>
        </div>
      </header>

      @if (errorMessage) {
        <div class="ui-alert ui-alert-danger" style="margin-bottom: 16px;">
          <span class="material-symbols-outlined">error</span>
          <div class="ui-alert-content">{{ errorMessage }}</div>
        </div>
      }

      @if (cancellingTarget) {
        <div class="ui-card" style="margin-bottom: 16px; border: 1px solid #ef4444;">
          <h2 style="margin: 0 0 10px; font-size: 1rem;">
            Hủy lịch khám #{{ cancellingTarget.appointmentId }} ({{ formatDateTime(cancellingTarget.startTime) }})
          </h2>
          <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: end;">
            <div class="ui-field" style="flex: 1; min-width: 240px;">
              <label class="ui-label">Lý do hủy (tùy chọn)</label>
              <input class="ui-input" [(ngModel)]="cancelReason" placeholder="VD: Bận đột xuất" />
            </div>
            <button type="button" class="ui-btn ui-btn-danger" (click)="confirmCancel()" [disabled]="processing">
              {{ processing ? 'Đang hủy...' : 'Xác nhận hủy' }}
            </button>
            <button type="button" class="ui-btn ui-btn-outline" (click)="cancellingTarget = null" [disabled]="processing">
              Đóng
            </button>
          </div>
        </div>
      }

      @if (loading) {
        <div class="ui-card">Đang tải lịch khám...</div>
      } @else if (appointments.length === 0) {
        <div class="ui-card">Bạn chưa có lịch khám nào.</div>
      } @else {
        <div class="ui-card" style="padding: 0; overflow-x: auto;">
          <table class="ui-table">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Thời gian</th>
                <th>Bệnh nhân</th>
                <th>Bác sĩ</th>
                <th>Phòng khám</th>
                <th>Trạng thái</th>
                <th>Ghi chú / Lý do</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              @for (a of appointments; track a.appointmentId) {
                <tr>
                  <td>#{{ a.appointmentId }}</td>
                  <td>
                    <div><strong>{{ formatDateTime(a.startTime) }}</strong></div>
                    <div style="font-size: 0.85em; opacity: 0.75;">đến {{ a.endTime ? a.endTime.slice(11, 16) : '—' }}</div>
                  </td>
                  <td>{{ a.patient?.fullName || 'Không rõ' }}</td>
                  <td>
                    @if (a.doctor) {
                      <div><strong>{{ a.doctor.fullName }}</strong></div>
                      @if (a.doctor.specialtyName) {
                        <div style="font-size: 0.85em; opacity: 0.75;">{{ a.doctor.specialtyName }}</div>
                      }
                    } @else {
                      <span>Không rõ</span>
                    }
                  </td>
                  <td>{{ a.room?.name || 'Không rõ' }}</td>
                  <td><app-status-badge kind="appointment" [value]="a.status" /></td>
                  <td>
                    @if (a.cancelReason) {
                      <div>Lý do hủy: {{ a.cancelReason }}</div>
                    } @else if (a.reason) {
                      <div>{{ a.reason }}</div>
                    } @else {
                      <span>—</span>
                    }
                  </td>
                  <td>
                    @if (canCancel(a)) {
                      <button type="button" class="ui-btn ui-btn-danger" (click)="openCancel(a)">Hủy lịch</button>
                    } @else if (a.status === 'CONFIRMED') {
                      <span style="font-size: 0.85em; opacity: 0.8;">Liên hệ phòng khám để hủy</span>
                    } @else {
                      <span>—</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `
})
export class MyAppointmentsPageComponent implements OnInit {
  private readonly agendaService = inject(AppointmentAgendaService);
  private readonly identityService = inject(IdentityService);
  private readonly apiError = inject(ApiErrorService);
  private readonly toast = inject(ToastService);

  readonly currentIdentity = this.identityService.identity;

  appointments: AppointmentView[] = [];
  loading = false;
  processing = false;
  errorMessage = '';

  cancellingTarget: AppointmentView | null = null;
  cancelReason = '';

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';
    this.agendaService.getMyAppointments().subscribe({
      next: (data) => {
        this.appointments = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.loading = false;
      }
    });
  }

  canCancel(a: AppointmentView): boolean {
    const accountId = this.currentIdentity().accountId;
    return a.status === 'CONFIRMED' && accountId !== null && a.bookedByAccountId === accountId;
  }

  openCancel(a: AppointmentView): void {
    this.cancellingTarget = a;
    this.cancelReason = '';
    this.errorMessage = '';
  }

  confirmCancel(): void {
    if (!this.cancellingTarget || this.processing) return;
    this.processing = true;
    this.errorMessage = '';
    this.agendaService.cancel(this.cancellingTarget.appointmentId, this.cancelReason.trim()).subscribe({
      next: () => {
        this.toast.success('Thành công', 'Đã hủy lịch khám');
        this.cancellingTarget = null;
        this.processing = false;
        this.loadAppointments();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.processing = false;
      }
    });
  }

  formatDateTime(iso: string): string {
    if (!iso) return '—';
    return `${iso.slice(0, 10)} ${iso.slice(11, 16)}`;
  }
}

