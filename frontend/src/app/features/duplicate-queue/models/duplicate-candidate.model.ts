import { Patient, SpringPage, MergeHistoryResponse } from '../../patient/models/patient.model';

export enum CandidateStatus {
  PENDING = 'PENDING',
  MERGED = 'MERGED',
  NOT_DUPLICATE = 'NOT_DUPLICATE'
}

export enum DuplicateReason {
  SAME_ID_DIFFERENT_INFO = 'SAME_ID_DIFFERENT_INFO',
  SAME_NAME_DOB_PHONE = 'SAME_NAME_DOB_PHONE',
  ID_ADDED_BELONGS_TO_OTHER = 'ID_ADDED_BELONGS_TO_OTHER'
}

export const DUPLICATE_REASON_LABELS: Record<DuplicateReason, string> = {
  SAME_ID_DIFFERENT_INFO: 'Trùng số giấy tờ nhưng khác thông tin',
  SAME_NAME_DOB_PHONE: 'Trùng họ tên, ngày sinh và số điện thoại',
  ID_ADDED_BELONGS_TO_OTHER: 'Giấy tờ bổ sung đã thuộc hồ sơ khác'
};

export interface DuplicateCandidate {
  id: number;
  reason: DuplicateReason;
  score: number;
  status: CandidateStatus;
  patient: Patient;
  candidatePatient: Patient;
  reviewedByAccountId: number | null;
  reviewedAt: string | null;
  reviewNote: string | null;
  createdAt: string;
}

export interface MergeRequest {
  keepPatientId: number;
  note?: string;
}

export interface ReviewNoteRequest {
  note?: string;
}

export interface DuplicateQueueSearchParams {
  status?: CandidateStatus;
  page?: number;
  size?: number;
}
