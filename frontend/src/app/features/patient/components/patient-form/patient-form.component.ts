import { Component, EventEmitter, Input, Output, OnInit, inject, DestroyRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { PatientService } from '../../services/patient.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { Gender, GENDER_OPTIONS } from '../../../../shared/enums';
import {
  CreatePatientResponse,
  Patient,
  PatientRequest
} from '../../models/patient.model';
import { ID_TYPE_OPTIONS } from '../../models/patient.constants';
import { phoneValidator } from '../../../../shared/validators/phone.validator';

const CREATE_MESSAGES: Record<CreatePatientResponse['outcome'], string> = {
  REUSED_EXISTING: 'Đã có hồ sơ trùng giấy tờ, dùng lại hồ sơ cũ',
  CREATED_ACTIVE: 'Đã tạo hồ sơ (đã xác minh)',
  CREATED_TEMPORARY: 'Đã tạo hồ sơ tạm'
};

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './patient-form.component.html',
  styleUrls: ['./patient-form.component.css']
})
export class PatientFormComponent implements OnInit {
  @Input() patient: Patient | null = null;
  @Output() saved = new EventEmitter<Patient>();
  @Output() closed = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly toastService = inject(ToastService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  form!: FormGroup;
  isSubmitting = false;
  isOpen = false;

  get isEditMode(): boolean {
    return !!this.patient;
  }

  genderOptions = GENDER_OPTIONS;
  idTypeOptions = ID_TYPE_OPTIONS;

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.form = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(255)]],
      dateOfBirth: ['', Validators.required],
      gender: [Gender.MALE, Validators.required],
      phone: ['', phoneValidator()],
      email: ['', Validators.email],
      address: [''],
      idType: [''],
      idNumber: ['', Validators.maxLength(20)],
      guardianName: [''],
      guardianPhone: ['', phoneValidator()],
      guardianRelation: ['']
    });
  }

  open(patient?: Patient): void {
    this.patient = patient || null;
    this.isOpen = true;
    this.document.body.classList.add('modal-open');

    this.form.reset({
      fullName: patient?.fullName ?? '',
      dateOfBirth: patient?.dateOfBirth ?? '',
      gender: patient?.gender ?? Gender.MALE,
      phone: patient?.phone ?? '',
      email: patient?.email ?? '',
      address: patient?.address ?? '',
      idType: patient?.idType ?? '',
      idNumber: patient?.idNumber ?? '',
      guardianName: patient?.guardianName ?? '',
      guardianPhone: patient?.guardianPhone ?? '',
      guardianRelation: patient?.guardianRelation ?? ''
    });

    // Backend không cho đổi giấy tờ qua PUT (phải qua luồng bổ sung giấy tờ để kiểm tra trùng)
    if (patient) {
      this.form.get('idType')?.disable();
      this.form.get('idNumber')?.disable();
    } else {
      this.form.get('idType')?.enable();
      this.form.get('idNumber')?.enable();
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
    const v = this.form.getRawValue();
    const request: PatientRequest = {
      fullName: v.fullName.trim(),
      dateOfBirth: v.dateOfBirth,
      gender: v.gender,
      phone: v.phone || null,
      email: v.email || null,
      address: v.address || null,
      idType: v.idNumber ? (v.idType || null) : null,
      idNumber: v.idNumber || null,
      guardianName: v.guardianName || null,
      guardianPhone: v.guardianPhone || null,
      guardianRelation: v.guardianRelation || null
    };

    if (this.isEditMode) {
      this.patientService.update(this.patient!.id, request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (patient) => this.handleSuccess(patient, 'Cập nhật thông tin bệnh nhân thành công'),
        error: (error) => this.handleError(error)
      });
    } else {
      this.patientService.create(request).pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe({
        next: (response) => this.handleSuccess(response.patient, CREATE_MESSAGES[response.outcome]
          + (response.duplicateQueued ? '; đã đưa vào hàng chờ duyệt trùng' : '')),
        error: (error) => this.handleError(error)
      });
    }
  }

  private handleSuccess(patient: Patient, message: string): void {
    this.toastService.success('Thành công', message);
    this.saved.emit(patient);
    this.close();
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
