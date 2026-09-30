import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { switchMap, filter } from 'rxjs/operators';

import { ImportService } from '../services/import.service';
import { SourceSystemService } from '../../source-system/services/source-system.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import {
  FileValidationResponse,
  ImportJob,
  ImportJobStatus,
  PreviewRow,
  StartImportRequest
} from '../models/import.model';
import { SourceSystem } from '../../source-system/models/source-system.model';
import { PageResponse } from '../../../shared/models';

interface ActiveJob {
  job: ImportJob;
  progressPercentage: number;
  isProcessing: boolean;
}

@Component({
  selector: 'app-patient-import-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './patient-import-page.component.html',
  styleUrls: ['./patient-import-page.component.css']
})
export class PatientImportPageComponent implements OnInit {
  private readonly importService = inject(ImportService);
  private readonly sourceSystemService = inject(SourceSystemService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  // Configuration
  selectedSourceSystemId: number | null = null;
  sourceSystems: SourceSystem[] = [];
  skipDuplicates = true;
  duplicateThreshold = 98;

  // File Upload State
  isDragging = false;
  selectedFile: File | null = null;
  isUploading = false;
  validationResponse: FileValidationResponse | null = null;

  // Preview Data
  previewData: PreviewRow[] = [];
  previewTotalRows = 0;
  isLoadingPreview = false;

  // Active Jobs
  activeJobs: ActiveJob[] = [];
  isLoadingJobs = false;

  ngOnInit(): void {
    this.loadSourceSystems();
    this.loadActiveJobs();
    this.startJobPolling();
  }

  private loadSourceSystems(): void {
    this.sourceSystemService.search({ page: 0, size: 100, isActive: true })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response.statusCode === 200 && response.data) {
            this.sourceSystems = response.data.result;
            if (this.sourceSystems.length > 0 && !this.selectedSourceSystemId) {
              this.selectedSourceSystemId = this.sourceSystems[0].id;
            }
          }
        },
        error: (error) => {
          this.toastService.error('Lỗi', 'Không thể tải danh sách hệ thống nguồn');
          console.error('Error loading source systems:', error);
        }
      });
  }

  private loadActiveJobs(): void {
    this.isLoadingJobs = true;
    this.importService.searchImportJobs({ page: 0, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response.statusCode === 200 && response.data) {
            this.activeJobs = response.data.result
              .slice(0, 5)
              .map((job: ImportJob) => this.mapToActiveJob(job));
          }
          this.isLoadingJobs = false;
        },
        error: (error) => {
          console.error('Error loading active jobs:', error);
          this.isLoadingJobs = false;
        }
      });
  }

  private startJobPolling(): void {
    interval(5000)
      .pipe(
        filter(() => this.activeJobs.some(j => j.isProcessing)),
        switchMap(() => this.importService.searchImportJobs({ page: 0, size: 10 })),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (response) => {
          if (response.statusCode === 200 && response.data) {
            this.activeJobs = response.data.result
              .slice(0, 5)
              .map((job: ImportJob) => this.mapToActiveJob(job));
          }
        }
      });
  }

  private mapToActiveJob(job: ImportJob): ActiveJob {
    const progressPercentage = job.totalRows > 0
      ? Math.round((job.processedRows / job.totalRows) * 100)
      : 0;
    const isProcessing = [ImportJobStatus.PROCESSING, ImportJobStatus.VALIDATING].includes(job.status);

    return { job, progressPercentage, isProcessing };
  }

  // Drag and Drop Handlers
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFileSelection(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelection(input.files[0]);
    }
  }

  private handleFileSelection(file: File): void {
    // Validate file type
    const allowedTypes = [
      'text/csv',
      'application/vnd.ms-excel',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ];

    if (!allowedTypes.includes(file.type) && !file.name.match(/\.(csv|xlsx|xls)$/i)) {
      this.toastService.error('Lỗi', 'Chỉ hỗ trợ file CSV hoặc Excel (.xlsx, .xls)');
      return;
    }

    // Validate file size (500MB)
    const maxSize = 500 * 1024 * 1024;
    if (file.size > maxSize) {
      this.toastService.error('Lỗi', 'Kích thước file vượt quá 500MB');
      return;
    }

    if (!this.selectedSourceSystemId) {
      this.toastService.error('Lỗi', 'Vui lòng chọn hệ thống nguồn trước');
      return;
    }

    this.selectedFile = file;
    this.uploadFile();
  }

  private uploadFile(): void {
    if (!this.selectedFile || !this.selectedSourceSystemId) {
      return;
    }

    this.isUploading = true;
    this.validationResponse = null;
    this.previewData = [];

    this.importService.uploadFile(this.selectedFile, this.selectedSourceSystemId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response.statusCode === 200 && response.data) {
            this.validationResponse = response.data;
            this.toastService.success('Thành công', 'File đã được tải lên và xác thực');
            
            // Load preview data if validation passed
            if (response.data.isValid) {
              // Note: Backend needs to provide jobId in FileValidationResponse
              // For now, we'll skip preview loading
              // this.loadPreviewData(response.data.jobId);
            }
          }
          this.isUploading = false;
        },
        error: (error) => {
          this.toastService.error('Lỗi', error?.error?.message || 'Không thể tải file lên');
          this.isUploading = false;
        }
      });
  }

  startImport(): void {
    if (!this.validationResponse || !this.selectedSourceSystemId) {
      this.toastService.error('Lỗi', 'Vui lòng tải file lên trước');
      return;
    }

    const request: StartImportRequest = {
      sourceSystemId: this.selectedSourceSystemId,
      fileToken: this.validationResponse.fileToken,
      columnMappings: this.getDefaultColumnMappings(),
      skipDuplicates: this.skipDuplicates,
      duplicateThreshold: this.duplicateThreshold
    };

    this.importService.startImport(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response.statusCode === 200) {
            this.toastService.success('Thành công', 'Quá trình nhập dữ liệu đã bắt đầu');
            this.validationResponse = null;
            this.selectedFile = null;
            this.previewData = [];
            this.loadActiveJobs();
          }
        },
        error: (error) => {
          this.toastService.error('Lỗi', error?.error?.message || 'Không thể bắt đầu import');
        }
      });
  }

  private getDefaultColumnMappings(): Record<string, string> {
    // Auto-map columns based on detected column names
    const mappings: Record<string, string> = {};
    
    if (this.validationResponse) {
      this.validationResponse.detectedColumns.forEach(col => {
        const lowerCol = col.toLowerCase();
        if (lowerCol.includes('name') || lowerCol.includes('tên')) {
          mappings[col] = 'fullName';
        } else if (lowerCol.includes('dob') || lowerCol.includes('birth') || lowerCol.includes('sinh')) {
          mappings[col] = 'dateOfBirth';
        } else if (lowerCol.includes('gender') || lowerCol.includes('sex') || lowerCol.includes('giới')) {
          mappings[col] = 'gender';
        } else if (lowerCol.includes('phone') || lowerCol.includes('điện')) {
          mappings[col] = 'phoneNumber';
        } else if (lowerCol.includes('national') || lowerCol.includes('cccd') || lowerCol.includes('cmnd')) {
          mappings[col] = 'nationalId';
        } else if (lowerCol.includes('insurance') || lowerCol.includes('bhyt')) {
          mappings[col] = 'healthInsuranceNo';
        } else if (lowerCol.includes('address') || lowerCol.includes('địa')) {
          mappings[col] = 'address';
        } else if (lowerCol.includes('code') || lowerCol.includes('mrn') || lowerCol.includes('mã')) {
          mappings[col] = 'localPatientCode';
        }
      });
    }

    return mappings;
  }

  getStatusBadgeClass(status: ImportJobStatus): string {
    switch (status) {
      case ImportJobStatus.COMPLETED:
        return 'bg-green-100 text-green-700 border-green-200';
      case ImportJobStatus.PROCESSING:
      case ImportJobStatus.VALIDATING:
        return 'bg-blue-100 text-blue-700 border-blue-200';
      case ImportJobStatus.FAILED:
        return 'bg-red-100 text-red-700 border-red-200';
      case ImportJobStatus.CANCELLED:
        return 'bg-gray-100 text-gray-700 border-gray-200';
      default:
        return 'bg-yellow-100 text-yellow-700 border-yellow-200';
    }
  }

  getMatchStatusDisplay(row: PreviewRow): { text: string; class: string; dotClass: string } {
    if (!row.matchStatus) {
      return { text: 'Đang xử lý', class: 'text-gray-600', dotClass: 'bg-gray-400' };
    }

    switch (row.matchStatus) {
      case 'HIGH_MATCH':
        return { 
          text: `Khớp cao ${row.estimatedMatchScore || 95}%`, 
          class: 'text-green-700', 
          dotClass: 'bg-green-500' 
        };
      case 'MEDIUM_MATCH':
        return { 
          text: `Cần xem xét ${row.estimatedMatchScore || 70}%`, 
          class: 'text-yellow-700', 
          dotClass: 'bg-yellow-500' 
        };
      case 'LOW_MATCH':
        return { 
          text: `Khớp thấp ${row.estimatedMatchScore || 40}%`, 
          class: 'text-orange-700', 
          dotClass: 'bg-orange-500' 
        };
      case 'NEW_RECORD':
        return { text: 'Hồ sơ mới', class: 'text-blue-700', dotClass: 'bg-blue-500' };
      case 'ERROR':
        return { text: 'Lỗi - Thiếu dữ liệu', class: 'text-red-700', dotClass: 'bg-red-500' };
      default:
        return { text: 'Không xác định', class: 'text-gray-600', dotClass: 'bg-gray-400' };
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  formatDateTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)} giờ trước`;
    return date.toLocaleDateString('vi-VN');
  }

  removeFile(): void {
    this.selectedFile = null;
    this.validationResponse = null;
    this.previewData = [];
  }
}
