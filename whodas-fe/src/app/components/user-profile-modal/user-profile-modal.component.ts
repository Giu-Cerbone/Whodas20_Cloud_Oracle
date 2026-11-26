import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../services/auth.service';
import { UserProfile, UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-profile-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-profile-modal.component.html',
  styleUrls: ['./user-profile-modal.component.css']
})
export class UserProfileModalComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() user: AuthUser | null = null;
  @Output() closed = new EventEmitter<void>();

  profile: UserProfile | null = null;
  loadingProfile = false;

  changeForm!: FormGroup;
  changing = false;
  changeError = '';
  changeSuccess = '';
  changeExpanded = false;
  showConfirm = false;
  showResultModal = false;
  resultSuccess = false;
  resultMessage = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.changeForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, this.passwordStrengthValidator]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.loadProfile();
      this.resetChangeForm();
    }
  }

  private loadProfile(): void {
    const username = this.user?.username;
    this.profile = null;
    if (!username) {
      this.loadingProfile = false;
      return;
    }
    this.loadingProfile = true;
    this.userService.getProfile(username).subscribe({
      next: (p) => {
        this.profile = p;
        this.loadingProfile = false;
      },
      error: () => {
        this.profile = null;
        this.loadingProfile = false;
      }
    });
  }

  private resetChangeForm(): void {
    this.changeForm.reset();
    this.changeError = '';
    this.changeSuccess = '';
    this.changing = false;
    this.changeExpanded = false;
    this.showConfirm = false;
    this.showResultModal = false;
  }

  toggleChange(): void {
    this.changeExpanded = !this.changeExpanded;
  }

  cancelConfirm(): void {
    this.showConfirm = false;
  }

  closeResultModal(): void {
    this.showResultModal = false;
  }

  passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value || '';
    if (!value) {
      return null;
    }
    const hasNumber = /\d/.test(value);
    const hasLower = /[a-z]/.test(value);
    const hasUpper = /[A-Z]/.test(value);
    const hasSpecial = /[^A-Za-z0-9]/.test(value);
    const longEnough = value.length >= 10;
    if (longEnough && hasNumber && hasLower && hasUpper && hasSpecial) {
      return null;
    }
    return { passwordWeak: true };
  }

  passwordMatchValidator(form: FormGroup): void {
    const newPass = form.get('newPassword');
    const confirmPass = form.get('confirmPassword');
    if (newPass && confirmPass && newPass.value !== confirmPass.value) {
      confirmPass.setErrors({ ...(confirmPass.errors || {}), mismatch: true });
    } else if (confirmPass?.errors?.['mismatch']) {
      delete confirmPass.errors['mismatch'];
      if (Object.keys(confirmPass.errors).length === 0) {
        confirmPass.setErrors(null);
      }
    }
  }

  newPasswordDifferentValidator(form: FormGroup): void {
    const current = form.get('currentPassword');
    const newPass = form.get('newPassword');
    if (current && newPass && current.value && newPass.value && current.value === newPass.value) {
      newPass.setErrors({ ...(newPass.errors || {}), samePassword: true });
    } else if (newPass?.errors?.['samePassword']) {
      delete newPass.errors['samePassword'];
      if (Object.keys(newPass.errors).length === 0) {
        newPass.setErrors(null);
      }
    }
  }

  get f() {
    return this.changeForm.controls;
  }

  onSubmit(): void {
    this.changeError = '';
    this.changeSuccess = '';
    if (this.changeForm.invalid) {
      this.changeForm.markAllAsTouched();
      return;
    }
    const username = this.user?.username;
    if (!username) {
      return;
    }
    this.showConfirm = true;
  }

  confirmChange(): void {
    this.showConfirm = false;
    const username = this.user?.username;
    if (!username) {
      return;
    }
    this.changing = true;
    this.userService.changePassword({
      username,
      currentPassword: this.f['currentPassword'].value,
      newPassword: this.f['newPassword'].value
    }).subscribe({
      next: (res) => {
        this.changing = false;
        this.resultSuccess = res.success;
        this.resultMessage = res.message;
        if (res.success) {
          this.changeExpanded = false;
          this.changeForm.reset();
          this.close();
          this.router.navigate(['/login'], { replaceUrl: true });
        }
        this.showResultModal = true;
      },
      error: (err) => {
        this.changing = false;
        this.resultSuccess = false;
        this.resultMessage = err.error?.message || 'Errore durante il cambio password';
        this.showResultModal = true;
      }
    });
  }

  close(): void {
    this.isOpen = false;
    this.closed.emit();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close();
    }
  }

  fullName(): string {
    if (this.profile) {
      return [this.profile.firstName, this.profile.lastName].filter(Boolean).join(' ');
    }
    return this.user?.name || this.user?.username || '';
  }
}