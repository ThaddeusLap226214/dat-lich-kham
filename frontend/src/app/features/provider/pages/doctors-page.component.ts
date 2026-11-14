import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProviderCatalogService } from '../services/provider-catalog.service';
import { Doctor, Specialty, WorkSchedule, dayOfWeekLabel } from '../models/provider.model';
import { ApiErrorService } from '../../../shared/ui/api-error.service';

@Component({
  selector: 'app-doctors-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="ui-page">
      <header class="ui-page-header">
        <div>
          <h1 class="ui-page-title">Chuyên khoa &amp; bác sĩ</h1>
          <p class="ui-page-subtitle">Tra cứu chuyên khoa, đội ngũ bác sĩ và lịch làm việc hàng tuần tại phòng khám</p>
        </div>
        <div class="ui-page-actions">
          <a class="ui-btn ui-btn-primary" routerLink="/booking">
            <span class="material-symbols-outlined">event_available</span> Đặt lịch khám
          </a>
        </div>
      </header>

      @if (errorMessage) {
        <div class="ui-alert ui-alert-danger">
          <span class="material-symbols-outlined">error</span>
          <div class="ui-alert-content">{{ errorMessage }}</div>
        </div>
      }

      <div class="ui-toolbar">
        <div class="ui-field">
          <label class="ui-label">Lọc theo chuyên khoa</label>
          <select class="ui-select" [(ngModel)]="selectedSpecialtyId" (change)="loadDoctors()">
            <option [ngValue]="null">-- Tất cả chuyên khoa --</option>
            @for (spec of specialties; track spec.id) {
              <option [ngValue]="spec.id">{{ spec.name }} ({{ spec.code }})</option>
            }
          </select>
        </div>
      </div>

      @if (specialties.length > 0) {
        <div class="ui-card" style="margin-bottom: 16px;">
          <h2 style="margin: 0 0 10px; font-size: 1rem;">Danh mục chuyên khoa ({{ specialties.length }})</h2>
          <div style="display: flex; flex-wrap: wrap; gap: 8px;">
            <button
              type="button"
              class="ui-btn"
              [class.ui-btn-primary]="selectedSpecialtyId === null"
              [class.ui-btn-outline]="selectedSpecialtyId !== null"
              (click)="selectSpecialty(null)">
              Tất cả
            </button>
            @for (spec of specialties; track spec.id) {
              <button
                type="button"
                class="ui-btn"
                [class.ui-btn-primary]="selectedSpecialtyId === spec.id"
                [class.ui-btn-outline]="selectedSpecialtyId !== spec.id"
                (click)="selectSpecialty(spec.id)"
                [title]="spec.description || spec.name">
                {{ spec.name }}
              </button>
            }
          </div>
        </div>
      }

      @if (loadingDoctors) {
        <div class="ui-card">Đang tải danh sách bác sĩ...</div>
      } @else if (doctors.length === 0) {
        <div class="ui-card">Không có bác sĩ nào đang hoạt động cho chuyên khoa này.</div>
      } @else {
        <div class="ui-card" style="padding: 0; overflow-x: auto;">
          <table class="ui-table">
            <thead>
              <tr>
                <th>Bác sĩ</th>
                <th>Học hàm / Chức danh</th>
                <th>Chuyên khoa</th>
                <th>Liên hệ</th>
                <th style="width: 220px;">Lịch làm việc</th>
              </tr>
            </thead>
            <tbody>
              @for (doc of doctors; track doc.id) {
                <tr>
                  <td><strong>{{ doc.fullName }}</strong></td>
                  <td>{{ doc.title || 'Bác sĩ' }}</td>
                  <td>{{ doc.specialtyName }}</td>
                  <td>
                    @if (doc.phone) { <div>{{ doc.phone }}</div> }
                    @if (doc.email) { <div style="font-size: 0.85em; opacity: 0.8;">{{ doc.email }}</div> }
                  </td>
                  <td>
                    <button type="button" class="ui-btn ui-btn-outline" (click)="toggleSchedules(doc)">
                      <span class="material-symbols-outlined">schedule</span>
                      {{ selectedDoctor?.id === doc.id ? 'Ẩn ca làm việc' : 'Xem ca làm việc' }}
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (selectedDoctor) {
        <div class="ui-card" style="margin-top: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
            <h2 style="margin: 0; font-size: 1.05rem;">
              Lịch làm việc trong tuần — {{ selectedDoctor.fullName }} ({{ selectedDoctor.specialtyName }})
            </h2>
            <button type="button" class="ui-btn ui-btn-outline" (click)="selectedDoctor = null">Đóng</button>
          </div>

          @if (loadingSchedules) {
            <p>Đang tải lịch làm việc...</p>
          } @else if (activeSchedules.length === 0) {
            <p>Bác sĩ chưa có ca làm việc nào đang hoạt động.</p>
          } @else {
            <table class="ui-table">
              <thead>
                <tr>
                  <th>Thứ</th>
                  <th>Giờ làm việc</th>
                  <th>Phòng khám</th>
                  <th>Thời lượng / lượt</th>
                  <th>Hiệu lực</th>
                </tr>
              </thead>
              <tbody>
                @for (s of activeSchedules; track s.id) {
                  <tr>
                    <td><strong>{{ dayLabel(s.dayOfWeek) }}</strong></td>
                    <td>{{ s.startTime.slice(0, 5) }} – {{ s.endTime.slice(0, 5) }}</td>
                    <td>{{ s.roomName }}</td>
                    <td>{{ s.slotMinutes }} phút</td>
                    <td>
                      Từ {{ s.effectiveFrom }}
                      @if (s.effectiveTo) { đến {{ s.effectiveTo }} }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
      }
    </section>
  `
})
export class DoctorsPageComponent implements OnInit {
  private readonly catalogService = inject(ProviderCatalogService);
  private readonly apiError = inject(ApiErrorService);

  specialties: Specialty[] = [];
  doctors: Doctor[] = [];
  selectedSpecialtyId: number | null = null;
  selectedDoctor: Doctor | null = null;
  schedules: WorkSchedule[] = [];

  loadingDoctors = false;
  loadingSchedules = false;
  errorMessage = '';

  readonly dayLabel = dayOfWeekLabel;

  get activeSchedules(): WorkSchedule[] {
    return this.schedules.filter(s => s.active);
  }

  ngOnInit(): void {
    this.catalogService.listSpecialties().subscribe({
      next: (data) => (this.specialties = data),
      error: (err) => (this.errorMessage = this.apiError.show(err).message)
    });
    this.loadDoctors();
  }

  selectSpecialty(specialtyId: number | null): void {
    this.selectedSpecialtyId = specialtyId;
    this.loadDoctors();
  }

  loadDoctors(): void {
    this.loadingDoctors = true;
    this.errorMessage = '';
    this.selectedDoctor = null;
    this.catalogService.listDoctors(this.selectedSpecialtyId).subscribe({
      next: (data) => {
        this.doctors = data;
        this.loadingDoctors = false;
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.loadingDoctors = false;
      }
    });
  }

  toggleSchedules(doctor: Doctor): void {
    if (this.selectedDoctor?.id === doctor.id) {
      this.selectedDoctor = null;
      return;
    }
    this.selectedDoctor = doctor;
    this.loadingSchedules = true;
    this.schedules = [];
    this.catalogService.listWorkSchedules(doctor.id).subscribe({
      next: (data) => {
        this.schedules = data;
        this.loadingSchedules = false;
      },
      error: (err) => {
        this.errorMessage = this.apiError.show(err).message;
        this.loadingSchedules = false;
      }
    });
  }
}

