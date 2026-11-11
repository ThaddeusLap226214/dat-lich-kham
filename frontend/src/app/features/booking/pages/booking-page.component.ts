import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BookingService } from '../services/booking.service';
import { Specialty, Doctor, Slot, AppointmentResponse } from '../models/booking.model';
import { Patient } from '../../patient/models/patient.model';
import { IdentityService } from '../../../core/identity.service';
import { ApiErrorService } from '../../../shared/ui/api-error.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';

@Component({
  selector: 'app-booking-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent],
  templateUrl: './booking-page.component.html',
  styleUrls: ['./booking-page.component.css']
})
export class BookingPageComponent implements OnInit, OnDestroy {
  private readonly bookingService = inject(BookingService);
  private readonly identityService = inject(IdentityService);
  private readonly apiError = inject(ApiErrorService);
  private readonly toast = inject(ToastService);

  readonly currentIdentity = this.identityService.identity;

  specialties: Specialty[] = [];
  doctors: Doctor[] = [];
  slots: Slot[] = [];
  myPatients: Patient[] = [];

  selectedSpecialtyId: number | null = null;
  selectedDoctorId: number | null = null;
  selectedDate: string = '';
  selectedSlot: Slot | null = null;
  selectedPatientId: number | null = null;
  reason: string = '';

  loadingSpecialties = false;
  loadingDoctors = false;
  loadingSlots = false;
  submittingHold = false;
  submittingConfirm = false;

  // Trạng thái giữ chỗ
  heldAppointment: AppointmentResponse | null = null;
  countdownSeconds = 0;
  countdownDisplay = '';
  private timerInterval: any = null;

  // Lịch đã xác nhận xong
  confirmedAppointment: AppointmentResponse | null = null;

  ngOnInit(): void {
    // Mặc định chọn ngày mai
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.selectedDate = this.formatDate(tomorrow);

    this.loadSpecialties();
    this.loadDoctors();
    this.loadPatients();
  }

  ngOnDestroy(): void {
    this.clearCountdown();
  }

  loadSpecialties(): void {
    this.loadingSpecialties = true;
    this.bookingService.getSpecialties().subscribe({
      next: (data) => {
        this.specialties = data;
        this.loadingSpecialties = false;
      },
      error: (err) => {
        this.apiError.show(err);
        this.loadingSpecialties = false;
      }
    });
  }

  loadDoctors(): void {
    this.loadingDoctors = true;
    this.bookingService.getDoctors(this.selectedSpecialtyId ?? undefined).subscribe({
      next: (data) => {
        this.doctors = data;
        this.loadingDoctors = false;
        if (this.doctors.length > 0 && !this.selectedDoctorId) {
          this.selectedDoctorId = this.doctors[0].id;
          this.loadSlots();
        }
      },
      error: (err) => {
        this.apiError.show(err);
        this.loadingDoctors = false;
      }
    });
  }

  onSpecialtyChange(): void {
    this.selectedDoctorId = null;
    this.selectedSlot = null;
    this.slots = [];
    this.loadDoctors();
  }

  onDoctorChange(): void {
    this.selectedSlot = null;
    this.loadSlots();
  }

  onDateChange(): void {
    this.selectedSlot = null;
    this.loadSlots();
  }

  loadSlots(): void {
    if (!this.selectedDoctorId || !this.selectedDate) {
      this.slots = [];
      return;
    }
    this.loadingSlots = true;
    this.bookingService.getSlots(this.selectedDoctorId, this.selectedDate).subscribe({
      next: (data) => {
        this.slots = data;
        this.loadingSlots = false;
        // Nếu slot đang chọn đã bị chiếm, hủy chọn
        if (this.selectedSlot) {
          const match = this.slots.find(s => s.startTime === this.selectedSlot?.startTime);
          if (!match || !match.available) {
            this.selectedSlot = null;
          }
        }
      },
      error: (err) => {
        this.apiError.show(err);
        this.loadingSlots = false;
      }
    });
  }

