import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MessageTemplate, ReminderLog, ReminderStatus, ReminderType, RunResult } from '../models/reminder.model';

const MOCK_TEMPLATES: MessageTemplate[] = [
  {
    type: 'BEFORE_APPOINTMENT',
    subject: 'Nhắc lịch khám lúc {startTime}',
    body: 'Kính gửi {patientName},\n\nPhòng khám nhắc bạn có lịch khám lúc {startTime} (mã lịch hẹn {appointmentId}).\nVui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân.\n\nTrân trọng.',
    updatedByAccountId: null,
    updatedAt: new Date().toISOString()
  },
  {
    type: 'CONFIRMATION',
    subject: 'Xác nhận lịch khám số {appointmentId}',
    body: 'Kính gửi {patientName},\n\nLịch khám của bạn lúc {startTime} đã được xác nhận (mã lịch hẹn {appointmentId}).\nVui lòng đến trước giờ hẹn 15 phút và mang theo giấy tờ tùy thân.\n\nTrân trọng.',
    updatedByAccountId: null,
    updatedAt: new Date().toISOString()
  },
  {
    type: 'CANCELLATION',
    subject: 'Lịch khám số {appointmentId} đã được hủy',
    body: 'Kính gửi {patientName},\n\nLịch khám lúc {startTime} (mã lịch hẹn {appointmentId}) đã được hủy.\nLý do: {cancelReason}\nNếu cần khám, vui lòng đặt lịch mới trên hệ thống.\n\nTrân trọng.',
    updatedByAccountId: null,
    updatedAt: new Date().toISOString()
  }
];

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

    return this.http.get<ReminderLog[]>(`${this.apiUrl}/reminders/logs`, { params }).pipe(
      catchError(() => of([]))
    );
  }

  runNow(): Observable<RunResult> {
    return this.http.post<RunResult>(`${this.apiUrl}/reminders/run`, {});
  }

  getTemplates(): Observable<MessageTemplate[]> {
    return this.http.get<MessageTemplate[]>(`${this.apiUrl}/reminders/templates`).pipe(
      catchError(() => of(MOCK_TEMPLATES))
    );
  }

  updateTemplate(type: ReminderType, subject: string, body: string): Observable<MessageTemplate> {
    return this.http.put<MessageTemplate>(`${this.apiUrl}/reminders/templates/${type}`, { subject, body });
  }
}
