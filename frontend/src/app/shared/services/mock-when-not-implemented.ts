import { HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, of, throwError } from 'rxjs';

// Chỉ dùng dữ liệu giả khi API chưa làm (501); lỗi khác (401, 403, 503…) để trang báo lỗi
export function mockWhenNotImplemented<T>(mock: () => T) {
  return (source: Observable<T>): Observable<T> => source.pipe(
    catchError((err: unknown) =>
      err instanceof HttpErrorResponse && err.status === 501 ? of(mock()) : throwError(() => err))
  );
}
