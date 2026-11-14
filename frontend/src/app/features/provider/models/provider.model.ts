export interface Specialty {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  active: boolean;
}

export interface SpecialtyRequest {
  code: string;
  name: string;
  description?: string | null;
  active?: boolean;
}

export interface Room {
  id: number;
  code: string;
  name: string;
  location?: string | null;
  active: boolean;
}

export interface RoomRequest {
  code: string;
  name: string;
  location?: string | null;
  active?: boolean;
}

export interface Doctor {
  id: number;
  fullName: string;
  title?: string | null;
  specialtyId: number;
  specialtyName: string;
  phone?: string | null;
  email?: string | null;
  active: boolean;
}

export interface DoctorRequest {
  fullName: string;
  title?: string | null;
  specialtyId: number;
  phone?: string | null;
  email?: string | null;
  active?: boolean;
}

export interface WorkSchedule {
  id: number;
  doctorId: number;
  roomId: number;
  roomName: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotMinutes: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  active: boolean;
}

export interface WorkScheduleRequest {
  roomId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotMinutes: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  active?: boolean;
}

export interface DoctorDayOff {
  id: number;
  doctorId: number;
  offDate: string;
  reason?: string | null;
}

export interface DoctorDayOffRequest {
  offDate: string;
  reason?: string | null;
}

export const DAY_OF_WEEK_OPTIONS: { value: number; label: string }[] = [
  { value: 1, label: 'Thứ Hai' },
  { value: 2, label: 'Thứ Ba' },
  { value: 3, label: 'Thứ Tư' },
  { value: 4, label: 'Thứ Năm' },
  { value: 5, label: 'Thứ Sáu' },
  { value: 6, label: 'Thứ Bảy' },
  { value: 7, label: 'Chủ nhật' }
];

export function dayOfWeekLabel(day: number): string {
  return DAY_OF_WEEK_OPTIONS.find(d => d.value === day)?.label ?? `Thứ ${day + 1}`;
}

