import { Component, EventEmitter, Output, OnInit, inject, signal, DestroyRef, ChangeDetectionStrategy, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { PageMeta } from '../../../../shared/models';
import { Gender, GENDER_OPTIONS } from '../../../../shared/enums';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { PatientService } from '../../services/patient.service';
import { IdType, Patient, PatientStatus } from '../../models/patient.model';
import { PatientSearchParams, defaultSearchParams } from '../../models/patient-search-params.model';
import { ID_TYPE_OPTIONS, STATUS_OPTIONS } from '../../models/patient.constants';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchService } from '../../../../shared/services/search.service';
import { formatPhoneNumber } from '../../../../shared/validators/phone.validator';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    PaginationComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.css']
})
export class PatientListComponent implements OnInit {
  @Output() addPatient = new EventEmitter<void>();
  @Output() editPatient = new EventEmitter<Patient>();

  private readonly patientService = inject(PatientService);
  private readonly toastService = inject(ToastService);
  readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  patients = signal<Patient[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);

  private searchParams: PatientSearchParams = { ...defaultSearchParams };

  totalCount = signal(0);
  temporaryCount = signal(0);
  activeCount = signal(0);

  statusOptions = STATUS_OPTIONS;
  selectedStatus: string = 'ALL';

  readonly MERGED = PatientStatus.MERGED;

  constructor() {
    effect(() => {
      const query = this.searchService.searchQuery();
      this.searchParams = {
        ...this.searchParams,
        keyword: query || undefined,
        page: 0
      };
      this.loadPatients();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadStats();
  }

  reload(): void {
    this.loadPatients();
    this.loadStats();
  }

  loadPatients(): void {
    this.isLoading.set(true);
    this.patientService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (page) => {
        this.patients.set(page.content);
        this.pageMeta.set({
          page: page.page + 1,
          pageSize: page.size,
          pages: page.totalPages,
          total: page.totalElements
        });
        this.isLoading.set(false);
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Không thể tải danh sách bệnh nhân');
        this.isLoading.set(false);
      }
    });
  }

  // Đếm trên toàn bộ dữ liệu (không theo trang đang xem): mỗi thẻ gọi một lần với size=1 và đọc totalElements
  loadStats(): void {
    forkJoin({
      all: this.patientService.search({ size: 1 }),
      temporary: this.patientService.search({ status: PatientStatus.TEMPORARY, size: 1 }),
      active: this.patientService.search({ status: PatientStatus.ACTIVE, size: 1 })
    }).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: ({ all, temporary, active }) => {
        this.totalCount.set(all.totalElements);
        this.temporaryCount.set(temporary.totalElements);
        this.activeCount.set(active.totalElements);
      },
      error: () => {}
    });
  }

  onPageChange(page: number): void {
    this.searchParams = {
      ...this.searchParams,
      page: page - 1
    };
    this.loadPatients();
  }

  onStatusFilterChange(): void {
    this.searchParams = {
      ...this.searchParams,
      status: this.selectedStatus !== 'ALL' ? this.selectedStatus as PatientStatus : undefined,
      page: 0
    };
    this.loadPatients();
  }

  resetFilters(): void {
    this.selectedStatus = 'ALL';
    this.searchService.clearQuery();
    this.searchParams = {
      ...defaultSearchParams,
      page: 0
    };
    this.reload();
  }

  onAddPatient(): void {
    this.addPatient.emit();
  }

  onEditPatient(patient: Patient): void {
    this.editPatient.emit(patient);
  }

  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].substring(0, 2).toUpperCase();
  }

  getAvatarColor(gender: Gender): string {
    switch (gender) {
      case Gender.MALE: return 'male';
      case Gender.FEMALE: return 'female';
      default: return 'other';
    }
  }

  getGenderLabel(gender: Gender): string {
    return GENDER_OPTIONS.find(o => o.value === gender)?.label || gender;
  }

  getStatusLabel(status: PatientStatus): string {
    return STATUS_OPTIONS.find(o => o.value === status)?.label || status;
  }

  getIdTypeLabel(idType: IdType | null): string {
    return ID_TYPE_OPTIONS.find(o => o.value === idType)?.label || '';
  }

  percent(count: number): number {
    return this.totalCount() > 0 ? count / this.totalCount() * 100 : 0;
  }

  formatPhone = formatPhoneNumber;
}
