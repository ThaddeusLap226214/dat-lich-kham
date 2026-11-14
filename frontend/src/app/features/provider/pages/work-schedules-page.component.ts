import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProviderCatalogService } from '../services/provider-catalog.service';
import {
  DAY_OF_WEEK_OPTIONS,
  Doctor,
  DoctorDayOff,
  DoctorDayOffRequest,
  Room,
  WorkSchedule,
  WorkScheduleRequest,
  dayOfWeekLabel
} from '../models/provider.model';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-work-schedules-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmDialogComponent],
  template: `
    <section class="ui-page">
      <header class="ui-page-header">
        <div>
          <h1 class="ui-page-title">Lịch làm việc bác sĩ</h1>
          <p class="ui-page-subtitle">Cấu hình ca làm việc hằng tuần, phòng khám, thời lượng một lượt khám và ngừng ca</p>
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
          <label class="ui-label">Chọn bác sĩ</label>
          <select class="ui-select" [(ngModel)]="selectedDoctorId" (change)="onDoctorChange()">
            @for (d of doctors; track d.id) {
              <option [ngValue]="d.id">{{ d.fullName }} — {{ d.specialtyName }}</option>
            }
          </select>
        </div>
      </div>

      <div class="ui-card" style="margin-bottom: 16px;">
        <h2 style="margin: 0 0 12px; font-size: 1rem;">
          {{ editingScheduleId ? 'Cập nhật ca làm việc #' + editingScheduleId : 'Thêm ca làm việc mới' }}
        </h2>
        <form (ngSubmit)="saveSchedule()" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; align-items: end;">
          <div class="ui-field">
            <label class="ui-label">Thứ *</label>
            <select class="ui-select" [(ngModel)]="form.dayOfWeek" name="dayOfWeek" required>
              @for (d of dayOptions; track d.value) {
                <option [ngValue]="d.value">{{ d.label }}</option>
              }
            </select>
          </div>
          <div class="ui-field">
            <label class="ui-label">Phòng khám *</label>
            <select class="ui-select" [(ngModel)]="form.roomId" name="roomId" required>
              @for (r of rooms; track r.id) {
                <option [ngValue]="r.id">{{ r.name }} ({{ r.code }})</option>
              }
            </select>
          </div>
          <div class="ui-field">
            <label class="ui-label">Giờ bắt đầu *</label>
            <input class="ui-input" type="time" [(ngModel)]="form.startTime" name="startTime" required />
          </div>
          <div class="ui-field">
            <label class="ui-label">Giờ kết thúc *</label>
            <input class="ui-input" type="time" [(ngModel)]="form.endTime" name="endTime" required />
          </div>
          <div class="ui-field">
            <label class="ui-label">Phút / lượt *</label>
            <input class="ui-input" type="number" min="5" max="120" [(ngModel)]="form.slotMinutes" name="slotMinutes" required />
          </div>
          <div class="ui-field">
            <label class="ui-label">Hiệu lực từ *</label>
            <input class="ui-input" type="date" [(ngModel)]="form.effectiveFrom" name="effectiveFrom" required />
          </div>
          <div class="ui-field">
            <label class="ui-label">Hiệu lực đến</label>
            <input class="ui-input" type="date" [(ngModel)]="form.effectiveTo" name="effectiveTo" />
          </div>
          <div style="display: flex; gap: 8px;">
            <button type="submit" class="ui-btn ui-btn-primary" [disabled]="saving || !selectedDoctorId">
              {{ editingScheduleId ? 'Lưu cập nhật' : 'Thêm ca' }}
            </button>
            @if (editingScheduleId) {
              <button type="button" class="ui-btn ui-btn-outline" (click)="resetForm()">Hủy</button>
            }
          </div>
        </form>
      </div>

      @if (loading) {
        <div class="ui-card">Đang tải danh sách ca làm việc...</div>
      } @else if (schedules.length === 0) {
        <div class="ui-card">Bác sĩ chưa có ca làm việc nào.</div>
      } @else {
        <div class="ui-card" style="padding: 0; overflow-x: auto;">
          <table class="ui-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Thứ</th>
                <th>Giờ làm việc</th>
                <th>Phòng khám</th>
                <th>Phút / lượt</th>
                <th>Hiệu lực</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              @for (s of schedules; track s.id) {
                <tr>
                  <td>{{ s.id }}</td>
                  <td><strong>{{ dayLabel(s.dayOfWeek) }}</strong></td>
                  <td>{{ s.startTime.slice(0, 5) }} – {{ s.endTime.slice(0, 5) }}</td>
                  <td>{{ s.roomName }}</td>
                  <td>{{ s.slotMinutes }} phút</td>
                  <td>{{ s.effectiveFrom }} {{ s.effectiveTo ? '→ ' + s.effectiveTo : '→ vô thời hạn' }}</td>
                  <td>{{ s.active ? 'Đang áp dụng' : 'Đã ngừng' }}</td>
                  <td style="display: flex; gap: 8px;">
                    <button type="button" class="ui-btn ui-btn-outline" (click)="editSchedule(s)">Sửa</button>
                    @if (s.active) {
                      <button type="button" class="ui-btn ui-btn-danger" (click)="confirmDeactivate(s)">Ngừng ca</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      <app-confirm-dialog
        [isOpen]="deactivateTarget !== null"
        title="Ngừng ca làm việc"
        [message]="'Ngừng ca #' + (deactivateTarget?.id ?? '') + ' (đặt active = false, không ảnh hưởng lịch hẹn đã đặt)?'"
        confirmText="Ngừng ca"
        (confirm)="performDeactivate()"
        (cancel)="deactivateTarget = null"
      />

      <div class="ui-card" style="margin-top: 20px;">
        <h2 style="margin: 0 0 12px; font-size: 1rem;">Ngày nghỉ / ngày lễ của bác sĩ</h2>
        <form (ngSubmit)="saveDayOff()" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; align-items: end; margin-bottom: 16px;">
          <div class="ui-field">
            <label class="ui-label">Ngày nghỉ *</label>
            <input class="ui-input" type="date" [(ngModel)]="dayOffForm.offDate" name="offDate" required />
          </div>
          <div class="ui-field">
            <label class="ui-label">Lý do</label>
            <input class="ui-input" [(ngModel)]="dayOffForm.reason" name="offReason" maxlength="255" placeholder="VD: Nghỉ phép / Hội thảo" />
          </div>
          <div>
            <button type="submit" class="ui-btn ui-btn-primary" [disabled]="saving || !selectedDoctorId">Thêm ngày nghỉ</button>
          </div>
        </form>

        @if (dayOffs.length === 0) {
          <p>Bác sĩ chưa có ngày nghỉ nào được khai báo.</p>
        } @else {
          <table class="ui-table">
            <thead>
              <tr>
                <th>Ngày nghỉ</th>
                <th>Lý do</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (d of dayOffs; track d.id) {
                <tr>
                  <td><strong>{{ d.offDate }}</strong></td>
                  <td>{{ d.reason || '—' }}</td>
                  <td>
                    <button type="button" class="ui-btn ui-btn-danger" (click)="removeDayOff(d)">Xóa</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
    </section>
  `
})
export class WorkSchedulesPageComponent implements OnInit {
  private readonly catalogService = inject(ProviderCatalogService);
  private readonly apiError = inject(ApiErrorService);
  private readonly toast = inject(ToastService);

