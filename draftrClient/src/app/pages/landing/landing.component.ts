import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css'],
})
export class LandingComponent {
  constructor(public auth: AuthService, private router: Router) {}

  @HostListener('window:keydown', ['$event'])
  onKeydown(event: KeyboardEvent) {
    if (event.key !== 'Enter') return;
    event.preventDefault();
    this.enterApp();
  }

  goToDashboard() {
    this.router.navigate(['/my-cubes']);
  }

  enterApp() {
    if (this.auth.isLoggedIn()) {
      this.goToDashboard();
    } else {
      this.router.navigate(['/login']);
    }
  }
}
