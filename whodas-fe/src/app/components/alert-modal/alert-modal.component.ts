import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';

import { AlertModalData, AlertModalService } from '../../services/alert-modal.service';

@Component({
  selector: 'app-alert-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert-modal.component.html',
  styleUrls: ['./alert-modal.component.css']
})
export class AlertModalComponent implements OnInit, OnDestroy {
  alert: AlertModalData | null = null;
  private subscription!: Subscription;

  constructor(private alertModalService: AlertModalService) {}

  ngOnInit(): void {
    this.subscription = this.alertModalService.alert$.subscribe(alert => {
      this.alert = alert;
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  onClose(): void {
    this.alertModalService.close();
  }

  isDanger(): boolean {
    return this.alert?.variant === 'danger';
  }

  getTitle(): string {
    return this.isDanger() ? 'Errore' : 'Avviso';
  }
}
