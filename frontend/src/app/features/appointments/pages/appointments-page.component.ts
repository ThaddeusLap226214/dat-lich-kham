import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppointmentAgendaService } from '../services/appointment-agenda.service';
import { AppointmentView } from '../models/appointment-agenda.model';
import { Doctor } from '../../booking/models/booking.model';
import { IdentityService } from '../../../core/identity.service';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';

@Component({
  selector: 'app-appointments-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent],
  templateUrl: './appointments-page.component.html',
  styleUrls: ['./appointments-page.component.css']
})
export class AppointmentsPageComponent implements OnInit {
  private readonly agendaService = inject(AppointmentAgendaService);
  private readonly identityService = inject(IdentityService);
  private readonly apiError = inject(ApiErrorService);
  private readonly toast = inject(ToastService);

  readonly currentIdentity = this.identityService.identity;

  doctors: Doctor[] = [];
  selectedDoctorId: number = 1;
  selectedDate: string = '';
  appointments: AppointmentView[] = [];

  loading = false;
  processingAction = false;

  // Dialog hủy lịch
  cancelModalOpen = false;
  cancellingAppointmentId: number | null = null;
  cancelReason: string = '';

  ngOnInit(): void {
    const today = new Date();
    this.selectedDate = this.formatDate(today);

    // Nếu người dùng là Bác sĩ, gắn cố định doctorId của họ
    const doctorId = this.currentIdentity().doctorId;
    if (doctorId) {
      this.selectedDoctorId = doctorId;
    }

    this.loadDoctors();
  }

  loadDoctors(): void {
    this.agendaService.getDoctors().subscribe({
      next: (data) => {
        this.doctors = data;
        const doctorId = this.currentIdentity().doctorId;
        if (doctorId) {
          this.selectedDoctorId = doctorId;
        } else if (this.doctors.length > 0 && !this.selectedDoctorId) {
          this.selectedDoctorId = this.doctors[0].id;
        }
        this.loadAppointments();
      },
      error: (err) => {
        this.apiError.show(err);
      }
    });
  }

  loadAppointments(): void {
    if (!this.selectedDoctorId || !this.selectedDate) {
      this.appointments = [];
      return;
    }

    this.loading = true;
    this.agendaService.getDoctorAgenda(this.selectedDoctorId, this.selectedDate).subscribe({
      next: (data) => {
        this.appointments = data;
        this.loading = false;
      },
      error: (err) => {
        this.apiError.show(err);
        this.loading = false;
      }
    });
  }

  onDoctorChange(): void {
    this.loadAppointments();
  }

  onDateChange(): void {
    this.loadAppointments();
  }

  // Thống kê
  get totalCount(): number {
    return this.appointments.length;
  }

  get confirmedCount(): number {
    return this.appointments.filter(a => a.status === 'CONFIRMED').length;
  }

  get completedCount(): number {
    return this.appointments.filter(a => a.status === 'COMPLETED').length;
  }

  get noShowCount(): number {
    return this.appointments.filter(a => a.status === 'NO_SHOW').length;
  }

  get cancelledCount(): number {
    return this.appointments.filter(a => a.status === 'CANCELLED').length;
  }

  // Hành động Hủy lịch
  openCancelModal(appointmentId: number): void {
    this.cancellingAppointmentId = appointmentId;
    this.cancelReason = '';
    this.cancelModalOpen = true;
  }

  closeCancelModal(): void {
    this.cancelModalOpen = false;
    this.cancellingAppointmentId = null;
    this.cancelReason = '';
  }

  submitCancel(): void {
    if (!this.cancellingAppointmentId) return;

    this.processingAction = true;
    this.agendaService.cancel(this.cancellingAppointmentId, this.cancelReason.trim()).subscribe({
      next: () => {
        this.toast.success('Hủy lịch thành công', `Lịch hẹn #${this.cancellingAppointmentId} đã được hủy.`);
        this.processingAction = false;
        this.closeCancelModal();
        this.loadAppointments();
      },
      error: (err) => {
        this.processingAction = false;
        this.apiError.show(err);
      }
    });
  }

  // Hành động Đã khám
  onComplete(appointmentId: number): void {
    this.processingAction = true;
    this.agendaService.complete(appointmentId).subscribe({
      next: () => {
        this.toast.success('Đã cập nhật', `Đã đánh dấu lịch khám #${appointmentId} là ĐÃ KHÁM.`);
        this.processingAction = false;
        this.loadAppointments();
      },
      error: (err) => {
        this.processingAction = false;
        this.apiError.show(err);
      }
    });
  }

  // Hành động Không đến
  onNoShow(appointmentId: number): void {
    this.processingAction = true;
    this.agendaService.noShow(appointmentId).subscribe({
      next: () => {
        this.toast.info('Đã ghi nhận', `Đã ghi nhận bệnh nhân KHÔNG ĐẾN cho lịch #${appointmentId}.`);
        this.processingAction = false;
        this.loadAppointments();
      },
      error: (err) => {
        this.processingAction = false;
        this.apiError.show(err);
      }
    });
  }

  formatTime(isoStr: string): string {
    if (!isoStr) return '';
    const date = new Date(isoStr);
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    return `${h}:${m}`;
  }

  isPastStartTime(startTimeStr: string): boolean {
    if (!startTimeStr) return false;
    return new Date(startTimeStr).getTime() <= new Date().getTime();
  }

  private formatDate(d: Date): string {
    const y = d.getFullYear();
    const m = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
