import { Component, ViewChild } from '@angular/core';

import { PatientListComponent } from '../components/patient-list/patient-list.component';
import { PatientFormComponent } from '../components/patient-form/patient-form.component';
import { Patient } from '../models/patient.model';

// Không có nút xóa: hệ thống không xóa hồ sơ bệnh nhân, hồ sơ trùng được gộp (docs/02 mục 4.1)
@Component({
  selector: 'app-patients-page',
  standalone: true,
  imports: [
    PatientListComponent,
    PatientFormComponent
  ],
  templateUrl: './patients-page.component.html',
  styleUrls: ['./patients-page.component.css']
})
export class PatientsPageComponent {
  @ViewChild('patientList') patientList!: PatientListComponent;
  @ViewChild('patientForm') patientForm!: PatientFormComponent;

  openAddModal(): void {
    this.patientForm.open();
  }

  openEditModal(patient: Patient): void {
    this.patientForm.open(patient);
  }

  onPatientSaved(): void {
    this.patientList.reload();
  }
}
