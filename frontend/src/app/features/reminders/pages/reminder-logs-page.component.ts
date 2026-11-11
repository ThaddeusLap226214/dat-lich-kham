import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReminderService } from '../services/reminder.service';
import { ReminderLog, ReminderStatus, ReminderType, RunResult } from '../models/reminder.model';
import { IdentityService } from '../../../core/identity.service';

@Component({
  selector: 'app-reminder-logs-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reminder-logs-page.component.html',
  styleUrls: ['./reminder-logs-page.component.css']
})
export class ReminderLogsPageComponent implements OnInit {
  private readonly reminderService = inject(ReminderService);
  private readonly identityService = inject(IdentityService);

  readonly currentIdentity = this.identityService.currentIdentity;

  logs: ReminderLog[] = [];
  loading = false;
  running = false;
  lastRunResult: RunResult | null = null;
  errorMessage = '';

  selectedStatus: ReminderStatus | '' = '';
  selectedType: ReminderType | '' = '';

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reminderService.getLogs(this.selectedStatus, this.selectedType).subscribe({
      next: (data) => {
        this.logs = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Không thể tải nhật ký thông báo';
        this.loading = false;
      }
    });
  }

  onFilterChange(): void {
    this.loadLogs();
  }

  runNow(): void {
    if (this.running) return;
    this.running = true;
    this.lastRunResult = null;
    this.errorMessage = '';

    this.reminderService.runNow().subscribe({
      next: (result) => {
        this.lastRunResult = result;
        this.running = false;
        this.loadLogs();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Có lỗi xảy ra khi yêu cầu quét ngay';
        this.running = false;
      }
    });
  }

  openMailpit(): void {
    window.open('http://localhost:8025', '_blank');
  }

  get totalCount(): number {
    return this.logs.length;
  }

  get sentCount(): number {
    return this.logs.filter(l => l.status === 'SENT').length;
  }

  get skippedCount(): number {
    return this.logs.filter(l => l.status === 'SKIPPED').length;
  }

  get failedCount(): number {
    return this.logs.filter(l => l.status === 'FAILED').length;
  }

  isAdmin(): boolean {
    return this.currentIdentity().role === 'ADMIN';
  }

  getTypeLabel(type: ReminderType): string {
    switch (type) {
      case 'BEFORE_APPOINTMENT':
        return 'Nhắc lịch (24h)';
      case 'BEFORE_APPOINTMENT_2H':
        return 'Nhắc lịch (2h)';
      case 'CONFIRMATION':
        return 'Xác nhận lịch';
      case 'CANCELLATION':
        return 'Báo hủy lịch';
      default:
        return type;
    }
  }

  getTypeBadgeClass(type: ReminderType): string {
    switch (type) {
      case 'BEFORE_APPOINTMENT':
      case 'BEFORE_APPOINTMENT_2H':
        return 'badge-info';
      case 'CONFIRMATION':
        return 'badge-success';
      case 'CANCELLATION':
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  getStatusBadgeClass(status: ReminderStatus): string {
    switch (status) {
      case 'SENT':
        return 'badge-success';
      case 'SKIPPED':
        return 'badge-warning';
      case 'FAILED':
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }
}
