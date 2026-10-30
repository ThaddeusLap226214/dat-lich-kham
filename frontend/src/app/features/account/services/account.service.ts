import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Account, AccountRole, CreateAccountRequest, DoctorOption, RegisterRequest } from '../models/account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  register(request: RegisterRequest): Observable<Account> {
    return this.http.post<Account>(`${this.api}/auth/register`, request);
  }

  list(role: AccountRole | null): Observable<Account[]> {
    const params = role ? new HttpParams().set('role', role) : undefined;
    return this.http.get<Account[]>(`${this.api}/accounts`, { params });
  }

  create(request: CreateAccountRequest): Observable<Account> {
    return this.http.post<Account>(`${this.api}/accounts`, request);
  }

  lock(id: number): Observable<Account> {
    return this.http.post<Account>(`${this.api}/accounts/${id}/lock`, null);
  }

  unlock(id: number): Observable<Account> {
    return this.http.post<Account>(`${this.api}/accounts/${id}/unlock`, null);
  }

  // Danh sách bác sĩ thuộc Provider (P3); khi API đó chưa làm, trang Tài khoản cho nhập mã bác sĩ bằng tay
  doctors(): Observable<DoctorOption[]> {
    return this.http.get<DoctorOption[]>(`${this.api}/doctors`);
  }
}
