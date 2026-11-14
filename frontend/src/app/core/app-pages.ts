import { Role } from './identity.service';

export type Part = 'P1' | 'P2' | 'P3' | 'P4' | 'P5' | 'P6';

export const PARTS: Record<Part, string> = {
  P1: 'Patient Service (MPI)',
  P2: 'Scheduling Service',
  P3: 'Provider Service',
  P4: 'API Gateway',
  P5: 'Reminder Service',
  P6: 'Auth Service'
};

export interface ApiCall {
  method: 'GET' | 'POST';
  // {today} = ngày hôm nay, {doctorId} = mã bác sĩ của vai trò đang chọn (mặc định 1)
  path: string;
  body?: unknown;
}

export interface AppPage {
  path: string;
  title: string;
  icon: string;
  roles: Role[];
  part: Part;
  summary: string;
  tasks: string[];
  api: ApiCall;
  // Trang đã có giao diện riêng (không dùng trang "chưa làm" chung)
  implemented?: boolean;
}

const ALL: Role[] = ['GUEST', 'PATIENT', 'RECEPTIONIST', 'DOCTOR', 'ADMIN'];

// Danh sách trang của hệ thống, mỗi trang thuộc một phần việc. Sidebar và router cùng đọc danh sách này.
export const APP_PAGES: AppPage[] = [
  {
    path: 'register', title: 'Đăng ký', icon: 'person_add', roles: ['GUEST'], part: 'P6',
    summary: 'Tạo tài khoản bệnh nhân mới; Auth Service cấp mã tài khoản rồi tạo người dùng trong Keycloak.',
    tasks: [], api: { method: 'POST', path: '/auth/register' }, implemented: true
  },
  {
    path: 'doctors', title: 'Chuyên khoa & bác sĩ', icon: 'stethoscope', roles: ALL, part: 'P3',
    summary: 'Ai cũng xem được danh sách chuyên khoa và bác sĩ, không cần đăng nhập.',
    tasks: ['Danh sách chuyên khoa (GET /api/v1/specialties)', 'Lọc bác sĩ theo chuyên khoa (GET /api/v1/doctors?specialtyId=)', 'Xem lịch làm việc trong tuần của một bác sĩ'],
    api: { method: 'GET', path: '/doctors' },
    implemented: true
  },
  {
    path: 'booking', title: 'Đặt lịch khám', icon: 'event_available', roles: ['PATIENT', 'RECEPTIONIST'], part: 'P2',
    summary: 'Chọn chuyên khoa → bác sĩ → ngày → slot trống → hồ sơ bệnh nhân → giữ chỗ → xác nhận.',
    tasks: [
      'Hiện slot trong ngày (GET /api/v1/slots), slot đã có người giữ thì khóa lại',
      'Giữ chỗ (POST /api/v1/appointments/hold), đếm ngược thời gian giữ chỗ',
      'Nhận 409 SLOT_ALREADY_HELD → báo "vừa có người giữ" và tải lại slot',
      'Xác nhận (POST /api/v1/appointments/{id}/confirm)'
    ],
    api: { method: 'GET', path: '/slots?doctorId=1&date={today}' },
    implemented: true
  },
  {
    path: 'my-appointments', title: 'Lịch khám của tôi', icon: 'calendar_month', roles: ['PATIENT'], part: 'P3',
    summary: 'Danh sách lịch hẹn kèm tên bác sĩ, phòng, bệnh nhân. Trang thuộc P3, API ghép thuộc P4 (Gateway ghép từ 3 service).',
    tasks: ['Gateway ghép Scheduling + Provider + Patient (GET /api/v1/composite/my-appointments)', 'Nút hủy lịch (POST /api/v1/appointments/{id}/cancel)'],
    api: { method: 'GET', path: '/composite/my-appointments' },
    implemented: true
  },
  {
    path: 'my-patients', title: 'Hồ sơ bệnh nhân của tôi', icon: 'family_restroom', roles: ['PATIENT'], part: 'P1',
    summary: 'Một tài khoản quản lý nhiều hồ sơ (bản thân, con, người nhà).',
    tasks: ['Danh sách hồ sơ (GET /api/v1/patients/mine)', 'Thêm hồ sơ (POST /api/v1/patients) — hiện kết quả REUSED / CREATED_ACTIVE / CREATED_TEMPORARY', 'Sửa hồ sơ (PUT /api/v1/patients/{id})'],
    api: { method: 'GET', path: '/patients/mine' }
  },
  {
    path: 'patients', title: 'Hồ sơ bệnh nhân', icon: 'person_search', roles: ['RECEPTIONIST', 'DOCTOR'], part: 'P1',
    summary: 'Trang tìm và xem hồ sơ bệnh nhân, chuyển từ giao diện MPI cũ.',
    tasks: [], api: { method: 'GET', path: '/patients' }, implemented: true
  },
  {
    path: 'duplicate-queue', title: 'Hàng chờ duyệt hồ sơ trùng', icon: 'rule', roles: ['RECEPTIONIST'], part: 'P1',
    summary: 'Các cặp hồ sơ nghi là cùng một người; lễ tân so sánh rồi gộp hoặc xác nhận không trùng.',
    tasks: ['Danh sách + số lượng chờ (GET /api/v1/duplicate-candidates, /count)', 'So sánh hai hồ sơ cạnh nhau', 'Gộp (POST …/{id}/merge) hoặc không trùng (POST …/{id}/reject)', 'Tách hồ sơ đã gộp nhầm (POST /api/v1/patients/{id}/unmerge)'],
    api: { method: 'GET', path: '/duplicate-candidates' }
  },
  {
    path: 'appointments', title: 'Lịch hẹn theo bác sĩ', icon: 'event_note', roles: ['RECEPTIONIST'], part: 'P2',
    summary: 'Lễ tân xem lịch hẹn của từng bác sĩ trong ngày, hủy giúp bệnh nhân.',
    tasks: ['Chọn bác sĩ + ngày (GET /api/v1/appointments?doctorId=&date=)', 'Hủy lịch thay bệnh nhân'],
    api: { method: 'GET', path: '/appointments?doctorId=1&date={today}' },
    implemented: true
  },
  {
    path: 'doctor-agenda', title: 'Lịch khám trong ngày', icon: 'today', roles: ['DOCTOR'], part: 'P3',
    summary: 'Bác sĩ xem bệnh nhân của mình trong ngày và đánh dấu đã khám. Trang thuộc P3, API ghép thuộc P4.',
    tasks: ['Danh sách từ GET /api/v1/composite/doctor-agenda (P4 làm API)', 'Nút "Đã khám" (POST /api/v1/appointments/{id}/complete)'],
    api: { method: 'GET', path: '/composite/doctor-agenda?doctorId={doctorId}&date={today}' },
    implemented: true
  },
  {
    path: 'admin/catalog', title: 'Chuyên khoa, bác sĩ, phòng', icon: 'medical_services', roles: ['ADMIN'], part: 'P3',
    summary: 'Quản trị viên thêm/sửa chuyên khoa, bác sĩ, phòng khám.',
    tasks: ['Bảng + form cho chuyên khoa, bác sĩ, phòng (POST/PUT /api/v1/specialties, /doctors, /rooms)'],
    api: { method: 'GET', path: '/rooms' },
    implemented: true
  },
  {
    path: 'admin/work-schedules', title: 'Lịch làm việc bác sĩ', icon: 'schedule', roles: ['ADMIN'], part: 'P3',
    summary: 'Cấu hình ca làm việc hằng tuần: thứ, giờ bắt đầu – kết thúc, phòng, thời lượng một lượt khám.',
    tasks: ['Xem ca theo bác sĩ (GET /api/v1/doctors/{id}/work-schedules)', 'Thêm/sửa/ngừng ca, báo lỗi SCHEDULE_OVERLAP'],
    api: { method: 'GET', path: '/doctors/1/work-schedules' },
    implemented: true
  },
  {
    path: 'admin/accounts', title: 'Tài khoản', icon: 'manage_accounts', roles: ['ADMIN'], part: 'P6',
    summary: 'Quản trị viên tạo tài khoản lễ tân, bác sĩ, quản trị kèm mật khẩu ban đầu; khóa / mở tài khoản.',
    tasks: [], api: { method: 'GET', path: '/accounts' }, implemented: true
  },
  {
    path: 'admin/reminders', title: 'Nhật ký thông báo', icon: 'notifications_active', roles: ['ADMIN', 'RECEPTIONIST'], part: 'P5',
    summary: 'Các thư nhắc lịch, xác nhận, báo hủy đã gửi / lỗi / bỏ qua; quản trị viên có nút quét ngay khi demo.',
    tasks: ['Bảng nhật ký (GET /api/v1/reminders/logs), lọc theo trạng thái và loại thư', 'Nút "Quét ngay" (POST /api/v1/reminders/run) — chỉ quản trị viên', 'Liên kết sang Mailpit http://localhost:8025 để xem thư'],
    api: { method: 'GET', path: '/reminders/logs' },
    implemented: true
  },
  {
    path: 'admin/message-templates', title: 'Mẫu thư', icon: 'mail', roles: ['ADMIN'], part: 'P5',
    summary: 'Quản trị viên sửa tiêu đề và nội dung của 3 loại thư: nhắc lịch, xác nhận, báo hủy.',
    tasks: ['Xem 3 mẫu (GET /api/v1/reminders/templates)', 'Sửa tiêu đề, nội dung một mẫu (PUT /api/v1/reminders/templates/{type})'],
    api: { method: 'GET', path: '/reminders/templates' },
    implemented: true
  }
];

export function pagesFor(role: Role): AppPage[] {
  return APP_PAGES.filter(p => p.roles.includes(role));
}
