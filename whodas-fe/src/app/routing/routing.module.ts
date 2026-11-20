import { Routes } from '@angular/router';
import { AuthGuard, LoginGuard } from '../guards/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('../components/header/login.component').then(c => c.HeaderComponent), canActivate: [LoginGuard] },
  { path: 'register', loadComponent: () => import('../components/registration/registration.component').then(c => c.RegistrationComponent), canActivate: [LoginGuard] },
  { path: 'forgot-password', loadComponent: () => import('../components/forgot-password/forgot-password.component').then(c => c.ForgotPasswordComponent), canActivate: [LoginGuard] },
  { path: 'reset-password', loadComponent: () => import('../components/reset-password/reset-password.component').then(c => c.ResetPasswordComponent), canActivate: [LoginGuard] },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'dashboard', loadComponent: () => import('../components/dashboard/dashboard.component').then(c => c.DashboardComponent),canActivate: [AuthGuard] },
  { path: 'patients', loadComponent: () => import('../components/patient-list/patient-list.component').then(c => c.PatientListComponent),canActivate: [AuthGuard] },
  { path: 'patients/new', loadComponent: () => import('../components/patient-form/patient-form.component').then(c => c.PatientFormComponent),canActivate: [AuthGuard] },
  { path: 'patients/edit/:id', loadComponent: () => import('../components/patient-form/patient-form.component').then(c => c.PatientFormComponent),canActivate: [AuthGuard] },
  { path: 'questionnaire', loadComponent: () => import('../components/questionnaire-assessment/questionnaire-assessment.component').then(c => c.QuestionnaireAssessmentComponent),canActivate: [AuthGuard] },
  { path: 'questionnaire/:patientId', loadComponent: () => import('../components/questionnaire-assessment/questionnaire-assessment.component').then(c => c.QuestionnaireAssessmentComponent),canActivate: [AuthGuard] },
  { path: 'assessment/run/:patientId', loadComponent: () => import('../components/questionnaire-runner/questionnaire-runner.component').then(c => c.QuestionnaireRunnerComponent),canActivate: [AuthGuard] },
  { path: 'documents', loadComponent: () => import('../components/document-upload/document-upload.component').then(c => c.DocumentUploadComponent),canActivate: [AuthGuard] },
  { path: 'documents/download', loadComponent: () => import('../components/document-download/document-download.component').then(c => c.DocumentDownloadComponent),canActivate: [AuthGuard] },
  { path: 'documents/history', loadComponent: () => import('../components/document-history/document-history.component').then(c => c.DocumentHistoryComponent),canActivate: [AuthGuard] },
  { path: 'certificates', loadComponent: () => import('../components/certificate-management/certificate-management.component').then(c => c.CertificateManagementComponent), canActivate: [AuthGuard] },
  { path: 'notifications', loadComponent: () => import('../components/notification-list/notification-list.component').then(c => c.NotificationListComponent),canActivate: [AuthGuard] },
  { path: 'users', loadComponent: () => import('../components/user-management/user-management.component').then(c => c.UserManagementComponent), canActivate: [AuthGuard] },
  { path: '**', redirectTo: 'dashboard' }
];