import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { PatientService } from '../services/patient.service';
import { Patient, PatientStatus } from '../models/patient.model';
import { PatientFormComponent } from '../components/patient-form/patient-form.component';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';

@Component({
  selector: 'app-my-patients-page',
  standalone: true,
  imports: [CommonModule, PatientFormComponent, StatusBadgeComponent],
  templateUrl: './my-patients-page.component.html',
  styleUrls: ['./my-patients-page.component.css']
})
export class MyPatientsPageComponent implements OnInit {
  private readonly patientService = inject(PatientService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  patients = signal<Patient[]>([]);
  isLoading = signal(true);
  selectedPatient: Patient | null = null;
  isFormOpen = false;

  readonly ACTIVE = PatientStatus.ACTIVE;
  readonly TEMPORARY = PatientStatus.TEMPORARY;

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.isLoading.set(true);
    this.patientService.mine().subscribe({
      next: (data) => {
        this.patients.set(data || []);
        this.isLoading.set(false);
      },
      error: (err: unknown) => {
        console.error('Failed to load my patients', err);
        this.toastService.error('Lỗi', 'Không thể tải danh sách hồ sơ của bạn');
        this.isLoading.set(false);
      }
    });
  }

  openAddModal(): void {
    this.selectedPatient = null;
    this.isFormOpen = true;
  }

  openEditModal(patient: Patient): void {
    this.selectedPatient = patient;
    this.isFormOpen = true;
  }

  onFormSaved(): void {
    this.isFormOpen = false;
    this.loadPatients();
  }

  onFormClosed(): void {
    this.isFormOpen = false;
  }

  bookAppointment(patient: Patient): void {
    this.router.navigate(['/booking'], { queryParams: { patientId: patient.id } });
  }
}
