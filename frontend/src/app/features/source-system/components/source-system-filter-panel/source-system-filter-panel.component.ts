import { Component, EventEmitter, Output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { STATUS_OPTIONS } from '../../models/source-system.constants';

export interface SourceSystemFilterState {
  facilityIds: number[];
  isActive: boolean | null;
}

@Component({
  selector: 'app-source-system-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './source-system-filter-panel.component.html',
  styleUrls: ['./source-system-filter-panel.component.css']
})
export class SourceSystemFilterPanelComponent {
  @Output() filterChange = new EventEmitter<SourceSystemFilterState>();
  @Output() resetFilter = new EventEmitter<void>();

  selectedFacilityIds = new Set<number>();
  selectedStatus: boolean | null = null;

  statusOptions = STATUS_OPTIONS;

  toggleStatus(status: boolean): void {
    if (this.selectedStatus === status) {
      this.selectedStatus = null;
    } else {
      this.selectedStatus = status;
    }
  }

  apply(): void {
    this.filterChange.emit({
      facilityIds: Array.from(this.selectedFacilityIds),
      isActive: this.selectedStatus
    });
  }

  reset(): void {
    this.selectedFacilityIds.clear();
    this.selectedStatus = null;
    this.resetFilter.emit();
  }
}
