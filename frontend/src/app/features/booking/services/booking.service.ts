import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Specialty, Doctor, Slot, HoldRequest, AppointmentResponse } from '../models/booking.model';
import { Gender, Patient, PatientStatus } from '../../patient/models/patient.model';

const MOCK_SPECIALTIES: Specialty[] = [
  { id: 1, code: 'NOI', name: 'Nội tổng quát', description: 'Khám và điều trị các bệnh nội khoa chung' },
  { id: 2, code: 'TMH', name: 'Tai Mũi Họng', description: 'Bệnh lý tai, mũi, họng người lớn và trẻ em' },
  { id: 3, code: 'MAT', name: 'Mắt', description: 'Khám và tư vấn thị lực, bệnh về mắt' },
  { id: 4, code: 'DALIEU', name: 'Da liễu', description: 'Chăm sóc và điều trị bệnh ngoài da' },
  { id: 5, code: 'RHM', name: 'Răng Hàm Mặt', description: 'Nha khoa tổng quát và phục hình' }
];

const MOCK_DOCTORS: Doctor[] = [
  { id: 1, specialtyId: 1, name: 'BS. CK1 Nguyễn Văn An', title: 'Bác sĩ chuyên khoa 1', email: 'an.nguyen@clinic.vn', phone: '0905111001', active: true },
  { id: 2, specialtyId: 1, name: 'ThS. BS Trần Thị Bình', title: 'Thạc sĩ bác sĩ', email: 'binh.tran@clinic.vn', phone: '0905111002', active: true },
  { id: 3, specialtyId: 2, name: 'BS. Lê Văn Cường', title: 'Bác sĩ', email: 'cuong.le@clinic.vn', phone: '0905111003', active: true },
  { id: 4, specialtyId: 3, name: 'BS. CK2 Phạm Thị Dung', title: 'Bác sĩ chuyên khoa 2', email: 'dung.pham@clinic.vn', phone: '0905111004', active: true },
  { id: 5, specialtyId: 4, name: 'BS. Hoàng Văn Em', title: 'Bác sĩ', email: 'em.hoang@clinic.vn', phone: '0905111005', active: true },
  { id: 6, specialtyId: 5, name: 'ThS. BS Vũ Thị Giang', title: 'Thạc sĩ bác sĩ', email: 'giang.vu@clinic.vn', phone: '0905111006', active: true }
];

const MOCK_PATIENTS: Patient[] = [
  {
    id: 1,
    ownerAccountId: 5,
    fullName: 'Nguyễn Văn A',
    dateOfBirth: '1990-01-01',
    gender: Gender.MALE,
    phone: '0905123456',
    email: 'a@example.com',
    address: 'Đà Nẵng',
    idType: null,
    idNumber: '048090000001',
    guardianName: null,
    guardianPhone: null,
    guardianRelation: null,
    status: PatientStatus.ACTIVE,
    mergedIntoId: null,
    createdAt: '2026-10-01T08:00:00'
  },
  {
    id: 2,
    ownerAccountId: 5,
    fullName: 'Trần Thị B',
    dateOfBirth: '1995-05-15',
    gender: Gender.FEMALE,
    phone: '0905234567',
    email: 'b@example.com',
    address: 'Đà Nẵng',
    idType: null,
    idNumber: '048195000002',
    guardianName: null,
    guardianPhone: null,
    guardianRelation: null,
    status: PatientStatus.ACTIVE,
    mergedIntoId: null,
    createdAt: '2026-10-01T08:00:00'
  }
];

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getSpecialties(): Observable<Specialty[]> {
    return this.http.get<Specialty[]>(`${this.apiUrl}/specialties`).pipe(
      catchError(() => of(MOCK_SPECIALTIES))
    );
  }

  getDoctors(specialtyId?: number): Observable<Doctor[]> {
    let params = new HttpParams();
    if (specialtyId !== undefined && specialtyId !== null) {
      params = params.set('specialtyId', specialtyId.toString());
    }
    return this.http.get<Doctor[]>(`${this.apiUrl}/doctors`, { params }).pipe(
      catchError(() => {
        if (specialtyId) {
          return of(MOCK_DOCTORS.filter(d => d.specialtyId === specialtyId));
        }
        return of(MOCK_DOCTORS);
      })
    );
  }

  getSlots(doctorId: number, date: string): Observable<Slot[]> {
    const params = new HttpParams()
      .set('doctorId', doctorId.toString())
      .set('date', date);
    return this.http.get<Slot[]>(`${this.apiUrl}/slots`, { params });
  }

  getMyPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.apiUrl}/patients/mine`).pipe(
      catchError(() => of(MOCK_PATIENTS))
    );
  }

  hold(request: HoldRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.apiUrl}/appointments/hold`, request);
  }

  confirm(id: number): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${this.apiUrl}/appointments/${id}/confirm`, {});
  }
}
