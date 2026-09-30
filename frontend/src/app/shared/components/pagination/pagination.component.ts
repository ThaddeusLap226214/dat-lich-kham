import { Component, EventEmitter, Input, Output, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageMeta } from '../../../shared/models';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css']
})
export class PaginationComponent {
  @Input() pageMeta: PageMeta | null = null;
  @Output() pageChange = new EventEmitter<number>();

  get currentPage(): number {
    return this.pageMeta?.page ?? 1;
  }

  get totalPages(): number {
    return this.pageMeta?.pages ?? 1;
  }

  get isFirstPage(): boolean {
    return this.currentPage <= 1;
  }

  get isLastPage(): boolean {
    return this.currentPage >= this.totalPages;
  }

  get displayRangeStart(): number {
    if (!this.pageMeta) return 0;
    return (this.pageMeta.page - 1) * this.pageMeta.pageSize + 1;
  }

  get displayRangeEnd(): number {
    if (!this.pageMeta) return 0;
    return Math.min(this.pageMeta.page * this.pageMeta.pageSize, this.pageMeta.total);
  }

  visiblePages = computed(() => {
    const total = this.totalPages;
    const current = this.currentPage;
    const pages: number[] = [];

    if (total <= 7) {
      for (let i = 1; i <= total; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);

      if (current > 3) {
        pages.push(-1);
      }

      const start = Math.max(2, current - 1);
      const end = Math.min(total - 1, current + 1);

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (current < total - 2) {
        pages.push(-1);
      }

      pages.push(total);
    }

    return pages;
  });

  onPageChange(page: number): void {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }
}