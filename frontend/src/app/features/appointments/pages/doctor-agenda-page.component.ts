import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppointmentAgendaService } from '../services/appointment-agenda.service';
import { AppointmentView } from '../models/appointment-agenda.model';
import { IdentityService } from '../../../core/identity.service';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';

@Component({
  selector: 'app-doctor-agenda-page',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  template: `
    <section class="ui-page">
      <header class="ui-page-header">
        <div>
          <h1 class="ui-page-title">Lịch khám trong ngày</h1>
          <p class="ui-page-subtitle">Danh sách bệnh nhân hẹn khám trong ngày của bác sĩ và cập nhật trạng thái sau khám</p>
        </div>
        <div class="ui-page-actions">
          <button type="button" class="ui-btn ui-btn-outline" (click)="loadAgenda()" [disabled]="loading">
            <span class="material-symbols-outlined">refresh</span> Tải lại
          </button>
        </div>
      </header>

      @if (errorMessage) {
        <div class="ui-alert ui-alert-danger" style="margin-bottom: 16px;">
          <span class="material-symbols-outlined">error</span>
          <div class="ui-alert-content">{{ errorMessage }}</div>
        </div>
      }

      <div class="ui-toolbar">
        <div class="ui-field">
          <label class="ui-label">Ngày khám</label>
          <input class="ui-input" type="date" [(ngModel)]="selectedDate" (change)="loadAgenda()" />
        </div>
      </div>

      @if (loading) {
        <div class="ui-card">Đang tải lịch khám trong ngày...</div>
      } @else if (appointments.length === 0) {
        <div class="ui-card">Không có lịch khám nào trong ngày {{ selectedDate }}.</div>
      } @else {
        <div class="ui-card" style="padding: 0; overflow-x: auto;">
          <table class="ui-table">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Giờ khám</th>
                <th>Bệnh nhân</th>
                <th>Phòng khám</th>
                <th>Lý do khám</th>
                <th>Trạng thái</th>
                <th>Cập nhật</th>
              </tr>
            </thead>
            <tbody>
              @for (a of appointments; track a.appointmentId) {
                <tr>
                  <td>#{{ a.appointmentId }}</td>
                  <td><strong>{{ a.startTime ? a.startTime.slice(11, 16) : '—' }} – {{ a.endTime ? a.endTime.slice(11, 16) : '—' }}</strong></td>
                  <td>
                    <div><strong>{{ a.patient?.fullName || 'Không rõ' }}</strong></div>
                    @if (a.patient?.dateOfBirth) {
                      <div style="font-size: 0.85em; opacity: 0.75;">Sinh: {{ a.patient?.dateOfBirth }}</div>
                    }
                  </td>
                  <td>{{ a.room?.name || 'Không rõ' }}</td>
                  <td>{{ a.reason || '—' }}</td>
                  <td><app-status-badge kind="appointment" [value]="a.status" /></td>
                  <td>
                    @if (a.status === 'CONFIRMED') {
                      <div style="display: flex; gap: 8px;">
                        <button type="button" class="ui-btn ui-btn-primary" (click)="markComplete(a)" [disabled]="processingId === a.appointmentId">
                          Đã khám
                        </button>
                        <button type="button" class="ui-btn ui-btn-outline" (click)="markNoShow(a)" [disabled]="processingId === a.appointmentId">
                          Không đến
                        </button>
                      </div>
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
export class DoctorAgendaPageComponent implements OnInit {
  private readonly agendaService = inject(AppointmentAgendaService);
  private readonly identityService = inject(IdentityService);
  private readonly apiError = inject(ApiErrorService);
  private readonly toast = inject(ToastService);

  readonly currentIdentity = this.identityService.identity;

  selectedDate = new Date().toISOString().slice(0, 10);
  appointments: AppointmentView[] = [];
  loading = false;
  processingId: number | null = null;
  errorMessage = '';

  ngOnInit(): void {
    this.loadAgenda();
  }

  loadAgenda(): void {
    const doctorId = this.currentIdentity().doctorId ?? 1;
    this.loading = true;
    this.errorMessage = '';
    this.agendaService.getDoctorAgenda(doctorId, this.selectedDate).subscribe({
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

  markComplete(a: AppointmentView): void {
    this.processingId = a.appointmentId;
    this.errorMessage = '';
    this.agendaService.complete(a.appointmentId).subscribe({
      next: () => {
        this.toast.success('Thành công', `Đã đánh dấu hoàn thành lịch #${a.appointmentId}`);
        this.processingId = null;
        this.loadAgenda();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.processingId = null;
      }
    });
  }

  markNoShow(a: AppointmentView): void {
    this.processingId = a.appointmentId;
    this.errorMessage = '';
    this.agendaService.noShow(a.appointmentId).subscribe({
      next: () => {
        this.toast.success('Thành công', `Đã ghi nhận bệnh nhân không đến lịch #${a.appointmentId}`);
        this.processingId = null;
        this.loadAgenda();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.processingId = null;
      }
    });
  }
}

