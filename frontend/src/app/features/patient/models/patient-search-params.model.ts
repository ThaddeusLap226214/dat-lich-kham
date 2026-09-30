import { PatientStatus } from './patient.model';

export { PatientStatus };

// Khớp GET /api/v1/patients: keyword tìm trong họ tên (có dấu/không dấu), số giấy tờ, số điện thoại
export interface PatientSearchParams {
  keyword?: string;
  status?: PatientStatus;
  page?: number;
  size?: number;
}

export const defaultSearchParams: PatientSearchParams = {
  page: 0,
  size: 10
};
