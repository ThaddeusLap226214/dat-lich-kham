import { Component, EventEmitter, Output, OnInit, inject, signal, DestroyRef, ChangeDetectionStrategy, effect } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { PageMeta } from '../../../../shared/models';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FacilityService } from '../../services/facility.service';
import { Facility, FacilityType } from '../../models/facility.model';
import { FacilitySearchParams, defaultSearchParams } from '../../models/facility-search-params.model';
import { FACILITY_TYPE_OPTIONS } from '../../models/facility.constants';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { SearchService } from '../../../../shared/services/search.service';
import { formatPhoneNumber } from '../../../../shared/validators/phone.validator';

@Component({
  selector: 'app-facility-list',
  standalone: true,
  imports: [
    DecimalPipe,
    FormsModule,
    PaginationComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './facility-list.component.html',
  styleUrls: ['./facility-list.component.css']
})
export class FacilityListComponent implements OnInit {
  @Output() addFacility = new EventEmitter<void>();
  @Output() editFacility = new EventEmitter<Facility>();
  @Output() deleteFacility = new EventEmitter<Facility>();

  private readonly facilityService = inject(FacilityService);
  private readonly toastService = inject(ToastService);
  readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  facilities = signal<Facility[]>([]);
  pageMeta = signal<PageMeta | null>(null);
  isLoading = signal(false);

  selectedType: FacilityType | 'ALL' = 'ALL';
  selectedStatus: string = 'ALL';

  private searchParams: FacilitySearchParams = { ...defaultSearchParams };

  activeCount = signal(0);
  totalCount = signal(0);

  facilityTypeOptions = FACILITY_TYPE_OPTIONS;

  constructor() {
    effect(() => {
      const query = this.searchService.searchQuery();
      this.searchParams = {
        ...this.searchParams,
        name: query || undefined,
        code: query || undefined,
        page: 0
      };
      this.loadFacilities();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.loadFacilities();
  }

  reload(): void {
    this.loadFacilities();
  }

  loadFacilities(): void {
    this.isLoading.set(true);
    this.facilityService.search(this.searchParams).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.facilities.set(response.data.result);
          this.pageMeta.set(response.data.meta);

          this.totalCount.set(response.data.meta.total);
          this.activeCount.set(
            response.data.result.filter((f: Facility) => f.isActive).length
          );
        }
        this.isLoading.set(false);
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Không thể tải danh sách cơ sở y tế');
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(page: number): void {
    this.searchParams = {
      ...this.searchParams,
      page: page - 1
    };
    this.loadFacilities();
  }

  onTypeFilterChange(): void {
    this.searchParams = {
      ...this.searchParams,
      facilityType: this.selectedType !== 'ALL' ? this.selectedType as FacilityType : undefined,
      page: 0
    };
    this.loadFacilities();
  }

  onStatusFilterChange(): void {
    this.searchParams = {
      ...this.searchParams,
      isActive: this.selectedStatus === 'ACTIVE' ? true : this.selectedStatus === 'INACTIVE' ? false : undefined,
      page: 0
    };
    this.loadFacilities();
  }

  resetFilters(): void {
    this.selectedType = 'ALL';
    this.selectedStatus = 'ALL';
    this.searchService.clearQuery();
    this.searchParams = {
      ...defaultSearchParams,
      page: 0
    };
    this.loadFacilities();
  }

  onAddFacility(): void {
    this.addFacility.emit();
  }

  onEditFacility(facility: Facility): void {
    this.editFacility.emit(facility);
  }

  onDeleteFacility(facility: Facility): void {
    this.deleteFacility.emit(facility);
  }

  getFacilityTypeLabel(type: FacilityType): string {
    return FACILITY_TYPE_OPTIONS.find(o => o.value === type)?.label || type;
  }

  getFacilityTypeClass(type: FacilityType): string {
    switch (type) {
      case FacilityType.HOSPITAL: return 'type-hospital';
      case FacilityType.CLINIC: return 'type-clinic';
      case FacilityType.OTHER: return 'type-other';
      default: return '';
    }
  }

  formatPhone = formatPhoneNumber;
}
