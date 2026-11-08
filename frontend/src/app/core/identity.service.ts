import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../../environments/environment';

export type Role = 'GUEST' | 'PATIENT' | 'RECEPTIONIST' | 'DOCTOR' | 'ADMIN';

export interface CurrentUser {
  role: Role;
  accountId: number | null;
  doctorId: number | null;
  // Tên hiển thị trên thanh đầu trang
  label: string;
}

const GUEST: CurrentUser = { role: 'GUEST', accountId: null, doctorId: null, label: 'Khách' };
const ROLES: Role[] = ['PATIENT', 'RECEPTIONIST', 'DOCTOR', 'ADMIN'];

// Luồng Authorization Code + PKCE với client "web" của Keycloak (docs/03-hop-dong-api.md, mục A5)
const AUTH_CONFIG: AuthConfig = {
  issuer: environment.keycloakRealmUrl,
  clientId: 'web',
  responseType: 'code',
  scope: 'openid profile',
  redirectUri: window.location.origin + '/',
  postLogoutRedirectUri: window.location.origin + '/',
  requireHttps: false,
  showDebugInformation: false
};

@Injectable({ providedIn: 'root' })
export class IdentityService {
  private readonly oauth = inject(OAuthService);
  private readonly current = signal<CurrentUser>(GUEST);

  readonly identity = this.current.asReadonly();
  readonly role = computed(() => this.current().role);

  // Chạy trước khi Angular dựng trang: nếu URL đang mang mã đăng nhập từ Keycloak thì đổi lấy token
  async init(): Promise<void> {
    this.oauth.configure(AUTH_CONFIG);
    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
    } catch {
      // Keycloak chưa bật: vẫn mở web ở chế độ khách
    }
    this.refresh();
    this.restoreReturnUrl();
  }

  login(returnUrl: string = window.location.pathname + window.location.search): void {
    this.oauth.initCodeFlow(returnUrl);
  }

  logout(): void {
    this.oauth.logOut();
  }

  // Token chỉ trả về khi còn hạn
  accessToken(): string | null {
    return this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null;
  }

  // Có token đã lưu nhưng hết hạn, hoặc API trả 401: xóa phiên cũ rồi sang trang đăng nhập Keycloak
  sessionExpired(): void {
    const returnUrl = window.location.pathname + window.location.search;
    this.oauth.logOut(true);
    this.current.set(GUEST);
    this.login(returnUrl);
  }

  hasStoredToken(): boolean {
    return !!this.oauth.getAccessToken();
  }

  private refresh(): void {
    const token = this.accessToken();
    this.current.set(token ? this.fromClaims(this.decode(token)) : GUEST);
  }

  private fromClaims(claims: Record<string, unknown>): CurrentUser {
    const role = ROLES.find(r => r === claims['role']);
    const accountId = Number(claims['accountId']);
    if (!role || !Number.isFinite(accountId)) {
      return GUEST;
    }
    const doctorId = claims['doctorId'] == null ? null : Number(claims['doctorId']);
    const name = String(claims['name'] ?? claims['preferred_username'] ?? `Tài khoản ${accountId}`);
    return { role, accountId, doctorId: Number.isFinite(doctorId) ? doctorId : null, label: name };
  }

  private decode(token: string): Record<string, unknown> {
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const bytes = Uint8Array.from(atob(payload), c => c.charCodeAt(0));
      return JSON.parse(new TextDecoder().decode(bytes));
    } catch {
      return {};
    }
  }

  // Sau khi đăng nhập xong, đưa người dùng về trang họ đang mở trước khi bị chuyển sang Keycloak
  private restoreReturnUrl(): void {
    const state = this.oauth.state;
    if (!state || !this.oauth.hasValidAccessToken()) {
      return;
    }
    let target = state;
    try {
      target = decodeURIComponent(state);
    } catch {
      // giữ nguyên
    }
    if (target.startsWith('/') && !target.startsWith('//')) {
      history.replaceState(null, '', target);
    }
  }
}
