import { Component, ViewChild, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { FacilityListComponent } from '../components/facility-list/facility-list.component';
import { FacilityFormComponent } from '../components/facility-form/facility-form.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FacilityService } from '../services/facility.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { Facility } from '../models/facility.model';
import { ApiResponse } from '../../../shared/models';

@Component({
  selector: 'app-facilities-page',
  standalone: true,
  imports: [
    FacilityListComponent,
    FacilityFormComponent,
    ConfirmDialogComponent
  ],
  templateUrl: './facilities-page.component.html',
  styleUrls: ['./facilities-page.component.css']
})
export class FacilitiesPageComponent {
  @ViewChild('facilityList') facilityList!: FacilityListComponent;
  @ViewChild('facilityForm') facilityForm!: FacilityFormComponent;

  private readonly facilityService = inject(FacilityService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  isDeleteDialogOpen = false;
  deleteMessage = '';
  facilityToDelete: Facility | null = null;

  openAddModal(): void {
    this.facilityForm.open();
  }

  openEditModal(facility: Facility): void {
    this.facilityForm.open(facility);
  }

  openDeleteConfirm(facility: Facility): void {
    this.facilityToDelete = facility;
    this.deleteMessage = `Bạn có chắc chắn muốn xóa cơ sở y tế "${facility.name}" không? Hành động này không thể hoàn tác và toàn bộ dữ liệu liên quan sẽ bị loại bỏ khỏi hệ thống.`;
    this.isDeleteDialogOpen = true;
  }

  onConfirmDelete(): void {
    if (!this.facilityToDelete) return;

    this.facilityService.delete(this.facilityToDelete.id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response: ApiResponse<void>) => {
        if (response.statusCode === 200) {
          this.toastService.success('Thành công', 'Xóa cơ sở y tế thành công');
          this.closeDeleteDialog();
          this.facilityList.reload();
        }
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Đã xảy ra lỗi khi xóa cơ sở y tế');
        this.closeDeleteDialog();
      }
    });
  }

  onCancelDelete(): void {
    this.closeDeleteDialog();
  }

  private closeDeleteDialog(): void {
    this.isDeleteDialogOpen = false;
    this.facilityToDelete = null;
  }

  onFacilitySaved(): void {
    this.facilityList.reload();
  }
}
