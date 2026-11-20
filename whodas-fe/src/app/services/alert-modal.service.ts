import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AlertModalData {
  message: string;
  variant: 'warning' | 'danger';
}

@Injectable({providedIn: 'root'})
export class AlertModalService {
  private alertSubject = new BehaviorSubject<AlertModalData | null>(null);

  constructor() {}

  showWarning(message: string): void {
    this.alertSubject.next({ message, variant: 'warning' });
  }

  showDanger(message: string): void {
    this.alertSubject.next({ message, variant: 'danger' });
  }

  close(): void {
    this.alertSubject.next(null);
  }

  get alert$(): Observable<AlertModalData | null> {
    return this.alertSubject.asObservable();
  }
}
