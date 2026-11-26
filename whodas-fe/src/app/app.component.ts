import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { NavigationEnd, NavigationStart, Router, RouterModule } from '@angular/router';
import { filter, Subscription, first } from 'rxjs';

import { AlertModalComponent } from './components/alert-modal/alert-modal.component';
import { ErrorModalComponent } from './components/error-modal/error-modal.component';
import { NotificationPanelComponent } from './components/notification-panel/notification-panel.component';
import { GuidelineModalComponent } from './components/guideline-modal/guideline-modal.component';
import { UserProfileModalComponent } from './components/user-profile-modal/user-profile-modal.component';
import { ErrorModalService } from './services/error-modal.service';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, ErrorModalComponent, AlertModalComponent, NotificationPanelComponent, GuidelineModalComponent, UserProfileModalComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'whodas-fe';
  showNavbar = false;
  showGuideline = false;
  showUserProfile = false;
  private userSub!: Subscription;
  private routerSub!: Subscription;
  private popSub!: Subscription;
  private lastUrl = '';

  constructor(
    private router: Router,
    public errorModalService: ErrorModalService,
    public authService: AuthService,
    public notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    // Aspetta il primo NavigationEnd per conoscere l'URL reale (dopo redirect)
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      first()
    ).subscribe((event: NavigationEnd) => {
      this.showNavbar = !event.urlAfterRedirects.includes('/login') && !event.urlAfterRedirects.includes('/register') && !event.urlAfterRedirects.includes('/reset-password') && !event.urlAfterRedirects.includes('/forgot-password');
      if (this.showNavbar) {
        this.initNotifications();
      }
    });

    // Gestisce navigazioni successive
    this.routerSub = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      const newShowNavbar = !event.urlAfterRedirects.includes('/login') && !event.urlAfterRedirects.includes('/register') && !event.urlAfterRedirects.includes('/reset-password') && !event.urlAfterRedirects.includes('/forgot-password');
      if (newShowNavbar !== this.showNavbar) {
        this.showNavbar = newShowNavbar;
        if (this.showNavbar) {
          this.initNotifications();
        } else {
          this.notificationService.disconnect();
          this.userSub?.unsubscribe();
        }
      }
      window.scrollTo(0, 0);
      this.lastUrl = event.urlAfterRedirects;
      history.pushState(null, '', location.href);
    });

    // Indietro/avanti del browser: si resta sempre sulla pagina corrente
    this.popSub = this.router.events.pipe(
      filter((event): event is NavigationStart =>
        event instanceof NavigationStart && event.navigationTrigger === 'popstate')
    ).subscribe(() => {
      if (this.lastUrl) {
        this.router.navigateByUrl(this.lastUrl, { replaceUrl: true });
      }
    });

    // Blocca il "indietro" del browser: ripristina la entry corrente
    window.addEventListener('popstate', this.blockBack);
  }

  private blockBack = (): void => {
    history.pushState(null, '', location.href);
  };

  private initNotifications(): void {
    this.userSub?.unsubscribe();
    this.userSub = this.authService.currentUser$.subscribe(user => {
      if (user && user.username) {
        this.notificationService.connect(user.username);
      } else {
        this.notificationService.disconnect();
      }
    });
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    this.routerSub?.unsubscribe();
    this.popSub?.unsubscribe();
    window.removeEventListener('popstate', this.blockBack);
    this.notificationService.disconnect();
  }

  logout(): void {
    this.notificationService.disconnect();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  canSeeUsers(): boolean {
    const u = this.authService.getCurrentUser()?.username;
    return u === 'whodas_user' || u === 'testuser';
  }

  openGuideline(): void {
    this.showGuideline = true;
  }

  onGuidelineClose(): void {
    this.showGuideline = false;
  }

  openUserProfile(): void {
    this.showUserProfile = true;
  }

  onUserProfileClose(): void {
    this.showUserProfile = false;
  }
}