  doctors: Doctor[] = [];
  rooms: Room[] = [];
  schedules: WorkSchedule[] = [];
  dayOffs: DoctorDayOff[] = [];
  selectedDoctorId: number | null = null;

  dayOffForm: DoctorDayOffRequest = {
    offDate: new Date().toISOString().slice(0, 10),
    reason: ''
  };

  loading = false;
  saving = false;
  errorMessage = '';

  editingScheduleId: number | null = null;
  deactivateTarget: WorkSchedule | null = null;

  readonly dayOptions = DAY_OF_WEEK_OPTIONS;
  readonly dayLabel = dayOfWeekLabel;

  form: WorkScheduleRequest = {
    roomId: 0,
    dayOfWeek: 1,
    startTime: '07:30',
    endTime: '11:30',
    slotMinutes: 20,
    effectiveFrom: new Date().toISOString().slice(0, 10),
    effectiveTo: null,
    active: true
  };

  ngOnInit(): void {
    this.catalogService.listRooms().subscribe({
      next: (rooms) => {
        this.rooms = rooms.filter(r => r.active);
        if (this.form.roomId === 0 && this.rooms.length > 0) {
          this.form.roomId = this.rooms[0].id;
        }
      },
      error: (err) => (this.errorMessage = this.apiError.show(err).message)
    });

    this.catalogService.listDoctors().subscribe({
      next: (doctors) => {
        this.doctors = doctors;
        if (doctors.length > 0) {
          this.selectedDoctorId = doctors[0].id;
          this.loadSchedules();
        }
      },
      error: (err) => (this.errorMessage = this.apiError.show(err).message)
    });
  }

