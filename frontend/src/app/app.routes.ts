import { Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { APP_PAGES } from './core/app-pages';

const skeletonRoutes: Routes = APP_PAGES
  .filter(p => !p.implemented)
  .map(p => ({
    path: p.path,
    loadComponent: () => import('./features/skeleton/skeleton-page.component').then(m => m.SkeletonPageComponent),
    data: { page: p }
  }));

// Trang MPI cũ ngoài phạm vi đề tài: giữ route để xem lại, không đưa vào menu (docs/05)
const legacyMpiRoutes: Routes = [
  {
    path: 'patient-import',
    loadComponent: () => import('./features/patient-import/pages/patient-import-page.component')
      .then(m => m.PatientImportPageComponent)
  },
  {
    path: 'patient-masters',
    loadComponent: () => import('./features/patient-master/pages/patient-master-page.component')
      .then(m => m.PatientMasterPageComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/pages/dashboard-page.component')
      .then(m => m.DashboardPageComponent)
  },
  {
    path: 'facilities',
    loadComponent: () => import('./features/facility/pages/facilities-page.component')
      .then(m => m.FacilitiesPageComponent)
  },
  {
    path: 'source-systems',
    loadComponent: () => import('./features/source-system/pages/source-systems-page.component')
      .then(m => m.SourceSystemsPageComponent)
  }
];

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home-page.component').then(m => m.HomePageComponent)
      },
      {
        path: 'patients',
        loadComponent: () => import('./features/patient/pages/patients-page.component')
          .then(m => m.PatientsPageComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/account/pages/register-page.component')
          .then(m => m.RegisterPageComponent)
      },
      {
        path: 'admin/accounts',
        loadComponent: () => import('./features/account/pages/accounts-page.component')
          .then(m => m.AccountsPageComponent)
      },
      {
        path: 'ui-kit',
        loadComponent: () => import('./features/ui-kit/ui-kit-page.component').then(m => m.UiKitPageComponent)
      },
      ...skeletonRoutes,
      ...legacyMpiRoutes,
      { path: '**', redirectTo: '' }
    ]
  }
];
