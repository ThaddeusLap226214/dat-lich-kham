import { Role } from './identity.service';

export type Part = 'P1' | 'P2' | 'P3' | 'P4' | 'P5';

export const PARTS: Record<Part, string> = {
  P1: 'Patient Service (MPI)',
  P2: 'Scheduling Service',
  P3: 'Provider Service',
  P4: 'Auth Service + API Gateway',
  P5: 'Reminder Service'
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
    path: 'login', title: 'Đăng nhập', icon: 'login', roles: ['GUEST'], part: 'P4',
    summary: 'Đăng nhập bằng tên đăng nhập và mật khẩu, nhận JWT, lưu lại để gửi kèm mọi request.',
    tasks: [
      'Form đăng nhập gọi POST /api/v1/auth/login',
      'Lưu accessToken, gắn header Authorization: Bearer cho mọi request (thay cho chế độ thử)',
      'Token hết hạn hoặc API trả 401 → quay về trang đăng nhập'
    ],
    api: { method: 'POST', path: '/auth/login', body: { username: 'benhnhan', password: '123456' } }
  },
  {
    path: 'register', title: 'Đăng ký', icon: 'person_add', roles: ['GUEST'], part: 'P4',
    summary: 'Tạo tài khoản bệnh nhân mới.',
    tasks: ['Form đăng ký gọi POST /api/v1/auth/register', 'Hiện lỗi USERNAME_TAKEN, VALIDATION_FAILED'],
    api: { method: 'POST', path: '/auth/register', body: { username: 'thu.nghiem', password: '123456', fullName: 'Thử Nghiệm' } }
  },
  {
    path: 'doctors', title: 'Chuyên khoa & bác sĩ', icon: 'stethoscope', roles: ALL, part: 'P3',
    summary: 'Ai cũng xem được danh sách chuyên khoa và bác sĩ, không cần đăng nhập.',
    tasks: ['Danh sách chuyên khoa (GET /api/v1/specialties)', 'Lọc bác sĩ theo chuyên khoa (GET /api/v1/doctors?specialtyId=)', 'Xem lịch làm việc trong tuần của một bác sĩ'],
    api: { method: 'GET', path: '/doctors' }
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
    api: { method: 'GET', path: '/slots?doctorId=1&date={today}' }
  },
  {
    path: 'my-appointments', title: 'Lịch khám của tôi', icon: 'calendar_month', roles: ['PATIENT'], part: 'P4',
    summary: 'Danh sách lịch hẹn kèm tên bác sĩ, phòng, bệnh nhân — dữ liệu do Gateway ghép từ 3 service (API Composition).',
    tasks: ['Gateway ghép Scheduling + Provider + Patient (GET /api/v1/composite/my-appointments)', 'Nút hủy lịch (POST /api/v1/appointments/{id}/cancel)'],
    api: { method: 'GET', path: '/composite/my-appointments' }
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
    api: { method: 'GET', path: '/appointments?doctorId=1&date={today}' }
  },
  {
    path: 'doctor-agenda', title: 'Lịch khám trong ngày', icon: 'today', roles: ['DOCTOR'], part: 'P3',
    summary: 'Bác sĩ xem bệnh nhân của mình trong ngày và đánh dấu đã khám. Trang thuộc P3, API ghép thuộc P4.',
    tasks: ['Danh sách từ GET /api/v1/composite/doctor-agenda (P4 làm API)', 'Nút "Đã khám" (POST /api/v1/appointments/{id}/complete)'],
    api: { method: 'GET', path: '/composite/doctor-agenda?doctorId={doctorId}&date={today}' }
  },
  {
    path: 'admin/catalog', title: 'Chuyên khoa, bác sĩ, phòng', icon: 'medical_services', roles: ['ADMIN'], part: 'P3',
    summary: 'Quản trị viên thêm/sửa chuyên khoa, bác sĩ, phòng khám.',
    tasks: ['Bảng + form cho chuyên khoa, bác sĩ, phòng (POST/PUT /api/v1/specialties, /doctors, /rooms)'],
    api: { method: 'GET', path: '/rooms' }
  },
  {
    path: 'admin/work-schedules', title: 'Lịch làm việc bác sĩ', icon: 'schedule', roles: ['ADMIN'], part: 'P3',
    summary: 'Cấu hình ca làm việc hằng tuần: thứ, giờ bắt đầu – kết thúc, phòng, thời lượng một lượt khám.',
    tasks: ['Xem ca theo bác sĩ (GET /api/v1/doctors/{id}/work-schedules)', 'Thêm/sửa/ngừng ca, báo lỗi SCHEDULE_OVERLAP'],
    api: { method: 'GET', path: '/doctors/1/work-schedules' }
  },
  {
    path: 'admin/accounts', title: 'Tài khoản', icon: 'manage_accounts', roles: ['ADMIN'], part: 'P4',
    summary: 'Quản trị viên tạo tài khoản lễ tân, bác sĩ, quản trị.',
    tasks: ['Danh sách tài khoản (GET /api/v1/accounts)', 'Tạo tài khoản (POST /api/v1/accounts), tài khoản bác sĩ phải chọn doctorId'],
    api: { method: 'GET', path: '/accounts' }
  },
  {
    path: 'admin/reminders', title: 'Nhật ký nhắc lịch', icon: 'notifications_active', roles: ['ADMIN'], part: 'P5',
    summary: 'Các email nhắc lịch đã gửi / lỗi / bỏ qua; nút chạy quét ngay khi demo.',
    tasks: ['Bảng nhật ký (GET /api/v1/reminders/logs)', 'Nút "Quét ngay" (POST /api/v1/reminders/run)', 'Liên kết sang Mailpit http://localhost:8025 để xem thư'],
    api: { method: 'GET', path: '/reminders/logs' }
  }
];

export function pagesFor(role: Role): AppPage[] {
  return APP_PAGES.filter(p => p.roles.includes(role));
}
