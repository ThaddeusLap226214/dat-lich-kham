import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageTemplate, ReminderLog, ReminderStatus, ReminderType, RunResult } from '../models/reminder.model';

@Injectable({
  providedIn: 'root'
})
export class ReminderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getLogs(status?: ReminderStatus | '', type?: ReminderType | ''): Observable<ReminderLog[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (type) {
      params = params.set('type', type);
    }

    return this.http.get<ReminderLog[]>(`${this.apiUrl}/reminders/logs`, { params });
  }

  runNow(): Observable<RunResult> {
    return this.http.post<RunResult>(`${this.apiUrl}/reminders/run`, {});
  }

  getTemplates(): Observable<MessageTemplate[]> {
    return this.http.get<MessageTemplate[]>(`${this.apiUrl}/reminders/templates`);
  }

  updateTemplate(type: ReminderType, subject: string, body: string): Observable<MessageTemplate> {
    return this.http.put<MessageTemplate>(`${this.apiUrl}/reminders/templates/${type}`, { subject, body });
  }
}
