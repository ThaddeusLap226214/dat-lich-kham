import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models';
import {
  CreateFacilityRequest,
  Facility,
  UpdateFacilityRequest
} from '../models/facility.model';
import { FacilitySearchParams } from '../models/facility-search-params.model';

@Injectable({
  providedIn: 'root'
})
export class FacilityService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/facilities';

  search(params: FacilitySearchParams): Observable<ApiResponse<PageResponse<Facility>>> {
    let httpParams = new HttpParams();

    if (params.name) {
      httpParams = httpParams.set('name', params.name);
    }
    if (params.code) {
      httpParams = httpParams.set('code', params.code);
    }
    if (params.facilityType) {
      httpParams = httpParams.set('facilityType', params.facilityType);
    }
    if (params.isActive !== undefined) {
      httpParams = httpParams.set('isActive', params.isActive.toString());
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<ApiResponse<PageResponse<Facility>>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  getById(id: number): Observable<ApiResponse<Facility>> {
    return this.http.get<ApiResponse<Facility>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  create(request: CreateFacilityRequest): Observable<ApiResponse<Facility>> {
    return this.http.post<ApiResponse<Facility>>(`${this.apiUrl}${this.basePath}`, request);
  }

  update(request: UpdateFacilityRequest): Observable<ApiResponse<Facility>> {
    return this.http.put<ApiResponse<Facility>>(`${this.apiUrl}${this.basePath}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}${this.basePath}/${id}`);
  }
}
