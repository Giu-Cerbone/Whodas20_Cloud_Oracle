import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface CheckResponse {
  exists: boolean;
  message: string | null;
}

export interface RegistrationRequest {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  fiscalCode: string;
  address: string;
  postalCode: string;
  comune: string;
  city: string;
  phone: string;
  email: string;
}

export interface RegistrationResponse {
  success: boolean;
  message: string;
  username?: string;
}

@Injectable({providedIn: 'root'})
export class RegistrationService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/registration`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  checkFiscalCode(cf: string): Observable<CheckResponse> {
    return this.http.get<CheckResponse>(`${this.baseUrl}/check/cf`, { params: { cf } });
  }

  checkEmail(email: string): Observable<CheckResponse> {
    return this.http.get<CheckResponse>(`${this.baseUrl}/check/email`, { params: { email } });
  }

  register(req: RegistrationRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(this.baseUrl, req).pipe(
      tap(response => {
        if (response.success && response.username) {
          // Auto-login dopo la nuova registrazione
          this.authService.login(response.username, req.password).subscribe();
        }
      })
    );
  }
}
