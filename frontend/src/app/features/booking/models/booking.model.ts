export interface Specialty {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export interface Doctor {
  id: number;
  specialtyId: number;
  fullName: string;
  title?: string;
  phone?: string;
  email?: string;
  active?: boolean;
}

export interface Slot {
  startTime: string;
  endTime: string;
  roomId: number;
  workScheduleId: number;
  available: boolean;
}

export interface HoldRequest {
  doctorId: number;
  patientId: number;
  startTime: string;
  reason?: string;
}

export interface AppointmentResponse {
  id: number;
  patientId: number;
  doctorId: number;
  roomId: number;
  workScheduleId: number;
  bookedByAccountId: number;
  startTime: string;
  endTime: string;
  status: 'HELD' | 'CONFIRMED' | 'COMPLETED' | 'NO_SHOW' | 'EXPIRED' | 'CANCELLED';
  holdExpiresAt?: string;
  reason?: string;
  cancelReason?: string;
  cancelledByAccountId?: number;
  confirmedAt?: string;
  cancelledAt?: string;
  completedAt?: string;
  noShowAt?: string;
  createdAt?: string;
}
