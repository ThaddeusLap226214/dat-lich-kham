import { Component, EventEmitter, Input, Output, OnInit, inject, DestroyRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { FacilityService } from '../../services/facility.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import {
  Facility,
  FacilityType,
  CreateFacilityRequest,
  UpdateFacilityRequest
} from '../../models/facility.model';
import { FACILITY_TYPE_OPTIONS, FACILITY_STATUS_OPTIONS } from '../../models/facility.constants';
import { phoneValidator } from '../../../../shared/validators/phone.validator';

@Component({
  selector: 'app-facility-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './facility-form.component.html',
  styleUrls: ['./facility-form.component.css']
})
export class FacilityFormComponent implements OnInit {
  @Input() facility: Facility | null = null;
  @Output() saved = new EventEmitter<Facility>();
  @Output() closed = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly facilityService = inject(FacilityService);
  private readonly toastService = inject(ToastService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  form!: FormGroup;
  isSubmitting = false;
  isOpen = false;

  get isEditMode(): boolean {
    return !!this.facility;
  }

  facilityTypeOptions = FACILITY_TYPE_OPTIONS;
  facilityStatusOptions = FACILITY_STATUS_OPTIONS;

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    const facility = this.facility;

    this.form = this.fb.group({
      code: [facility?.code || '', Validators.required],
      name: [facility?.name || '', Validators.required],
      facilityType: [facility?.facilityType || FacilityType.HOSPITAL],
      address: [facility?.address || ''],
      phoneNumber: [facility?.phoneNumber || '', phoneValidator()],
      isActive: [facility?.isActive !== undefined ? facility.isActive : true]
    });
  }

  open(facility?: Facility): void {
    this.facility = facility || null;
    this.isOpen = true;
    this.document.body.classList.add('modal-open');

    if (facility) {
      this.form.patchValue({
        code: facility.code,
        name: facility.name,
        facilityType: facility.facilityType,
        address: facility.address,
        phoneNumber: facility.phoneNumber,
        isActive: facility.isActive
      });
    } else {
      this.form.reset({
        code: '',
        name: '',
        facilityType: FacilityType.HOSPITAL,
        address: '',
        phoneNumber: '',
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
      const request: UpdateFacilityRequest = {
        id: this.facility!.id,
        code: formValue.code,
        name: formValue.name,
        facilityType: formValue.facilityType,
        address: formValue.address || null,
        phoneNumber: formValue.phoneNumber || null,
        isActive: formValue.isActive
      };

      this.facilityService.update(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    } else {
      const request: CreateFacilityRequest = {
        code: formValue.code,
        name: formValue.name,
        facilityType: formValue.facilityType,
        address: formValue.address || null,
        phoneNumber: formValue.phoneNumber || null,
        isActive: formValue.isActive
      };

      this.facilityService.create(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response),
        error: (error) => this.handleError(error)
      });
    }
  }

  private handleSuccess(response: { statusCode: number; data: Facility }): void {
    if (response.statusCode === 200 || response.statusCode === 201) {
      this.toastService.success(
        'Thành công',
        this.isEditMode ? 'Cập nhật thông tin cơ sở y tế thành công' : 'Thêm mới cơ sở y tế thành công'
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
