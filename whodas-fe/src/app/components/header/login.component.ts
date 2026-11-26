import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { GuidelineModalComponent } from '../guideline-modal/guideline-modal.component';
import { ForgotPasswordModalComponent } from '../forgot-password-modal/forgot-password-modal.component';
import { UserProfileModalComponent } from '../user-profile-modal/user-profile-modal.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule, GuidelineModalComponent, ForgotPasswordModalComponent, UserProfileModalComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class HeaderComponent implements OnInit {
  username = '';
  password = '';
  error = '';
  loading = false;
  registered = false;
  showGuideline = false;
  showForgotPassword = false;
  showErrorModal = false;
  errorMessage = '';
  showUserProfile = false;
  changePasswordLoading = false;
  newPassword = '';
  confirmNewPassword = '';
  changePasswordError = '';

  constructor(public authService: AuthService, public userService: UserService, private router: Router, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.registered = this.route.snapshot.queryParams['registered'] === 'true';
  }

  goRegister(): void {
    this.router.navigate(['/register']);
  }

  openGuideline(): void {
    this.showGuideline = true;
  }

  onGuidelineClose(): void {
    this.showGuideline = false;
  }

  openForgotPassword(): void {
    this.showForgotPassword = true;
  }

  onForgotPasswordClose(): void {
    this.showForgotPassword = false;
  }

  onLogin(): void {
    this.error = '';
    this.loading = true;

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        const username = this.username;
        this.userService.getProfile(username).subscribe({
          next: (p) => {
            if (p.passwordTemporary) {
              this.showUserProfile = true;
            } else {
              this.router.navigate(['/dashboard'], { replaceUrl: true });
            }
          },
          error: () => this.router.navigate(['/dashboard'], { replaceUrl: true })
        });
      },
      error: (err) => {
        this.loading = false;
        const isCredentialsError =
          err.status === 401 ||
          err.error?.error === 'invalid_grant' ||
          (typeof err.error?.error_description === 'string' &&
            err.error.error_description.toLowerCase().includes('invalid user credentials'));
        if (isCredentialsError) {
          this.error = '';
          this.errorMessage = 'Username o password non corretti. Verifica le credenziali e riprova.';
          this.showErrorModal = true;
        } else {
          this.error = err.error?.message || 'Errore durante l\'accesso';
        }
      }
    });
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
  }
}