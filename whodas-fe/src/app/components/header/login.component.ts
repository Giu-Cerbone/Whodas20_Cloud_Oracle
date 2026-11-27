import { Component, OnInit, OnDestroy } from '@angular/core';
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
export class HeaderComponent implements OnInit, OnDestroy {
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
  now: Date = new Date();
  hourDelay = '0s';
  minuteDelay = '0s';
  secondDelay = '0s';
  private clockTimer: number | undefined;

  constructor(public authService: AuthService, public userService: UserService, private router: Router, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.registered = this.route.snapshot.queryParams['registered'] === 'true';
    this.syncClock();
    this.startClockTimer();
  }

  ngOnDestroy(): void {
    this.stopClockTimer();
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

  private syncClock(): void {
    const d = new Date();
    this.now = d;
    const h = d.getHours() * 3600 + d.getMinutes() * 60 + d.getSeconds();
    const m = d.getMinutes() * 60 + d.getSeconds();
    this.hourDelay = `-${h % 43200}s`;
    this.minuteDelay = `-${m % 3600}s`;
    this.secondDelay = `-${d.getSeconds()}s`;
  }

  private startClockTimer(): void {
    const tick = (): void => {
      this.now = new Date();
      this.clockTimer = window.setTimeout(tick, 1000 - (Date.now() % 1000));
    };
    tick();
  }

  private stopClockTimer(): void {
    if (this.clockTimer !== undefined) {
      window.clearTimeout(this.clockTimer);
      this.clockTimer = undefined;
    }
  }

  clockHint(): string {
    const d = this.now;
    const s = d.toLocaleString('it-IT', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
    const months = ['gennaio','febbraio','marzo','aprile','maggio','giugno','luglio','agosto','settembre','ottobre','novembre','dicembre'];
    let out = s.charAt(0).toUpperCase() + s.slice(1);
    months.forEach(m => { out = out.replace(m, m.charAt(0).toUpperCase() + m.slice(1)); });
    return out;
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
