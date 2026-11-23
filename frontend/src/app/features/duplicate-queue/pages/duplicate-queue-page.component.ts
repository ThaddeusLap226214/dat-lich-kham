import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DuplicateQueueService } from '../services/duplicate-queue.service';
import {
  CandidateStatus,
  DuplicateCandidate,
  DuplicateReason,
  DUPLICATE_REASON_LABELS
} from '../models/duplicate-candidate.model';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { PageMeta } from '../../../shared/models/pagination.model';

@Component({
  selector: 'app-duplicate-queue-page',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, PaginationComponent],
  templateUrl: './duplicate-queue-page.component.html',
  styleUrls: ['./duplicate-queue-page.component.css']
})
export class DuplicateQueuePageComponent implements OnInit {
  private readonly duplicateQueueService = inject(DuplicateQueueService);
  private readonly toastService = inject(ToastService);

  candidates = signal<DuplicateCandidate[]>([]);
  isLoading = signal(true);
  pendingCount = signal(0);
  selectedStatus = signal<CandidateStatus>(CandidateStatus.PENDING);

  pageMeta = signal<PageMeta>({
    page: 1,
    pageSize: 10,
    total: 0,
    pages: 0
  });

  // Modals state
  selectedCandidate: DuplicateCandidate | null = null;
  isMergeModalOpen = false;
  isRejectModalOpen = false;
  isUnmergeModalOpen = false;

  keepPatientId: number = 0;
  reviewNote: string = '';
  isSubmitting = false;

  readonly CandidateStatus = CandidateStatus;
  readonly REASON_LABELS = DUPLICATE_REASON_LABELS;

  ngOnInit(): void {
    this.loadPendingCount();
    this.loadCandidates();
  }

  loadPendingCount(): void {
    this.duplicateQueueService.count(CandidateStatus.PENDING).subscribe({
      next: (res) => this.pendingCount.set(res.count),
      error: () => {}
    });
  }

  loadCandidates(): void {
    this.isLoading.set(true);
    this.duplicateQueueService.list({
      status: this.selectedStatus(),
      page: this.pageMeta().page - 1,
      size: this.pageMeta().pageSize
    }).subscribe({
      next: (res) => {
        this.candidates.set(res.content || []);
        this.pageMeta.set({
          page: res.page + 1,
          pageSize: res.size,
          total: res.totalElements,
          pages: res.totalPages
        });
        this.isLoading.set(false);
      },
      error: (err: unknown) => {
        console.error('Failed to load duplicate queue', err);
        this.toastService.error('Lỗi', 'Không thể tải danh sách hàng chờ duyệt trùng');
        this.isLoading.set(false);
      }
    });
  }

  changeStatus(status: CandidateStatus): void {
    this.selectedStatus.set(status);
    this.pageMeta.update((m) => ({ ...m, page: 1 }));
    this.loadCandidates();
  }

  onPageChange(page: number): void {
    this.pageMeta.update((m) => ({ ...m, page }));
    this.loadCandidates();
  }

  openMergeModal(candidate: DuplicateCandidate): void {
    this.selectedCandidate = candidate;
    // Mặc định chọn giữ hồ sơ có sẵn (candidatePatient) nếu nó ACTIVE
    this.keepPatientId = candidate.candidatePatient.status === 'ACTIVE'
      ? candidate.candidatePatient.id
      : candidate.patient.id;
    this.reviewNote = '';
    this.isMergeModalOpen = true;
  }

  openRejectModal(candidate: DuplicateCandidate): void {
    this.selectedCandidate = candidate;
    this.reviewNote = '';
    this.isRejectModalOpen = true;
  }

  openUnmergeModal(candidate: DuplicateCandidate): void {
    this.selectedCandidate = candidate;
    this.reviewNote = '';
    this.isUnmergeModalOpen = true;
  }

  closeModals(): void {
    this.isMergeModalOpen = false;
    this.isRejectModalOpen = false;
    this.isUnmergeModalOpen = false;
    this.selectedCandidate = null;
    this.isSubmitting = false;
  }

  confirmMerge(): void {
    if (!this.selectedCandidate) return;
    this.isSubmitting = true;

    this.duplicateQueueService.merge(this.selectedCandidate.id, {
      keepPatientId: this.keepPatientId,
      note: this.reviewNote
    }).subscribe({
      next: () => {
        this.toastService.success('Thành công', 'Đã gộp hồ sơ thành công và phát sự kiện đồng bộ lịch');
        this.closeModals();
        this.loadPendingCount();
        this.loadCandidates();
      },
      error: (err: any) => {
        console.error('Merge failed', err);
        this.toastService.error('Lỗi', err?.error?.message || 'Gộp hồ sơ thất bại');
        this.isSubmitting = false;
      }
    });
  }

  confirmReject(): void {
    if (!this.selectedCandidate) return;
    this.isSubmitting = true;

    this.duplicateQueueService.reject(this.selectedCandidate.id, {
      note: this.reviewNote
    }).subscribe({
      next: () => {
        this.toastService.success('Thành công', 'Đã xác nhận không trùng thành công');
        this.closeModals();
        this.loadPendingCount();
        this.loadCandidates();
      },
      error: (err: any) => {
        console.error('Reject failed', err);
        this.toastService.error('Lỗi', err?.error?.message || 'Xác nhận không trùng thất bại');
        this.isSubmitting = false;
      }
    });
  }

  confirmUnmerge(): void {
    if (!this.selectedCandidate) return;
    this.isSubmitting = true;

    const mergedPatientId = this.selectedCandidate.patient.status === 'MERGED'
      ? this.selectedCandidate.patient.id
      : this.selectedCandidate.candidatePatient.id;

    this.duplicateQueueService.unmerge(mergedPatientId, {
      note: this.reviewNote
    }).subscribe({
      next: () => {
        this.toastService.success('Thành công', 'Đã tách hồ sơ thành công và hoàn tác trạng thái');
        this.closeModals();
        this.loadPendingCount();
        this.loadCandidates();
      },
      error: (err: any) => {
        console.error('Unmerge failed', err);
        this.toastService.error('Lỗi', err?.error?.message || 'Tách hồ sơ thất bại');
        this.isSubmitting = false;
      }
    });
  }
}
