import { Component, ViewChild, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SourceSystemListComponent } from '../components/source-system-list/source-system-list.component';
import { SourceSystemFormComponent } from '../components/source-system-form/source-system-form.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { SourceSystemService } from '../services/source-system.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { SourceSystem } from '../models/source-system.model';
import { ApiResponse } from '../../../shared/models';

@Component({
  selector: 'app-source-systems-page',
  standalone: true,
  imports: [
    SourceSystemListComponent,
    SourceSystemFormComponent,
    ConfirmDialogComponent
  ],
  templateUrl: './source-systems-page.component.html',
  styleUrls: ['./source-systems-page.component.css']
})
export class SourceSystemsPageComponent {
  @ViewChild('sourceSystemList') sourceSystemList!: SourceSystemListComponent;
  @ViewChild('sourceSystemForm') sourceSystemForm!: SourceSystemFormComponent;

  private readonly sourceSystemService = inject(SourceSystemService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  isDeleteDialogOpen = false;
  deleteMessage = '';
  sourceSystemToDelete: SourceSystem | null = null;

  openAddModal(): void {
    this.sourceSystemForm.open();
  }

  openEditModal(sourceSystem: SourceSystem): void {
    this.sourceSystemForm.open(sourceSystem);
  }

  openDeleteConfirm(sourceSystem: SourceSystem): void {
    this.sourceSystemToDelete = sourceSystem;
    this.deleteMessage = `Bạn có chắc chắn muốn xóa hệ thống nguồn "${sourceSystem.name}" (${sourceSystem.code}) không? Hành động này không thể hoàn tác và toàn bộ dữ liệu liên quan sẽ bị loại bỏ khỏi hệ thống.`;
    this.isDeleteDialogOpen = true;
  }

  onConfirmDelete(): void {
    if (!this.sourceSystemToDelete) return;

    this.sourceSystemService.delete(this.sourceSystemToDelete.id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response: ApiResponse<void>) => {
        if (response.statusCode === 200) {
          this.toastService.success('Thành công', 'Xóa hệ thống nguồn thành công');
          this.closeDeleteDialog();
          this.sourceSystemList.reload();
        }
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Đã xảy ra lỗi khi xóa hệ thống nguồn');
        this.closeDeleteDialog();
      }
    });
  }

  onCancelDelete(): void {
    this.closeDeleteDialog();
  }

  private closeDeleteDialog(): void {
    this.isDeleteDialogOpen = false;
    this.sourceSystemToDelete = null;
  }

  onSourceSystemSaved(): void {
    this.sourceSystemList.reload();
  }
}
