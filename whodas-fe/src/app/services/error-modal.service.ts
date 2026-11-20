import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  code?: string;
  path?: string;
}

@Injectable({providedIn: 'root'})
export class ErrorModalService {
  private errorSubject = new BehaviorSubject<ErrorResponse | null>(null);

  constructor() {}

  showError(message: string, code?: string, status?: number): void {
    this.errorSubject.next({ message, code, status, error: 'Error', timestamp: new Date().toISOString() });
  }

  showErrorResponse(response: ErrorResponse): void {
    this.errorSubject.next(response);
  }

  clearError(): void {
    this.errorSubject.next(null);
  }

  getError$(): Observable<ErrorResponse | null> {
    return this.errorSubject.asObservable();
  }
}