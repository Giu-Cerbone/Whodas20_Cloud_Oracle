import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { AlertModalService } from '../../services/alert-modal.service';
import { RegistrationService } from '../../services/registration.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
})
export class RegistrationComponent implements OnDestroy {
  username = '';
  password = '';
  confirmPassword = '';
  firstName = '';
  lastName = '';
  birthDate = '';
  fiscalCode = '';
  address = '';
  postalCode = '';
  comune = '';
  city = '';
  phone = '';
  email = '';

  loading = false;
  emailBlocked = false;
  cfBlocked = false;
  fieldErrors: { [key: string]: string } = {};

  private emailSub?: Subscription;
  private cfSub?: Subscription;

  constructor(
    private registrationService: RegistrationService,
    private alertModalService: AlertModalService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnDestroy(): void {
    this.emailSub?.unsubscribe();
    this.cfSub?.unsubscribe();
  }

  goLogin(): void {
    this.router.navigate(['/login']);
  }

  onFiscalCodeInput(): void {
    this.fiscalCode = this.fiscalCode.toUpperCase();
  }

  onEmailBlur(): void {
    this.emailSub?.unsubscribe();
    const value = this.email.trim();
    if (!value) {
      this.emailBlocked = false;
      return;
    }
    this.emailSub = this.registrationService.checkEmail(value).subscribe({
      next: (res) => {
        if (res.exists) {
          this.emailBlocked = true;
          this.alertModalService.showWarning(res.message || "Email gia' associata ad una utenza");
        } else {
          this.emailBlocked = false;
        }
      },
      error: () => {
        this.emailBlocked = false;
      }
    });
  }

  onCfBlur(): void {
    this.cfSub?.unsubscribe();
    const value = this.fiscalCode.trim();
    if (!value) {
      this.cfBlocked = false;
      return;
    }
    this.cfSub = this.registrationService.checkFiscalCode(value).subscribe({
      next: (res) => {
        if (res.exists) {
          this.cfBlocked = true;
          this.alertModalService.showWarning(res.message || "Codice Fiscale gia' esistente");
        } else {
          this.cfBlocked = false;
        }
      },
      error: () => {
        this.cfBlocked = false;
      }
    });
  }

  onConfirmPasswordBlur(): void {
    if (!this.confirmPassword) {
      this.fieldErrors['confirmPassword'] = 'Campo obbligatorio';
    } else if (this.confirmPassword !== this.password) {
      this.fieldErrors['confirmPassword'] = 'Le password non coincidono';
    } else {
      delete this.fieldErrors['confirmPassword'];
    }
  }

  onFieldFocus(field: string): void {
    delete this.fieldErrors[field];
  }

  onSubmit(form: NgForm): void {
    this.fieldErrors = {};

    if (!this.username.trim()) {
      this.fieldErrors['username'] = 'Campo obbligatorio';
    } else if (this.username.trim().length < 3) {
      this.fieldErrors['username'] = 'Minimo 3 caratteri';
    }
    if (!this.password) {
      this.fieldErrors['password'] = 'Campo obbligatorio';
    } else if (this.password.length < 6) {
      this.fieldErrors['password'] = 'Minimo 6 caratteri';
    }
    if (!this.confirmPassword) {
      this.fieldErrors['confirmPassword'] = 'Campo obbligatorio';
    } else if (this.confirmPassword !== this.password) {
      this.fieldErrors['confirmPassword'] = 'Le password non coincidono';
    }
    if (!this.firstName.trim()) {
      this.fieldErrors['firstName'] = 'Campo obbligatorio';
    }
    if (!this.lastName.trim()) {
      this.fieldErrors['lastName'] = 'Campo obbligatorio';
    }
    if (!/^[A-Za-z0-9]{16}$/.test(this.fiscalCode.trim())) {
      this.fieldErrors['fiscalCode'] = 'Deve avere 16 caratteri alfanumerici';
    }
    if (!/^\d{5}$/.test(this.postalCode.trim())) {
      this.fieldErrors['postalCode'] = 'Deve avere 5 cifre';
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) {
      this.fieldErrors['email'] = 'Inserisci un indirizzo email valido';
    }
    if (!this.birthDate.trim()) {
      this.fieldErrors['birthDate'] = 'Campo obbligatorio';
    }
    if (!this.address.trim()) {
      this.fieldErrors['address'] = 'Campo obbligatorio';
    }
    if (!this.postalCode.trim()) {
      this.fieldErrors['postalCode'] = 'Campo obbligatorio';
    } else if (!/^\d{5}$/.test(this.postalCode.trim())) {
      this.fieldErrors['postalCode'] = 'Deve avere 5 cifre';
    }
    if (!this.comune.trim()) {
      this.fieldErrors['comune'] = 'Campo obbligatorio';
    }
    if (!this.city.trim()) {
      this.fieldErrors['city'] = 'Campo obbligatorio';
    }
    if (!this.phone.trim()) {
      this.fieldErrors['phone'] = 'Campo obbligatorio';
    }
    if (this.emailBlocked) {
      this.fieldErrors['email'] = "Email gia' associata ad una utenza";
    }
    if (this.cfBlocked) {
      this.fieldErrors['fiscalCode'] = "Codice Fiscale gia' esistente";
    }

    if (Object.keys(this.fieldErrors).length > 0) {
      if (this.cfBlocked) {
        this.alertModalService.showWarning("Codice Fiscale gia' esistente");
      } else if (this.emailBlocked) {
        this.alertModalService.showWarning("Email gia' associata ad una utenza");
      }
      return;
    }

    this.loading = true;
    const req = {
      username: this.username.trim(),
      password: this.password,
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      birthDate: this.birthDate || null,
      fiscalCode: this.fiscalCode.trim(),
      address: this.address.trim(),
      postalCode: this.postalCode.trim(),
      comune: this.comune.trim(),
      city: this.city.trim(),
      phone: this.phone.trim(),
      email: this.email.trim()
    };

    this.registrationService.register(req).subscribe({
      next: (res) => {
        this.loading = false;
        // Auto-login dopo la nuova registrazione
        this.authService.login(this.username.trim(), this.password).subscribe({
          next: () => {
            this.router.navigate(['/dashboard'], { replaceUrl: true });
          },
          error: () => {
            this.router.navigate(['/login'], { queryParams: { registered: 'true' }, replaceUrl: true });
          }
        });
      },
      error: (err) => {
        this.loading = false;
        const message = err?.error?.message || 'Errore durante la registrazione';
        if (err?.status === 409) {
          this.alertModalService.showWarning(message);
        } else {
          this.alertModalService.showDanger(message);
        }
      }
    });
  }
}
