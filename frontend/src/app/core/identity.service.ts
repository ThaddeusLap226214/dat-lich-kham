import { Injectable, computed, signal } from '@angular/core';

export type Role = 'GUEST' | 'PATIENT' | 'RECEPTIONIST' | 'DOCTOR' | 'ADMIN';

export interface DemoIdentity {
  key: string;
  label: string;
  role: Role;
  accountId: number | null;
  doctorId: number | null;
}

// Khớp tài khoản mẫu trong auth-service/src/main/resources/data.sql
export const DEMO_IDENTITIES: DemoIdentity[] = [
  { key: 'guest', label: 'Khách (chưa đăng nhập)', role: 'GUEST', accountId: null, doctorId: null },
  { key: 'benhnhan', label: 'benhnhan — Bệnh nhân', role: 'PATIENT', accountId: 5, doctorId: null },
  { key: 'letan', label: 'letan — Lễ tân', role: 'RECEPTIONIST', accountId: 2, doctorId: null },
  { key: 'bs.an', label: 'bs.an — Bác sĩ', role: 'DOCTOR', accountId: 3, doctorId: 1 },
  { key: 'admin', label: 'admin — Quản trị', role: 'ADMIN', accountId: 1, doctorId: null }
];

const STORAGE_KEY = 'demo-identity';

// "Chế độ thử": chọn vai trò thay cho đăng nhập, cho tới khi Auth Service và bộ lọc JWT ở Gateway làm xong
@Injectable({ providedIn: 'root' })
export class IdentityService {
  private readonly current = signal<DemoIdentity>(this.restore());

  readonly identity = this.current.asReadonly();
  readonly role = computed(() => this.current().role);

  select(key: string): void {
    const found = DEMO_IDENTITIES.find(i => i.key === key) ?? DEMO_IDENTITIES[0];
    this.current.set(found);
    try {
      localStorage.setItem(STORAGE_KEY, found.key);
    } catch {
      // trình duyệt chặn lưu trữ thì chỉ giữ trong phiên hiện tại
    }
  }

  private restore(): DemoIdentity {
    let key: string | null = null;
    try {
      key = localStorage.getItem(STORAGE_KEY);
    } catch {
      key = null;
    }
    return DEMO_IDENTITIES.find(i => i.key === key) ?? DEMO_IDENTITIES[0];
  }
}
