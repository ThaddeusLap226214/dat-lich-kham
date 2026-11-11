import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReminderService } from '../services/reminder.service';
import { MessageTemplate, ReminderType } from '../models/reminder.model';
import { IdentityService } from '../../../core/identity.service';

@Component({
  selector: 'app-message-templates-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './message-templates-page.component.html',
  styleUrls: ['./message-templates-page.component.css']
})
export class MessageTemplatesPageComponent implements OnInit {
  private readonly reminderService = inject(ReminderService);
  private readonly identityService = inject(IdentityService);

  readonly currentIdentity = this.identityService.currentIdentity;

  templates: MessageTemplate[] = [];
  loading = false;
  saving = false;
  successMessage = '';
  errorMessage = '';

  editingTemplate: MessageTemplate | null = null;
  editSubject = '';
  editBody = '';

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reminderService.getTemplates().subscribe({
      next: (data) => {
        this.templates = data;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Không thể tải danh sách mẫu thư';
        this.loading = false;
      }
    });
  }

  openEdit(template: MessageTemplate): void {
    this.editingTemplate = template;
    this.editSubject = template.subject;
    this.editBody = template.body;
    this.successMessage = '';
    this.errorMessage = '';
  }

  cancelEdit(): void {
    this.editingTemplate = null;
  }

  saveEdit(): void {
    if (!this.editingTemplate) return;
    if (!this.editSubject.trim() || !this.editBody.trim()) {
      this.errorMessage = 'Tiêu đề và nội dung không được để trống';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.reminderService.updateTemplate(this.editingTemplate.type, this.editSubject.trim(), this.editBody.trim()).subscribe({
      next: (updated) => {
        this.successMessage = `Đã cập nhật mẫu thư "${this.getTypeLabel(this.editingTemplate!.type)}" thành công!`;
        this.saving = false;
        this.editingTemplate = null;
        this.loadTemplates();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Có lỗi xảy ra khi lưu mẫu thư';
        this.saving = false;
      }
    });
  }

  getTypeLabel(type: ReminderType): string {
    switch (type) {
      case 'BEFORE_APPOINTMENT':
        return 'Nhắc lịch khám trước 24h';
      case 'BEFORE_APPOINTMENT_2H':
        return 'Nhắc lịch khám trước 2h';
      case 'CONFIRMATION':
        return 'Xác nhận đặt lịch hẹn';
      case 'CANCELLATION':
        return 'Thông báo hủy lịch hẹn';
      default:
        return type;
    }
  }

  getTypeIcon(type: ReminderType): string {
    switch (type) {
      case 'BEFORE_APPOINTMENT':
      case 'BEFORE_APPOINTMENT_2H':
        return 'notifications_active';
      case 'CONFIRMATION':
        return 'check_circle';
      case 'CANCELLATION':
        return 'cancel';
      default:
        return 'mail';
    }
  }

  insertPlaceholder(placeholder: string): void {
    this.editBody += placeholder;
  }
}
