import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiCall } from './app-pages';
import { IdentityService } from './identity.service';

export interface ProbeResult {
  url: string;
  status: number;
  body: unknown;
}

// Gọi thử một API qua Gateway và trả nguyên mã HTTP + nội dung, kể cả khi lỗi
@Injectable({ providedIn: 'root' })
export class ApiProbeService {
  private readonly http = inject(HttpClient);
  private readonly identity = inject(IdentityService);

  resolveUrl(call: ApiCall): string {
    const today = new Date().toLocaleDateString('sv-SE');
    const doctorId = this.identity.identity().doctorId ?? 1;
    const path = call.path.replace('{today}', today).replace('{doctorId}', String(doctorId));
    return environment.apiUrl + path;
  }

  probe(call: ApiCall): Observable<ProbeResult> {
    const url = this.resolveUrl(call);
    const request$ = call.method === 'POST'
      ? this.http.post(url, call.body ?? {}, { observe: 'response' })
      : this.http.get(url, { observe: 'response' });
    return request$.pipe(
      map((res: HttpResponse<unknown>) => ({ url, status: res.status, body: res.body })),
      catchError((err: HttpErrorResponse) => of({ url, status: err.status, body: err.error }))
    );
  }
}
