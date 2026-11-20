import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';

import { ErrorResponse, ErrorModalService } from '../../services/error-modal.service';

@Component({
  selector: 'app-error-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './error-modal.component.html',
  styleUrls: ['./error-modal.component.css']
})
export class ErrorModalComponent implements OnInit, OnDestroy {
  error: ErrorResponse | null = null;
  private subscription!: Subscription;

  constructor(private errorModalService: ErrorModalService) {}

  ngOnInit(): void {
    this.subscription = this.errorModalService.getError$().subscribe(error => {
      this.error = error;
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  onClose(): void {
    this.errorModalService.clearError();
  }

  getErrorTitle(): string {
    if (!this.error) return 'Errore';
    if (this.error.status === 404) return 'Risorsa non trovata';
    if (this.error.status === 400) return 'Richiesta non valida';
    if (this.error.status === 403) return 'Non autorizzato';
    if (this.error.status === 500) return 'Errore interno';
    return this.error.error || 'Errore';
  }
}
