import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { SpringPage, MergeHistoryResponse } from '../../patient/models/patient.model';
import {
  CandidateStatus,
  DuplicateCandidate,
  DuplicateQueueSearchParams,
  MergeRequest,
  ReviewNoteRequest
} from '../models/duplicate-candidate.model';

@Injectable({
  providedIn: 'root'
})
export class DuplicateQueueService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/duplicate-candidates';

  list(params: DuplicateQueueSearchParams = {}): Observable<SpringPage<DuplicateCandidate>> {
    let httpParams = new HttpParams();

    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<SpringPage<DuplicateCandidate>>(`${this.apiUrl}${this.basePath}`, {
      params: httpParams
    });
  }

  count(status: CandidateStatus = CandidateStatus.PENDING): Observable<{ count: number }> {
    const params = new HttpParams().set('status', status);
    return this.http.get<{ count: number }>(`${this.apiUrl}${this.basePath}/count`, { params });
  }

  merge(id: number, request: MergeRequest): Observable<MergeHistoryResponse> {
    return this.http.post<MergeHistoryResponse>(`${this.apiUrl}${this.basePath}/${id}/merge`, request);
  }

  reject(id: number, request: ReviewNoteRequest): Observable<DuplicateCandidate> {
    return this.http.post<DuplicateCandidate>(`${this.apiUrl}${this.basePath}/${id}/reject`, request);
  }

  unmerge(patientId: number, request: ReviewNoteRequest): Observable<MergeHistoryResponse> {
    return this.http.post<MergeHistoryResponse>(`${this.apiUrl}/patients/${patientId}/unmerge`, request);
  }

  getMergeHistory(patientId: number): Observable<MergeHistoryResponse[]> {
    return this.http.get<MergeHistoryResponse[]>(`${this.apiUrl}/patients/${patientId}/merge-history`);
  }
}