  loadPatients(): void {
    if (this.currentIdentity().role === 'PATIENT') {
      this.bookingService.getMyPatients().subscribe({
        next: (data) => {
          this.myPatients = data;
          if (this.myPatients.length > 0 && !this.selectedPatientId) {
            this.selectedPatientId = this.myPatients[0].id;
          }
        }
      });
    } else {
      // Lễ tân: mặc định hồ sơ 1
      this.selectedPatientId = 1;
    }
  }

  selectSlot(slot: Slot): void {
    if (!slot.available || this.heldAppointment) {
      return;
    }
    this.selectedSlot = slot;
  }

  onHold(): void {
    if (!this.selectedDoctorId || !this.selectedSlot || !this.selectedPatientId) {
      this.toast.warning('Thiếu thông tin', 'Vui lòng chọn bác sĩ, khung giờ và hồ sơ bệnh nhân.');
      return;
    }

    this.submittingHold = true;
    this.bookingService.hold({
      doctorId: this.selectedDoctorId,
      patientId: Number(this.selectedPatientId),
      startTime: this.selectedSlot.startTime,
      reason: this.reason.trim() || undefined
    }).subscribe({
      next: (res) => {
        this.submittingHold = false;
        this.heldAppointment = res;
        this.toast.success('Giữ chỗ thành công', 'Bạn có 5 phút để xác nhận đặt lịch khám.');
        this.startCountdown(res.holdExpiresAt);
        // Tải lại slot để hiển thị slot vừa giữ đã khóa
        this.loadSlots();
      },
      error: (err) => {
        this.submittingHold = false;
        if (this.apiError.is(err, 'SLOT_ALREADY_HELD')) {
          this.toast.warning('Khung giờ đã bị giữ', 'Khung giờ này vừa có người giữ chỗ. Danh sách giờ đã được cập nhật.');
          this.selectedSlot = null;
          this.loadSlots();
        } else {
          this.apiError.show(err);
        }
      }
    });
  }

  onConfirm(): void {
    if (!this.heldAppointment) return;

    this.submittingConfirm = true;
    this.bookingService.confirm(this.heldAppointment.id).subscribe({
      next: (res) => {
        this.submittingConfirm = false;
        this.clearCountdown();
        this.confirmedAppointment = res;
        this.heldAppointment = null;
        this.toast.success('Đặt lịch thành công', `Lịch khám #${res.id} đã được xác nhận.`);
      },
      error: (err) => {
        this.submittingConfirm = false;
        if (this.apiError.is(err, 'HOLD_EXPIRED')) {
          this.toast.error('Hết hạn giữ chỗ', 'Thời gian giữ chỗ 5 phút đã qua. Vui lòng chọn và giữ lại slot.');
          this.heldAppointment = null;
          this.clearCountdown();
          this.loadSlots();
        } else {
          this.apiError.show(err);
        }
      }
    });
  }

  resetBooking(): void {
    this.clearCountdown();
    this.heldAppointment = null;
    this.confirmedAppointment = null;
    this.selectedSlot = null;
    this.reason = '';
    this.loadSlots();
  }

  private startCountdown(expiresAtStr?: string): void {
    this.clearCountdown();
    if (!expiresAtStr) return;

    const expiresAt = new Date(expiresAtStr).getTime();
    const update = () => {
      const now = new Date().getTime();
      const diff = Math.max(0, Math.floor((expiresAt - now) / 1000));
      this.countdownSeconds = diff;
      const mins = Math.floor(diff / 60);
      const secs = diff % 60;
      this.countdownDisplay = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;

      if (diff <= 0) {
        this.clearCountdown();
        this.toast.warning('Hết hạn giữ chỗ', 'Khung giờ giữ chỗ đã hết hạn. Slot đã được nhả cho người khác.');
        this.heldAppointment = null;
        this.loadSlots();
      }
    };

    update();
    this.timerInterval = setInterval(update, 1000);
  }

  private clearCountdown(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  formatTime(isoStr: string): string {
    if (!isoStr) return '';
    const date = new Date(isoStr);
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    return `${h}:${m}`;
  }

  getSelectedDoctorName(): string {
    const doc = this.doctors.find(d => d.id === this.selectedDoctorId);
    return doc ? doc.fullName : `Bác sĩ #${this.selectedDoctorId}`;
  }

  private formatDate(d: Date): string {
    const y = d.getFullYear();
    const m = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
