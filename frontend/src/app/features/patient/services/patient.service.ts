import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreatePatientResponse,
  Patient,
  PatientRequest,
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

  getById(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  create(request: PatientRequest): Observable<CreatePatientResponse> {
    return this.http.post<CreatePatientResponse>(`${this.apiUrl}${this.basePath}`, request);
  }

  update(id: number, request: PatientRequest): Observable<Patient> {
    return this.http.put<Patient>(`${this.apiUrl}${this.basePath}/${id}`, request);
  }
}
