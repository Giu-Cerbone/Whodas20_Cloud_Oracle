import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { User, UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  loading = true;
  syncing = false;
  error = '';
  success = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Errore durante il caricamento degli utenti';
        this.loading = false;
      }
    });
  }

  onRefresh(): void {
    this.syncing = true;
    this.error = '';
    this.success = '';
    this.userService.syncLdap().subscribe({
      next: (msg) => {
        this.success = msg;
        this.syncing = false;
        this.loadUsers();
      },
      error: (err) => {
        this.error = err.error?.message || 'Errore durante la sincronizzazione';
        this.syncing = false;
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ATTIVO': return 'badge bg-success';
      case 'SCADUTO': return 'badge bg-warning text-dark';
      case 'CANCELLATO': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }
}