import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface Notification {
  id?: number;
  tipoObj: string;
  typeMessage: string;
  oggetto: string;
  description: string;
  dataSpedizione: string;
  dataLettura: string | null;
  dataElimina: string | null;
  stato: string;
  userId: string | null;
  username: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export type NotificationFilter = 'all' | 'unread' | 'read' | 'deleted';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private baseUrl = `${environment.baseUrl}/${environment.apiVersion}/notifications`;
  private abortController: AbortController | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);

  notifications$ = this.notificationsSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService, private ngZone: NgZone) {}

  // SSE via fetch: il token viaggia nell'header Authorization e non nell'URL
  connect(username: string): void {
    this.disconnect();

    const credential = this.authService.getBasicCredential();
    if (!credential) {
      return;
    }

    const controller = new AbortController();
    this.abortController = controller;

    fetch(`${this.baseUrl}/stream`, {
      headers: { Authorization: `Basic ${credential}`, Accept: 'text/event-stream' },
      signal: controller.signal
    })
      .then(async response => {
        if (!response.ok || !response.body) {
          throw new Error(`SSE status ${response.status}`);
        }
        const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
        let buffer = '';
        while (true) {
          const { value, done } = await reader.read();
          if (done) {
            break;
          }
          buffer += value.replace(/\r\n/g, '\n');
          let idx: number;
          while ((idx = buffer.indexOf('\n\n')) >= 0) {
            this.handleEvent(buffer.substring(0, idx));
            buffer = buffer.substring(idx + 2);
          }
        }
        throw new Error('SSE stream closed');
      })
      .catch(err => {
        if (controller.signal.aborted) {
          return;
        }
        console.warn('SSE connection error, will retry:', err);
        this.reconnectTimer = setTimeout(() => this.connect(username), 5000);
      });
  }

  private handleEvent(raw: string): void {
    let eventName = 'message';
    const data: string[] = [];
    raw.split('\n').forEach(line => {
      if (line.startsWith('event:')) {
        eventName = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        data.push(line.substring(5).replace(/^ /, ''));
      }
    });
    if (eventName !== 'notification' || !data.length) {
      return;
    }
    this.ngZone.run(() => {
      try {
        this.addNotification(JSON.parse(data.join('\n')) as Notification);
      } catch (e) {
        console.warn('Notifica SSE non valida', e);
      }
    });
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
  }

  private addNotification(notification: Notification): void {
    const normalized = this.normalize(notification);
    const current = this.notificationsSubject.value;
    const existingIndex = normalized.id != null
      ? current.findIndex(n => n.id === normalized.id)
      : -1;

    const updated = existingIndex >= 0
      ? current.map((n, i) => (i === existingIndex ? normalized : n))
      : [normalized, ...current];

    this.notificationsSubject.next(updated);
    this.updateUnreadCount(updated);
  }

  private normalize(notification: Notification): Notification {
    return {
      ...notification,
      dataLettura: notification.dataLettura ?? null,
      dataElimina: notification.dataElimina ?? null
    };
  }

  loadNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.baseUrl).pipe(
      tap(notifications => {
        const normalized = notifications.map(n => this.normalize(n));
        this.notificationsSubject.next(normalized);
        this.updateUnreadCount(normalized);
      })
    );
  }

  getNotifications(): Notification[] {
    return this.notificationsSubject.value;
  }

  loadUnread(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.baseUrl}/unread`).pipe(
      tap(notifications => {
        const normalized = notifications.map(n => this.normalize(n));
        const current = this.notificationsSubject.value.filter(n => n.dataElimina === null);
        const existingIds = new Set(current.map(n => n.id));
        const merged = [...normalized];
        for (const n of current) {
          if (!existingIds.has(n.id)) {
            merged.push(n);
          }
        }
        this.notificationsSubject.next(merged);
        this.updateUnreadCount(merged);
      })
    );
  }

  countUnread(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/unread`).pipe(
      tap(count => this.unreadCountSubject.next(count))
    );
  }

  markAsRead(id: number): Observable<Notification> {
    return this.http.put<Notification>(`${this.baseUrl}/${id}/read`, null).pipe(
      tap(notification => {
        const current = this.notificationsSubject.value;
        this.notificationsSubject.next(
          current.map(n => n.id === id ? this.normalize(notification) : n)
        );
        this.updateUnreadCount(this.notificationsSubject.value);
      })
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/read-all`, null).pipe(
      tap(() => {
        const current = this.notificationsSubject.value;
        this.notificationsSubject.next(
          current.map(n => n.dataLettura ? n : { ...n, dataLettura: new Date().toISOString(), stato: 'LETTO' })
        );
        this.unreadCountSubject.next(0);
      })
    );
  }

  deleteNotification(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        const now = new Date().toISOString();
        const current = this.notificationsSubject.value;
        this.notificationsSubject.next(
          current.map(n => n.id === id ? { ...n, dataElimina: now, stato: 'CANCELLATO' } : n)
        );
        this.updateUnreadCount(this.notificationsSubject.value);
      })
    );
  }

  filterNotifications(notifications: Notification[], filter: NotificationFilter): Notification[] {
    switch (filter) {
      case 'unread':
        return notifications.filter(n => n.dataLettura === null && n.dataElimina === null);
      case 'read':
        return notifications.filter(n => n.dataLettura !== null && n.dataElimina === null);
      case 'deleted':
        return notifications.filter(n => n.dataElimina !== null);
      default:
        return notifications;
    }
  }

  private updateUnreadCount(notifications: Notification[]): void {
    const count = notifications.filter(n => n.dataLettura === null && n.dataElimina === null).length;
    this.unreadCountSubject.next(count);
  }

  getUnreadCount(): number {
    const notifications = this.notificationsSubject.value;
    return notifications.filter(n => n.dataLettura === null && n.dataElimina === null).length;
  }
}
