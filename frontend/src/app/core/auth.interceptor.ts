import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { EMPTY, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { IdentityService } from './identity.service';

// Gắn Authorization: Bearer <token> vào mọi request gửi tới Gateway. Token hết hạn hoặc Gateway trả 401 → đăng nhập lại
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }
  const identity = inject(IdentityService);
  const token = identity.accessToken();
  if (!token && identity.hasStoredToken()) {
    identity.sessionExpired();
    return EMPTY;
  }
  const outgoing = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(outgoing).pipe(
    catchError((err: unknown) => {
      if (token && err instanceof HttpErrorResponse && err.status === 401) {
        identity.sessionExpired();
      }
      return throwError(() => err);
    })
  );
};
