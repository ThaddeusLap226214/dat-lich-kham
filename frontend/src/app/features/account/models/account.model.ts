// Khớp DTO của Auth Service (docs/03-hop-dong-api.md B1)
export type AccountRole = 'PATIENT' | 'RECEPTIONIST' | 'DOCTOR' | 'ADMIN';

export interface Account {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  role: AccountRole;
  doctorId: number | null;
  enabled: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  fullName: string;
  email: string | null;
  phone: string | null;
}

export interface CreateAccountRequest extends RegisterRequest {
  role: AccountRole;
  doctorId: number | null;
}

// Rút gọn từ DoctorResponse của Provider, chỉ dùng để chọn bác sĩ khi tạo tài khoản DOCTOR
export interface DoctorOption {
  id: number;
  fullName: string;
  specialtyName: string;
}

export const ROLE_OPTIONS: { value: AccountRole; label: string }[] = [
  { value: 'RECEPTIONIST', label: 'Lễ tân' },
  { value: 'DOCTOR', label: 'Bác sĩ' },
  { value: 'ADMIN', label: 'Quản trị viên' },
  { value: 'PATIENT', label: 'Bệnh nhân' }
];
