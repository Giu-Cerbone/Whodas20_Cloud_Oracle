import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {
  resetForm!: FormGroup;
  loading = false;
  submitted = false;
  success = false;
  error = '';
  token = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.error = 'Token di reset mancante o non valido';
      return;
    }

    this.resetForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup) {
    const newPass = form.get('newPassword');
    const confirmPass = form.get('confirmPassword');
    if (newPass && confirmPass && newPass.value !== confirmPass.value) {
      confirmPass.setErrors({ mismatch: true });
    } else if (confirmPass?.errors?.['mismatch']) {
      delete confirmPass.errors['mismatch'];
      if (Object.keys(confirmPass.errors).length === 0) {
        confirmPass.setErrors(null);
      }
    }
  }

  get f() {
    return this.resetForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';

    if (this.resetForm.invalid) {
      return;
    }

    this.loading = true;
    this.userService.resetPassword({
      token: this.token,
      newPassword: this.f['newPassword'].value,
      confirmPassword: this.f['confirmPassword'].value
    }).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.success = true;
        } else {
          this.error = response.message;
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Errore durante il reset password';
      }
    });
  }

  goLogin(): void {
    this.router.navigate(['/login']);
  }
}