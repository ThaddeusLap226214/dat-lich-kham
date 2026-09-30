import { Component, EventEmitter, Output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Gender, GENDER_OPTIONS } from '../../../../shared/enums';
import { PatientStatus } from '../../models/patient.model';
import { STATUS_OPTIONS } from '../../models/patient.constants';

@Component({
  selector: 'app-patient-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './patient-filter-panel.component.html',
  styleUrls: ['./patient-filter-panel.component.css']
})
export class PatientFilterPanelComponent {
  @Output() filterChange = new EventEmitter<{ genders: Gender[]; statuses: PatientStatus[] }>();
  @Output() resetFilter = new EventEmitter<void>();

  selectedGenders = new Set<Gender>();
  selectedStatuses = new Set<PatientStatus>();

  genderOptions = GENDER_OPTIONS;
  statusOptions = STATUS_OPTIONS;

  toggleGender(gender: Gender): void {
    if (this.selectedGenders.has(gender)) {
      this.selectedGenders.delete(gender);
    } else {
      this.selectedGenders.add(gender);
    }
  }

  toggleStatus(status: PatientStatus): void {
    if (this.selectedStatuses.has(status)) {
      this.selectedStatuses.delete(status);
    } else {
      this.selectedStatuses.add(status);
    }
  }

  apply(): void {
    this.filterChange.emit({
      genders: Array.from(this.selectedGenders),
      statuses: Array.from(this.selectedStatuses)
    });
  }

  reset(): void {
    this.selectedGenders.clear();
    this.selectedStatuses.clear();
    this.resetFilter.emit();
  }
}
