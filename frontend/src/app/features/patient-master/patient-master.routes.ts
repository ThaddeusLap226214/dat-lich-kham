import { Routes } from '@angular/router';

export const PATIENT_MASTER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/patient-master-page.component').then(m => m.PatientMasterPageComponent)
  }
];
