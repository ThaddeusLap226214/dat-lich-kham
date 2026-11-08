import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AppPage } from './app-pages';
import { IdentityService } from './identity.service';

// Trang cần đăng nhập mà chưa đăng nhập → sang Keycloak; đã đăng nhập nhưng sai vai trò → về trang chủ
export const roleGuard: CanActivateFn = (route, state) => {
  const identity = inject(IdentityService);
  const router = inject(Router);
  const page = route.data['page'] as AppPage | undefined;
  if (!page || page.roles.includes('GUEST')) {
    return true;
  }
  if (identity.role() === 'GUEST') {
    identity.login(state.url);
    return false;
  }
  return page.roles.includes(identity.role()) ? true : router.createUrlTree(['/']);
};