  onDoctorChange(): void {
    this.resetForm();
    this.loadSchedules();
  }

  loadSchedules(): void {
    if (!this.selectedDoctorId) return;
    this.loading = true;
    this.errorMessage = '';
    this.catalogService.listWorkSchedules(this.selectedDoctorId).subscribe({
      next: (data) => {
        this.schedules = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.loading = false;
      }
    });
    this.catalogService.listDayOffs(this.selectedDoctorId).subscribe({
      next: (data) => (this.dayOffs = data),
      error: (err) => (this.errorMessage = this.apiError.show(err).message)
    });
  }

  editSchedule(s: WorkSchedule): void {
    this.editingScheduleId = s.id;
    this.form = {
      roomId: s.roomId,
      dayOfWeek: s.dayOfWeek,
      startTime: s.startTime.slice(0, 5),
      endTime: s.endTime.slice(0, 5),
      slotMinutes: s.slotMinutes,
      effectiveFrom: s.effectiveFrom,
      effectiveTo: s.effectiveTo ?? null,
      active: s.active
    };
  }

  resetForm(): void {
    this.editingScheduleId = null;
    this.form = {
      roomId: this.rooms[0]?.id ?? 0,
      dayOfWeek: 1,
      startTime: '07:30',
      endTime: '11:30',
      slotMinutes: 20,
      effectiveFrom: new Date().toISOString().slice(0, 10),
      effectiveTo: null,
      active: true
    };
  }

  saveSchedule(): void {
    if (!this.selectedDoctorId) return;
    this.saving = true;
    this.errorMessage = '';
    const req: WorkScheduleRequest = {
      ...this.form,
      effectiveTo: this.form.effectiveTo ? this.form.effectiveTo : null
    };
    const obs = this.editingScheduleId
      ? this.catalogService.updateWorkSchedule(this.editingScheduleId, req)
      : this.catalogService.createWorkSchedule(this.selectedDoctorId, req);

    obs.subscribe({
      next: () => {
        this.toast.success('Thành công', this.editingScheduleId ? 'Đã cập nhật ca làm việc' : 'Đã thêm ca làm việc mới');
        this.saving = false;
        this.resetForm();
        this.loadSchedules();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.saving = false;
      }
    });
  }

  confirmDeactivate(s: WorkSchedule): void {
    this.deactivateTarget = s;
  }

  performDeactivate(): void {
    if (!this.deactivateTarget) return;
    const id = this.deactivateTarget.id;
    this.deactivateTarget = null;
    this.catalogService.deleteWorkSchedule(id).subscribe({
      next: () => {
        this.toast.success('Thành công', 'Đã ngừng ca làm việc');
        this.loadSchedules();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
      }
    });
  }

  saveDayOff(): void {
    if (!this.selectedDoctorId) return;
    this.saving = true;
    this.errorMessage = '';
    this.catalogService.addDayOff(this.selectedDoctorId, {
      offDate: this.dayOffForm.offDate,
      reason: this.dayOffForm.reason?.trim() || null
    }).subscribe({
      next: () => {
        this.toast.success('Thành công', 'Đã thêm ngày nghỉ cho bác sĩ');
        this.saving = false;
        this.dayOffForm = { offDate: new Date().toISOString().slice(0, 10), reason: '' };
        this.loadSchedules();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.saving = false;
      }
    });
  }

  removeDayOff(d: DoctorDayOff): void {
    if (!this.selectedDoctorId) return;
    this.catalogService.deleteDayOff(this.selectedDoctorId, d.id).subscribe({
      next: () => {
        this.toast.success('Thành công', 'Đã xóa ngày nghỉ');
        this.loadSchedules();
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
      }
    });
  }
}

