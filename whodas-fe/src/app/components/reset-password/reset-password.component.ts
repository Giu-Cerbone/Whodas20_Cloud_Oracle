import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  username = '';
  loading = false;
  showConfirm = false;
  showResult = false;
  tempPassword = '';
  resultSuccess = false;
  resultMessage = '';

  constructor(private route: ActivatedRoute, private router: Router, private userService: UserService) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.resultSuccess = false;
      this.resultMessage = 'Token di reset non valido.';
      this.showResult = true;
    } else {
      this.userService.resolveResetUsername(this.token).subscribe({
        next: (u) => { this.username = u || ''; },
        error: () => { this.username = ''; }
      });
      this.loading = true;
      this.userService.generateTemporaryPassword(this.token).subscribe({
        next: (res) => {
          this.loading = false;
          if (res.success && res.temporaryPassword) {
            this.tempPassword = res.temporaryPassword;
          } else {
            this.resultSuccess = false;
            this.resultMessage = res.message || 'Errore durante la generazione della password temporanea.';
            this.showResult = true;
          }
        },
        error: (err) => {
          this.loading = false;
          this.resultSuccess = false;
          this.resultMessage = err.error?.message || 'Errore durante la generazione della password temporanea.';
          this.showResult = true;
        }
      });
    }
  }

  onConfirm(): void {
    this.router.navigate(['/login'], { queryParams: { temp: 'true' } });
  }

  cancelConfirm(): void {
    this.showConfirm = false;
  }

  closeResult(): void {
    this.showResult = false;
    if (this.resultSuccess) {
      this.router.navigate(['/login'], { queryParams: { temp: 'true' } });
    }
  }

  copyPassword(): void {
    navigator.clipboard.writeText(this.tempPassword).then(() => {
      this.resultMessage = 'Password temporanea copiata negli appunti.';
    });
  }
}