import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { IdentityService } from './identity.service';

// Gắn header danh tính giống Gateway sẽ gắn sau khi xác minh JWT (docs/03-hop-dong-api.md, mục A5).
// Khi đăng nhập thật làm xong: thay bằng header Authorization: Bearer <token> và bỏ chế độ thử.
export const identityInterceptor: HttpInterceptorFn = (req, next) => {
  const identity = inject(IdentityService).identity();
  if (identity.accountId === null) {
    return next(req);
  }
  const headers: Record<string, string> = {
    'X-User-Id': String(identity.accountId),
    'X-User-Role': identity.role
  };
  if (identity.doctorId !== null) {
    headers['X-Doctor-Id'] = String(identity.doctorId);
  }
  return next(req.clone({ setHeaders: headers }));
};
