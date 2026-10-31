export type Tone = 'success' | 'warning' | 'danger' | 'neutral' | 'info';

export interface StatusStyle {
  label: string;
  tone: Tone;
}

// Một nơi duy nhất quy định chữ và màu của mọi trạng thái; giá trị khớp enum của backend (docs/02-co-so-du-lieu.md)
export const STATUS_STYLES = {
  appointment: {
    HELD: { label: 'Đang giữ chỗ', tone: 'warning' },
    CONFIRMED: { label: 'Đã xác nhận', tone: 'success' },
    COMPLETED: { label: 'Đã khám', tone: 'neutral' },
    NO_SHOW: { label: 'Không đến', tone: 'neutral' },
    EXPIRED: { label: 'Hết hạn giữ chỗ', tone: 'neutral' },
    CANCELLED: { label: 'Đã hủy', tone: 'danger' }
  },
  patient: {
    ACTIVE: { label: 'Đã xác minh', tone: 'success' },
    TEMPORARY: { label: 'Hồ sơ tạm', tone: 'warning' },
    MERGED: { label: 'Đã gộp', tone: 'neutral' }
  },
  patientOutcome: {
    CREATED_ACTIVE: { label: 'Đã tạo hồ sơ mới', tone: 'success' },
    CREATED_TEMPORARY: { label: 'Đã tạo hồ sơ tạm', tone: 'warning' },
    REUSED_EXISTING: { label: 'Đã liên kết hồ sơ có sẵn', tone: 'info' }
  },
  duplicate: {
    PENDING: { label: 'Chờ duyệt', tone: 'warning' },
    MERGED: { label: 'Đã gộp', tone: 'success' },
    NOT_DUPLICATE: { label: 'Không trùng', tone: 'neutral' }
  },
  reminder: {
    SENT: { label: 'Đã gửi', tone: 'success' },
    FAILED: { label: 'Lỗi', tone: 'danger' },
    SKIPPED: { label: 'Bỏ qua', tone: 'neutral' }
  },
  account: {
    true: { label: 'Đang hoạt động', tone: 'success' },
    false: { label: 'Đã khóa', tone: 'neutral' }
  },
  role: {
    PATIENT: { label: 'Bệnh nhân', tone: 'info' },
    RECEPTIONIST: { label: 'Lễ tân', tone: 'info' },
    DOCTOR: { label: 'Bác sĩ', tone: 'info' },
    ADMIN: { label: 'Quản trị viên', tone: 'info' }
  }
} satisfies Record<string, Record<string, StatusStyle>>;

export type StatusKind = keyof typeof STATUS_STYLES;

export function statusStyle(kind: StatusKind, value: string | boolean | null | undefined): StatusStyle {
  const styles: Record<string, StatusStyle> = STATUS_STYLES[kind];
  return styles[String(value)] ?? { label: String(value ?? '—'), tone: 'neutral' };
}
