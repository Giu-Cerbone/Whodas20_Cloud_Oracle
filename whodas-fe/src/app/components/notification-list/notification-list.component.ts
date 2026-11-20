import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { Notification, NotificationFilter, NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './notification-list.component.html',
  styleUrls: ['./notification-list.component.css']
})
export class NotificationListComponent implements OnInit {
  activeFilter: NotificationFilter = 'unread';
  selectedNotification: Notification | null = null;

  filters: { key: NotificationFilter; label: string; icon: string }[] = [
    { key: 'unread', label: 'Non lette', icon: 'fa-envelope' },
    { key: 'read', label: 'Lette', icon: 'fa-envelope-open' },
    { key: 'deleted', label: 'Cancellate', icon: 'fa-trash' },
    { key: 'all', label: 'Tutte', icon: 'fa-layer-group' }
  ];

  constructor(
    private notificationService: NotificationService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.notificationService.loadNotifications().subscribe();
    this.route.queryParamMap.subscribe(params => {
      const filter = params.get('filter') as NotificationFilter | null;
      if (filter && this.filters.some(f => f.key === filter)) {
        this.activeFilter = filter;
        this.selectedNotification = null;
      }
    });
  }

  selectFilter(filter: NotificationFilter): void {
    this.activeFilter = filter;
    this.selectedNotification = null;
  }

  get filteredNotifications(): Notification[] {
    const notifications = this.notificationService.getNotifications();
    return this.notificationService.filterNotifications(notifications, this.activeFilter);
  }

  openNotification(notification: Notification): void {
    this.selectedNotification = notification;
    if (notification.dataLettura === null && notification.dataElimina === null) {
      this.notificationService.markAsRead(notification.id!).subscribe();
    }
  }

  closeDetail(): void {
    this.selectedNotification = null;
  }

  deleteNotification(notification: Notification): void {
    this.notificationService.deleteNotification(notification.id!).subscribe(() => {
      if (this.selectedNotification?.id === notification.id) {
        this.selectedNotification = null;
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe();
  }

  trackByIdx(index: number, _: Notification): number {
    return index;
  }
}
