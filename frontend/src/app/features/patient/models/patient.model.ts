import { Gender } from '../../../shared/enums';

export { Gender };

export enum PatientStatus {
  TEMPORARY = 'TEMPORARY',
  ACTIVE = 'ACTIVE',
  MERGED = 'MERGED'
}

export enum IdType {
  CCCD = 'CCCD',
  PASSPORT = 'PASSPORT',
  BHYT = 'BHYT'
}

export interface Patient {
  id: number;
  ownerAccountId: number | null;
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  phone: string | null;
  email: string | null;
  address: string | null;
  idType: IdType | null;
  idNumber: string | null;
  guardianName: string | null;
  guardianPhone: string | null;
  guardianRelation: string | null;
  status: PatientStatus;
  mergedIntoId: number | null;
  createdAt: string;
}

// Dùng cho cả POST (tạo) và PUT /{id} (sửa); khi sửa, backend bỏ qua idType/idNumber
export interface PatientRequest {
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  phone: string | null;
  email: string | null;
  address: string | null;
  idType: IdType | null;
  idNumber: string | null;
  guardianName: string | null;
  guardianPhone: string | null;
  guardianRelation: string | null;
}

export interface CreatePatientResponse {
  patient: Patient;
  outcome: 'REUSED_EXISTING' | 'CREATED_ACTIVE' | 'CREATED_TEMPORARY';
  duplicateQueued: boolean;
}

// Dạng trang Spring trả về: PageResponse<T>(content, page từ 0, size, totalElements, totalPages)
export interface SpringPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
