export type ReminderStatus = 'SENT' | 'FAILED' | 'SKIPPED';

export type ReminderType = 'BEFORE_APPOINTMENT' | 'BEFORE_APPOINTMENT_2H' | 'CONFIRMATION' | 'CANCELLATION';

export interface ReminderLog {
  id: number;
  appointmentId: number;
  patientId: number;
  appointmentStart: string;
  reminderType: ReminderType;
  recipientEmail: string | null;
  status: ReminderStatus;
  attemptCount: number;
  lastError: string | null;
  sentAt: string | null;
  createdAt: string;
}

export interface RunResult {
  checked: number;
  sent: number;
  skipped: number;
  failed: number;
}

export interface MessageTemplate {
  type: ReminderType;
  subject: string;
  body: string;
  updatedByAccountId: number | null;
  updatedAt: string;
}

export interface UpdateTemplateRequest {
  subject: string;
  body: string;
}
