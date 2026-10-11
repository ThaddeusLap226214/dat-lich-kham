import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ToastService } from '../components/toast/toast.service';

// Dạng lỗi chung của mọi service (docs/06 mục A4)
export interface ApiError {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
}

const FALLBACK_MESSAGES: Record<number, string> = {
  0: 'Không kết nối được tới máy chủ. Kiểm tra hệ thống đã bật chưa (docker compose ps).',
  400: 'Dữ liệu gửi lên chưa hợp lệ.',
  401: 'Phiên đăng nhập đã hết, vui lòng đăng nhập lại.',
  403: 'Bạn không có quyền thực hiện thao tác này.',
  404: 'Không tìm thấy dữ liệu.',
  409: 'Thao tác bị từ chối do dữ liệu đã thay đổi.',
  501: 'Chức năng này chưa làm.',
  503: 'Dịch vụ phía sau tạm thời không phản hồi, vui lòng thử lại sau.'
};

@Injectable({ providedIn: 'root' })
export class ApiErrorService {
  private readonly toast = inject(ToastService);

  parse(err: unknown): ApiError {
    if (err instanceof HttpErrorResponse) {
      const body = err.error as Partial<ApiError> | null;
      return {
        status: err.status,
        error: body?.error ?? (err.status === 0 ? 'NETWORK_ERROR' : 'UNKNOWN'),
        message: body?.message || FALLBACK_MESSAGES[err.status] || 'Đã có lỗi xảy ra, vui lòng thử lại.',
        path: body?.path,
        timestamp: body?.timestamp
      };
    }
    return { status: -1, error: 'UNKNOWN', message: 'Đã có lỗi xảy ra, vui lòng thử lại.' };
  }

  // Trang tự xử lý các mã lỗi nghiệp vụ mình biết (ví dụ SLOT_ALREADY_HELD), phần còn lại gọi show()
  is(err: unknown, code: string): boolean {
    return this.parse(err).error === code;
  }

  show(err: unknown): ApiError {
    const e = this.parse(err);
    if (e.status === 501) {
      this.toast.info('Chức năng chưa làm', e.message);
    } else if (e.status === 409 || e.status === 400) {
      this.toast.warning('Không thực hiện được', e.message);
    } else {
      this.toast.error(e.status > 0 ? `Lỗi ${e.status}` : 'Mất kết nối', e.message);
    }
    return e;
  }
}
