import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  AddIdentityRequest,
  CreatePatientResponse,
  MergeHistoryResponse,
  Patient,
  PatientRequest,
  ReviewNoteRequest,
  SpringPage
} from '../models/patient.model';
import { PatientSearchParams } from '../models/patient-search-params.model';

@Injectable({
  providedIn: 'root'
})
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/patients';

  search(params: PatientSearchParams): Observable<SpringPage<Patient>> {
    let httpParams = new HttpParams();

    if (params.keyword) {
      httpParams = httpParams.set('keyword', params.keyword);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<SpringPage<Patient>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  mine(): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.apiUrl}${this.basePath}/mine`);
  }

  getById(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  create(request: PatientRequest): Observable<CreatePatientResponse> {
    return this.http.post<CreatePatientResponse>(`${this.apiUrl}${this.basePath}`, request);
  }

  update(id: number, request: PatientRequest): Observable<Patient> {
    return this.http.put<Patient>(`${this.apiUrl}${this.basePath}/${id}`, request);
  }

  addIdentity(id: number, request: AddIdentityRequest): Observable<Patient> {
    return this.http.post<Patient>(`${this.apiUrl}${this.basePath}/${id}/identity`, request);
  }

  unmerge(id: number, request: ReviewNoteRequest): Observable<MergeHistoryResponse> {
    return this.http.post<MergeHistoryResponse>(`${this.apiUrl}${this.basePath}/${id}/unmerge`, request);
  }

  getMergeHistory(id: number): Observable<MergeHistoryResponse[]> {
    return this.http.get<MergeHistoryResponse[]>(`${this.apiUrl}${this.basePath}/${id}/merge-history`);
  }
}
