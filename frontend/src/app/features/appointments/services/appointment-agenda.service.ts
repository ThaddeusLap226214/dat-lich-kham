import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { mockWhenNotImplemented } from '../../../shared/services/mock-when-not-implemented';
import { AppointmentView } from '../models/appointment-agenda.model';
import { Doctor } from '../../booking/models/booking.model';

const MOCK_DOCTORS: Doctor[] = [
  { id: 1, specialtyId: 1, fullName: 'BS. CK1 Nguyễn Văn An', title: 'Bác sĩ chuyên khoa 1', active: true },
  { id: 2, specialtyId: 1, fullName: 'ThS. BS Trần Thị Bình', title: 'Thạc sĩ bác sĩ', active: true },
  { id: 3, specialtyId: 2, fullName: 'BS. Lê Văn Cường', title: 'Bác sĩ', active: true },
  { id: 4, specialtyId: 3, fullName: 'BS. CK2 Phạm Thị Dung', title: 'Bác sĩ chuyên khoa 2', active: true },
  { id: 5, specialtyId: 4, fullName: 'BS. Hoàng Văn Em', title: 'Bác sĩ', active: true },
  { id: 6, specialtyId: 5, fullName: 'ThS. BS Vũ Thị Giang', title: 'Thạc sĩ bác sĩ', active: true }
];

@Injectable({
  providedIn: 'root'
})
export class AppointmentAgendaService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getDoctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.apiUrl}/doctors`).pipe(
      mockWhenNotImplemented(() => MOCK_DOCTORS)
    );
  }

  getMyAppointments(): Observable<AppointmentView[]> {
    return this.http.get<AppointmentView[]>(`${this.apiUrl}/composite/my-appointments`);
  }

  getDoctorAgenda(doctorId: number, date: string): Observable<AppointmentView[]> {
    const params = new HttpParams()
      .set('doctorId', doctorId.toString())
      .set('date', date);

    // Ưu tiên gọi API Composite của Gateway (P4-6)
    return this.http.get<AppointmentView[]>(`${this.apiUrl}/composite/doctor-agenda`, { params }).pipe(
      catchError(() => {
        // Fallback gọi trực tiếp Scheduling Service (P2-1 / P2-9); lỗi tiếp thì để trang báo lỗi, không trả bảng rỗng
        return this.http.get<any[]>(`${this.apiUrl}/appointments`, { params }).pipe(
          map(items => items.map(item => this.mapToView(item, doctorId)))
        );
      })
    );
  }

  cancel(id: number, reason: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/appointments/${id}/cancel`, { reason });
  }

  complete(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/appointments/${id}/complete`, {});
  }

  noShow(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/appointments/${id}/no-show`, {});
  }

  private mapToView(item: any, doctorId: number): AppointmentView {
    const doc = MOCK_DOCTORS.find(d => d.id === doctorId);
    return {
      appointmentId: item.id,
      status: item.status,
      startTime: item.startTime,
      endTime: item.endTime,
      reason: item.reason,
      cancelReason: item.cancelReason,
      doctor: doc ? { id: doc.id, fullName: doc.fullName, title: doc.title } : { id: doctorId, fullName: `Bác sĩ #${doctorId}` },
      room: { id: item.roomId, code: `P${item.roomId}`, name: `Phòng khám ${item.roomId}` },
      patient: { id: item.patientId, fullName: `Hồ sơ #${item.patientId}` }
    };
  }
}
