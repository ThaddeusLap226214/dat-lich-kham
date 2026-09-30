import { Component, EventEmitter, Input, Output, OnInit, inject, DestroyRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { SourceSystemService } from '../../services/source-system.service';
import { FacilityService } from '../../../facility/services/facility.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import {
  SourceSystem,
  CreateSourceSystemRequest,
  UpdateSourceSystemRequest
} from '../../models/source-system.model';
import { Facility } from '../../../facility/models/facility.model';

@Component({
  selector: 'app-source-system-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './source-system-form.component.html',
  styleUrls: ['./source-system-form.component.css']
})
export class SourceSystemFormComponent implements OnInit {
  @Input() sourceSystem: SourceSystem | null = null;
  @Output() saved = new EventEmitter<SourceSystem>();
  @Output() closed = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly sourceSystemService = inject(SourceSystemService);
  private readonly facilityService = inject(FacilityService);
  private readonly toastService = inject(ToastService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  form!: FormGroup;
  isSubmitting = false;
  isOpen = false;
  facilities: Facility[] = [];

  get isEditMode(): boolean {
    return !!this.sourceSystem;
  }

  ngOnInit(): void {
    this.initForm();
    this.loadFacilities();
  }

  private initForm(): void {
    const sourceSystem = this.sourceSystem;

    this.form = this.fb.group({
      facilityId: [sourceSystem?.facilityId || '', Validators.required],
      code: [sourceSystem?.code || '', Validators.required],
      name: [sourceSystem?.name || '', Validators.required],
      description: [sourceSystem?.description || ''],
      isActive: [sourceSystem?.isActive !== undefined ? sourceSystem.isActive : true]
    });
  }

  private loadFacilities(): void {
    this.facilityService.search({ page: 0, size: 100 }).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (response) => {
        if (response.statusCode === 200) {
          this.facilities = response.data.result;
        }
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error('Lỗi hệ thống', error?.error?.message || 'Không thể tải danh sách cơ sở y tế');
      }
    });
  }

  open(sourceSystem?: SourceSystem): void {
    this.sourceSystem = sourceSystem || null;
    this.isOpen = true;
    this.document.body.classList.add('modal-open');

    if (sourceSystem) {
      this.form.patchValue({
        facilityId: sourceSystem.facilityId,
        code: sourceSystem.code,
        name: sourceSystem.name,
        description: sourceSystem.description,
        isActive: sourceSystem.isActive
      });
    } else {
      this.form.reset({
        facilityId: '',
        code: '',
        name: '',
        description: '',
        isActive: true
      });
    }
  }

  close(): void {
    this.isOpen = false;
    this.document.body.classList.remove('modal-open');
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close();
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formValue = this.form.value;

    if (this.isEditMode) {
      const request: UpdateSourceSystemRequest = {
        id: this.sourceSystem!.id,
        facilityId: formValue.facilityId,
        code: formValue.code,
        name: formValue.name,
        description: formValue.description || null,
        isActive: formValue.isActive
      };

      this.sourceSystemService.update(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    } else {
      const request: CreateSourceSystemRequest = {
        facilityId: formValue.facilityId,
        code: formValue.code,
        name: formValue.name,
        description: formValue.description || null,
        isActive: formValue.isActive
      };

      this.sourceSystemService.create(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    }
  }

  private handleSuccess(response: { statusCode: number; data: SourceSystem }): void {
    if (response.statusCode === 200 || response.statusCode === 201) {
      this.toastService.success(
        'Thành công',
        this.isEditMode ? 'Cập nhật thông tin hệ thống nguồn thành công' : 'Thêm mới hệ thống nguồn thành công'
      );
      this.saved.emit(response.data);
      this.close();
    }
    this.isSubmitting = false;
  }

  private handleError(error: { error?: { message?: string } }): void {
    this.toastService.error(
      'Lỗi hệ thống',
      error?.error?.message || 'Đã xảy ra lỗi. Vui lòng thử lại.'
    );
    this.isSubmitting = false;
  }
}
