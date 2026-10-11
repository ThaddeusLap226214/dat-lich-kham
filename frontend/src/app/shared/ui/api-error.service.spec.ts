import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ToastService } from '../components/toast/toast.service';
import { ApiErrorService } from './api-error.service';
import { statusStyle } from './status';

describe('ApiErrorService', () => {
  let service: ApiErrorService;
  let toast: ToastService;

  beforeEach(() => {
    service = TestBed.inject(ApiErrorService);
    toast = TestBed.inject(ToastService);
  });

  it('501 hiện toast thông tin "Chức năng chưa làm" kèm câu của backend', () => {
    service.show(new HttpErrorResponse({ status: 501, error: { status: 501, error: 'NOT_IMPLEMENTED', message: 'Chưa làm: P5-1' } }));
    const t = toast.toasts()[0];
    expect(t.type).toBe('info');
    expect(t.title).toBe('Chức năng chưa làm');
    expect(t.message).toBe('Chưa làm: P5-1');
  });

  it('409 giữ mã lỗi để trang tự xử lý và hiện câu tiếng Việt của backend', () => {
    const err = new HttpErrorResponse({ status: 409, error: { status: 409, error: 'SLOT_ALREADY_HELD', message: 'Khung giờ này vừa có người giữ' } });
    expect(service.is(err, 'SLOT_ALREADY_HELD')).toBeTrue();
    service.show(err);
    expect(toast.toasts()[0].type).toBe('warning');
    expect(toast.toasts()[0].message).toBe('Khung giờ này vừa có người giữ');
  });

  it('mất kết nối (status 0, không có thân lỗi) dùng câu dự phòng', () => {
    const e = service.show(new HttpErrorResponse({ status: 0, error: null }));
    expect(e.error).toBe('NETWORK_ERROR');
    expect(toast.toasts()[0].title).toBe('Mất kết nối');
  });

  it('nhãn trạng thái: giá trị lạ không làm vỡ giao diện', () => {
    expect(statusStyle('appointment', 'CANCELLED')).toEqual({ label: 'Đã hủy', tone: 'danger' });
    expect(statusStyle('account', false).label).toBe('Đã khóa');
    expect(statusStyle('appointment', 'LA')).toEqual({ label: 'LA', tone: 'neutral' });
  });
});
