import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Doctor,
  DoctorDayOff,
  DoctorDayOffRequest,
  DoctorRequest,
  Room,
  RoomRequest,
  Specialty,
  SpecialtyRequest,
  WorkSchedule,
  WorkScheduleRequest
} from '../models/provider.model';

@Injectable({
  providedIn: 'root'
})
export class ProviderCatalogService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  listSpecialties(): Observable<Specialty[]> {
    return this.http.get<Specialty[]>(`${this.apiUrl}/specialties`);
  }

  createSpecialty(request: SpecialtyRequest): Observable<Specialty> {
    return this.http.post<Specialty>(`${this.apiUrl}/specialties`, request);
  }

  updateSpecialty(id: number, request: SpecialtyRequest): Observable<Specialty> {
    return this.http.put<Specialty>(`${this.apiUrl}/specialties/${id}`, request);
  }

  listRooms(): Observable<Room[]> {
    return this.http.get<Room[]>(`${this.apiUrl}/rooms`);
  }

  createRoom(request: RoomRequest): Observable<Room> {
    return this.http.post<Room>(`${this.apiUrl}/rooms`, request);
  }

  updateRoom(id: number, request: RoomRequest): Observable<Room> {
    return this.http.put<Room>(`${this.apiUrl}/rooms/${id}`, request);
  }

  listDoctors(specialtyId?: number | null): Observable<Doctor[]> {
    let params = new HttpParams();
    if (specialtyId != null) {
      params = params.set('specialtyId', specialtyId.toString());
    }
    return this.http.get<Doctor[]>(`${this.apiUrl}/doctors`, { params });
  }

  createDoctor(request: DoctorRequest): Observable<Doctor> {
    return this.http.post<Doctor>(`${this.apiUrl}/doctors`, request);
  }

  updateDoctor(id: number, request: DoctorRequest): Observable<Doctor> {
    return this.http.put<Doctor>(`${this.apiUrl}/doctors/${id}`, request);
  }

  listWorkSchedules(doctorId: number): Observable<WorkSchedule[]> {
    return this.http.get<WorkSchedule[]>(`${this.apiUrl}/doctors/${doctorId}/work-schedules`);
  }

  createWorkSchedule(doctorId: number, request: WorkScheduleRequest): Observable<WorkSchedule> {
    return this.http.post<WorkSchedule>(`${this.apiUrl}/doctors/${doctorId}/work-schedules`, request);
  }

  updateWorkSchedule(id: number, request: WorkScheduleRequest): Observable<WorkSchedule> {
    return this.http.put<WorkSchedule>(`${this.apiUrl}/work-schedules/${id}`, request);
  }

  deleteWorkSchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/work-schedules/${id}`);
  }

  listDayOffs(doctorId: number): Observable<DoctorDayOff[]> {
    return this.http.get<DoctorDayOff[]>(`${this.apiUrl}/doctors/${doctorId}/day-offs`);
  }

  addDayOff(doctorId: number, request: DoctorDayOffRequest): Observable<DoctorDayOff> {
    return this.http.post<DoctorDayOff>(`${this.apiUrl}/doctors/${doctorId}/day-offs`, request);
  }

  deleteDayOff(doctorId: number, dayOffId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/doctors/${doctorId}/day-offs/${dayOffId}`);
  }
}

