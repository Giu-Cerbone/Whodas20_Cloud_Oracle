import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { GuidelineModalComponent } from '../guideline-modal/guideline-modal.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule, GuidelineModalComponent],
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

  constructor(private authService: AuthService, private router: Router, private route: ActivatedRoute) {}

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

  onLogin(): void {
    this.error = '';
    this.loading = true;

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard'], { replaceUrl: true });
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Credenziali non valide';
      }
    });
  }
}