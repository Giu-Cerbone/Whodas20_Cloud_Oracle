import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-forgot-password-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './forgot-password-modal.component.html',
  styleUrls: ['./forgot-password-modal.component.css']
})
export class ForgotPasswordModalComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();

  emailForm!: FormGroup;
  loading = false;
  showConfirm = false;
  showResult = false;
  resultSuccess = false;
  resultMessage = '';

  constructor(private fb: FormBuilder, private userService: UserService) {}

  ngOnInit(): void {
    this.emailForm = this.fb.group({
      email: ['', Validators.required]
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.emailForm.reset();
      this.loading = false;
      this.showConfirm = false;
      this.showResult = false;
    }
  }

  get f() {
    return this.emailForm.controls;
  }

  onSubmit(): void {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }
    this.showConfirm = true;
  }

  cancelConfirm(): void {
    this.showConfirm = false;
  }

  confirmSend(): void {
    this.showConfirm = false;
    this.loading = true;
    this.userService.sendResetEmail({ usernameOrEmail: this.f['email'].value }).subscribe({
      next: (res) => {
        this.loading = false;
        this.resultSuccess = res.success;
        this.resultMessage = res.message;
        this.showResult = true;
      },
      error: (err) => {
        this.loading = false;
        this.resultSuccess = false;
        this.resultMessage = err.error?.message || 'Errore durante l\'invio dell\'email';
        this.showResult = true;
      }
    });
  }

  closeResult(): void {
    this.showResult = false;
    this.close();
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
}