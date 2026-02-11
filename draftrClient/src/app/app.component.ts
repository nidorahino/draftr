import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavComponent } from './layout/nav/nav.component';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NavComponent, RouterOutlet],
template: `
  <app-nav></app-nav>
  <main class="app-page">
    <router-outlet></router-outlet>
  </main>
`,
})
export class AppComponent {
  constructor(private theme: ThemeService) {
    this.theme.init(); // initialize theme on app load
  }
}
