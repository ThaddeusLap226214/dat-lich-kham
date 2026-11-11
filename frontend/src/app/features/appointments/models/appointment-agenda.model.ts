export interface DoctorInfo {
  id: number;
  name: string;
  title?: string;
  specialtyName?: string;
}

export interface RoomInfo {
  id: number;
  code: string;
  name: string;
}

export interface PatientInfo {
  id: number;
  fullName: string;
  phone?: string;
}

export interface AppointmentView {
  appointmentId: number;
  status: 'HELD' | 'CONFIRMED' | 'COMPLETED' | 'NO_SHOW' | 'EXPIRED' | 'CANCELLED';
  startTime: string;
  endTime: string;
  reason?: string;
  cancelReason?: string;
  doctor?: DoctorInfo | null;
  room?: RoomInfo | null;
  patient?: PatientInfo | null;
}
