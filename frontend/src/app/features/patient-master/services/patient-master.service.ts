import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, PageResponse } from '../../../shared/models';
import { PatientMaster, PatientMasterSearchRequest } from '../models/patient-master.model';

@Injectable({
  providedIn: 'root'
})
export class PatientMasterService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/patient-masters';

  search(params: PatientMasterSearchRequest): Observable<ApiResponse<PageResponse<PatientMaster>>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    if (params.keyword) {
      httpParams = httpParams.set('keyword', params.keyword);
    }
    if (params.gender) {
      httpParams = httpParams.set('gender', params.gender);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.ageFrom !== undefined) {
      httpParams = httpParams.set('ageFrom', params.ageFrom.toString());
    }
    if (params.ageTo !== undefined) {
      httpParams = httpParams.set('ageTo', params.ageTo.toString());
    }

    return this.http.get<ApiResponse<PageResponse<PatientMaster>>>(this.apiUrl, { params: httpParams });
  }

  getById(id: number): Observable<ApiResponse<PatientMaster>> {
    return this.http.get<ApiResponse<PatientMaster>>(`${this.apiUrl}/${id}`);
  }

  update(id: number, data: Partial<PatientMaster>): Observable<ApiResponse<PatientMaster>> {
    return this.http.put<ApiResponse<PatientMaster>>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }
}
