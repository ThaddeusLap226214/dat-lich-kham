import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/models';
import {
  FileValidationResponse,
  ImportJob,
  ImportJobDetail,
  ImportRowStatus,
  PreviewRow,
  StartImportRequest
} from '../models/import.model';

@Injectable({
  providedIn: 'root'
})
export class ImportService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  private readonly basePath = '/imports';

  /**
   * Upload and validate file
   */
  uploadFile(file: File, sourceSystemId: number): Observable<ApiResponse<FileValidationResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sourceSystemId', sourceSystemId.toString());

    return this.http.post<ApiResponse<FileValidationResponse>>(
      `${this.apiUrl}${this.basePath}/upload`,
      formData
    );
  }

  /**
   * Preview import data
   */
  previewImportData(jobId: number, page: number = 0, size: number = 10): Observable<ApiResponse<PageResponse<PreviewRow>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PageResponse<PreviewRow>>>(
      `${this.apiUrl}${this.basePath}/${jobId}/preview`,
      { params }
    );
  }

  /**
   * Start import process
   */
  startImport(request: StartImportRequest): Observable<ApiResponse<ImportJob>> {
    return this.http.post<ApiResponse<ImportJob>>(
      `${this.apiUrl}${this.basePath}/start`,
      request
    );
  }

  /**
   * Get import job status
   */
  getImportJobStatus(jobId: number): Observable<ApiResponse<ImportJob>> {
    return this.http.get<ApiResponse<ImportJob>>(
      `${this.apiUrl}${this.basePath}/${jobId}`
    );
  }

  /**
   * Get import job details
   */
  getImportJobDetails(
    jobId: number,
    status?: ImportRowStatus,
    page: number = 0,
    size: number = 20
  ): Observable<ApiResponse<PageResponse<ImportJobDetail>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<ImportJobDetail>>>(
      `${this.apiUrl}${this.basePath}/${jobId}/details`,
      { params }
    );
  }

  /**
   * Search import jobs
   */
  searchImportJobs(
    params: {
      fileName?: string;
      status?: string;
      sourceSystemId?: number;
      page?: number;
      size?: number;
    }
  ): Observable<ApiResponse<PageResponse<ImportJob>>> {
    let httpParams = new HttpParams();

    if (params.fileName) {
      httpParams = httpParams.set('fileName', params.fileName);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.sourceSystemId !== undefined) {
      httpParams = httpParams.set('sourceSystemId', params.sourceSystemId.toString());
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<ApiResponse<PageResponse<ImportJob>>>(
      `${this.apiUrl}${this.basePath}`,
      { params: httpParams }
    );
  }

  /**
   * Cancel import job
   */
  cancelImportJob(jobId: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}${this.basePath}/${jobId}/cancel`,
      {}
    );
  }

  /**
   * Retry failed rows
   */
  retryFailedRows(jobId: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}${this.basePath}/${jobId}/retry-failed`,
      {}
    );
  }
}
