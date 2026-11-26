import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { Notification, NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './notification-panel.component.html',
  styleUrls: ['./notification-panel.component.css']
})
export class NotificationPanelComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  isOpen = false;
  private notificationsSub!: Subscription;
  private unreadSub!: Subscription;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notificationsSub = this.notificationService.notifications$.subscribe(notifications => {
      this.notifications = notifications.filter(n => n.dataElimina === null && n.dataLettura === null);
    });
    this.unreadSub = this.notificationService.unreadCount$.subscribe(count => {
      this.unreadCount = count;
    });
    this.refresh();
  }

  ngOnDestroy(): void {
    this.notificationsSub?.unsubscribe();
    this.unreadSub?.unsubscribe();
  }

  togglePanel(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.refresh();
    }
  }

  private refresh(): void {
    this.notificationService.loadUnread().subscribe();
  }

  closePanel(): void {
    this.isOpen = false;
  }

  openNotification(notification: Notification): void {
    if (notification.dataLettura === null) {
      this.notificationService.markAsRead(notification.id!).subscribe();
    }
  }

  deleteNotification(notification: Notification): void {
    this.notificationService.deleteNotification(notification.id!).subscribe();
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe();
  }

  trackByIdx(_index: number, notification: Notification): number | undefined {
    return notification.id;
  }
}
