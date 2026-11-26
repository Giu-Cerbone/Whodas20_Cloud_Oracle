import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, map } from 'rxjs';
import { Router } from '@angular/router';

export interface AuthUser {
  username: string;
  name: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'api/v1/auth';
  private tokenKey = 'auth_token';
  private userKey = 'auth_user';
  private usernameKey = 'auth_username';
  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);

  currentUser$: Observable<AuthUser | null> = this.currentUserSubject.asObservable();
  currentUserName$: Observable<string> = this.currentUser$.pipe(
    map(user => user?.firstName || '')
  );

  constructor(private http: HttpClient, private router: Router) {
    this.loadUserFromStorage();
  }

  private loadUserFromStorage(): void {
    const stored = localStorage.getItem(this.userKey);
    if (stored) {
      try {
        this.currentUserSubject.next(JSON.parse(stored) as AuthUser);
        return;
      } catch (e) {
        localStorage.removeItem(this.userKey);
      }
    }
    const token = localStorage.getItem(this.tokenKey);
    if (token && !this.isTokenExpired(token)) {
      const user = this.userFromToken(token);
      if (user) {
        localStorage.setItem(this.userKey, JSON.stringify(user));
        this.currentUserSubject.next(user);
      }
    } else if (token) {
      this.clearAuthData();
    }
  }

  private isTokenExpired(token: string): boolean {
    const claims = this.decodeJwtPayload(token);
    if (!claims || !claims['exp']) {
      return true;
    }
    const exp = claims['exp'] * 1000;
    return Date.now() >= exp;
  }

  private clearAuthData(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    localStorage.removeItem(this.usernameKey);
  }

  login(username: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/login`, { username, password }).pipe(
      tap(response => {
        const token = response?.token ?? response?.access_token;
        if (token) {
          localStorage.setItem(this.tokenKey, token);
          localStorage.setItem(this.usernameKey, username);
          const user = this.userFromToken(token)
            ?? this.userFromResponse(response.user, username);
          if (user) {
            localStorage.setItem(this.userKey, JSON.stringify(user));
            this.currentUserSubject.next(user);
          }
        }
      })
    );
  }

  /**
   * Credenziale HTTP Basic (base64 di "username:token") da mettere nell'header
   * Authorization di ogni richiesta successiva al login: il token e' quello
   * restituito dalla risposta di login.
   */
  getBasicCredential(): string | null {
    const token = this.getToken();
    if (!token || this.isTokenExpired(token)) {
      return null;
    }
    const username = localStorage.getItem(this.usernameKey)
      ?? this.currentUserSubject.value?.username
      ?? '';
    return this.encodeBase64(`${username}:${token}`);
  }

  private encodeBase64(value: string): string {
    const bytes = new TextEncoder().encode(value);
    let binary = '';
    bytes.forEach(byte => binary += String.fromCharCode(byte));
    return btoa(binary);
  }

  invalidateSession(): void {
    this.clearAuthData();
    sessionStorage.clear();
    this.currentUserSubject.next(null);
  }

  logout(): void {
    this.invalidateSession();
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired(token);
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  private userFromResponse(user: any, fallbackUsername: string): AuthUser | null {
    if (!user) {
      return null;
    }
    const firstName = user.firstName ?? user.given_name ?? null;
    const lastName = user.lastName ?? user.family_name ?? null;
    const username = user.username ?? user.preferred_username ?? fallbackUsername;
    // Nome visualizzato: esclusivamente firstName
    const displayName = firstName ?? '';
    return {
      username,
      name: displayName,
      firstName,
      lastName,
      email: user.email ?? null,
      roles: user.roles ?? []
    };
  }

  private userFromToken(token: string): AuthUser | null {
    const claims = this.decodeJwtPayload(token);
    if (!claims) {
      return null;
    }
    const firstName = claims['given_name'] ?? null;
    const lastName = claims['family_name'] ?? null;
    const username = claims['preferred_username'] ?? claims['username'] ?? claims['sub'] ?? null;
    if (!username) {
      return null;
    }
    // Nome visualizzato: esclusivamente given_name
    const displayName = claims['given_name'] ?? '';
    return {
      username,
      name: displayName,
      firstName,
      lastName,
      email: claims['email'] ?? null,
      roles: claims['realm_access']?.roles ?? []
    };
  }

  private decodeJwtPayload(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
      const binary = atob(padded);
      const json = binary
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('');
      return JSON.parse(decodeURIComponent(json));
    } catch (e) {
      return null;
    }
  }
}