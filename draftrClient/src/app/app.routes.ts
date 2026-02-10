import { Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { MyCubesComponent } from './pages/my-cubes/my-cubes.component';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';
import { CubeShellComponent } from './pages/cube/cube-shell/cube-shell.component';
import { CubeCardsComponent } from './pages/cube/cube-cards/cube-cards.component';
import { CubeMembersComponent } from './pages/cube/cube-members/cube-members.component';
import { CubeAuditComponent } from './pages/cube/cube-audit/cube-audit.component';
import { CubeBansComponent } from './pages/cube/cube-bans/cube-bans.component';
import { CubeCollectionComponent } from './pages/cube/cube-collection/cube-collection.component';
import { CubeSettingsComponent } from './pages/cube/cube-settings/cube-settings.component';
import { CubeDraftComponent } from './pages/cube/cube-draft/cube-draft.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },

  { path: 'my-cubes', component: MyCubesComponent, canActivate: [authGuard] },

  // Cube workspace
  {
    path: 'cubes/:cubeId',
    component: CubeShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'cards' },
      { path: 'cards', component: CubeCardsComponent },
      { path: 'members', component: CubeMembersComponent },
      { path: 'audit', component: CubeAuditComponent },
      { path: 'bans', component: CubeBansComponent },
      { path: 'collection', component: CubeCollectionComponent },
      { path: 'settings', component: CubeSettingsComponent },
      { path: 'draft', component: CubeDraftComponent },
    ],
  },

  { path: '**', redirectTo: '' },
];

