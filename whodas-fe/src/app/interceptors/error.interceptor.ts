import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ErrorModalService } from '../services/error-modal.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private errorModalService: ErrorModalService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError(error => {
        if (error.status === 401) {
          // 401 è atteso quando non autenticati, non mostrare modal
          return throwError(() => error);
        }
        if (req.url.includes('/registration')) {
          // Gli errori della registrazione sono gestiti esplicitamente dal componente
          return throwError(() => error);
        }
        if (error.status === 404) {
          this.errorModalService.showError('Risorsa non trovata', 'NOT_FOUND', 404);
        } else if (error.status === 400) {
          this.errorModalService.showError(error.error?.message || 'Richiesta non valida', 'VALIDATION_ERROR', 400);
        } else if (error.status === 403) {
          this.errorModalService.showError('Non autorizzato', 'UNAUTHORIZED', 403);
        } else if (error.status === 500) {
          this.errorModalService.showError('Errore interno del server', 'INTERNAL_ERROR', 500);
        } else if (error.status === 0) {
          this.errorModalService.showError('Impossibile raggiungere il server', 'CONNECTION_ERROR', 0);
        } else {
          this.errorModalService.showError(error.error?.message || 'Errore sconosciuto', 'UNKNOWN_ERROR', error.status);
        }
        return throwError(() => error);
      })
    );
  }
}