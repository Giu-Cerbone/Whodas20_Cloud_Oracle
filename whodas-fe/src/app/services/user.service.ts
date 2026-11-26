import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface User {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  fiscalCode: string;
  status: string;
  registeredAt: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ForgotPasswordResponse {
  success: boolean;
  message: string;
}

export interface ResetPasswordResponse {
  success: boolean;
  message: string;
}

export interface TemporaryPasswordResponse {
  success: boolean;
  message: string;
  temporaryPassword?: string;
}

export interface UserProfile {
  username: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  fiscalCode: string;
  address: string | null;
  postalCode: string | null;
  comune: string | null;
  city: string | null;
  phone: string | null;
  email: string;
  status: string;
  registeredAt: string;
  passwordTemporary: boolean;
}

export interface ChangePasswordRequest {
  username: string;
  currentPassword: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/registration`;

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.baseUrl);
  }

  syncLdap(): Observable<string> {
    return this.http.post(`${this.baseUrl}/sync`, {}, { responseType: 'text' });
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ForgotPasswordResponse> {
    return this.http.post<ForgotPasswordResponse>(`${this.baseUrl}/forgot-password`, request);
  }

  sendResetEmail(request: { usernameOrEmail: string }): Observable<ForgotPasswordResponse> {
    return this.http.post<ForgotPasswordResponse>(`${this.baseUrl}/send-reset-email`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<ResetPasswordResponse> {
    return this.http.post<ResetPasswordResponse>(`${this.baseUrl}/reset-password`, request);
  }

  getProfile(username: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/me`, { params: { username } });
  }

  changePassword(request: ChangePasswordRequest): Observable<ResetPasswordResponse> {
    return this.http.post<ResetPasswordResponse>(`${this.baseUrl}/change-password`, request);
  }

  resolveResetUsername(token: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/reset-username`, { params: { token }, responseType: 'text' });
  }

  generateTemporaryPassword(token: string): Observable<TemporaryPasswordResponse> {
    return this.http.post<TemporaryPasswordResponse>(`${this.baseUrl}/generate-temporary-password`, null, { params: { token } });
  }
}