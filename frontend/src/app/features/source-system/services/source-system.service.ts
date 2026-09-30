import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models';
import {
  CreateSourceSystemRequest,
  SourceSystem,
  UpdateSourceSystemRequest
} from '../models/source-system.model';
import { SourceSystemSearchParams } from '../models/source-system-search-params.model';

@Injectable({
  providedIn: 'root'
})
export class SourceSystemService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/source-systems';

  search(params: SourceSystemSearchParams): Observable<ApiResponse<PageResponse<SourceSystem>>> {
    let httpParams = new HttpParams();

    if (params.name) {
      httpParams = httpParams.set('name', params.name);
    }
    if (params.code) {
      httpParams = httpParams.set('code', params.code);
    }
    if (params.facilityId != null && params.facilityId != undefined) {
      httpParams = httpParams.set('facilityId', params.facilityId.toString());
    }
    if (params.isActive !== undefined && params.isActive !== null) {
      httpParams = httpParams.set('isActive', params.isActive.toString());
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<ApiResponse<PageResponse<SourceSystem>>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  getById(id: number): Observable<ApiResponse<SourceSystem>> {
    return this.http.get<ApiResponse<SourceSystem>>(`${this.apiUrl}${this.basePath}/${id}`);
  }

  create(request: CreateSourceSystemRequest): Observable<ApiResponse<SourceSystem>> {
    return this.http.post<ApiResponse<SourceSystem>>(`${this.apiUrl}${this.basePath}`, request);
  }

  update(request: UpdateSourceSystemRequest): Observable<ApiResponse<SourceSystem>> {
    return this.http.put<ApiResponse<SourceSystem>>(`${this.apiUrl}${this.basePath}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}${this.basePath}/${id}`);
  }
}
