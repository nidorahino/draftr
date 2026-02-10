import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { CubeService } from '../../../services/cube.service';
import { CubeContextService } from '../../../services/cube-context.service';
import { CubeDetail } from '../../../models/cube-detail';

@Component({
  selector: 'app-cube-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cube-settings.component.html',
})
export class CubeSettingsComponent implements OnInit, OnDestroy {
  cubeId = 0;

  cube: CubeDetail | null = null;

  editing = false;
  saving = false;
  deleting = false;

  error: string | null = null;
  success = false;

  model = {
    name: '',
    maxPlayers: 8,
  };

  private original = {
    name: '',
    maxPlayers: 8,
  };

  private sub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private cubes: CubeService,
    private ctx: CubeContextService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cubeId = Number(this.route.parent?.snapshot.paramMap.get('cubeId'));

    // keep role + header in sync if cube updates
    this.sub = this.ctx.getCube().subscribe((c) => {
      this.cube = c;

      if (c && !this.editing) {
        this.model = { name: c.name, maxPlayers: c.maxPlayers };
        this.original = { ...this.model };
      }
    });

    // initialize from snapshot (in case subscription is late)
    const snap = this.ctx.snapshot();
    if (snap) {
      this.cube = snap;
      this.model = { name: snap.name, maxPlayers: snap.maxPlayers };
      this.original = { ...this.model };
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  // ---------- role gating ----------
  isOwnerOrAdmin(): boolean {
    const role = (this.cube?.myRole ?? '').toUpperCase();
    return role === 'OWNER' || role === 'ADMIN';
  }

  isMemberOnly(): boolean {
    const role = (this.cube?.myRole ?? '').toUpperCase();
    return role === 'MEMBER';
  }

  // ---------- edit flow ----------
  startEdit(): void {
    if (!this.isOwnerOrAdmin()) return;

    this.error = null;
    this.success = false;
    this.editing = true;
    this.original = { ...this.model };
  }

  cancelEdit(): void {
    this.error = null;
    this.success = false;
    this.editing = false;
    this.model = { ...this.original };
  }

  save(): void {
    if (!this.isOwnerOrAdmin()) return;

    this.saving = true;
    this.error = null;
    this.success = false;

    const payload = {
      name: this.model.name.trim(),
      maxPlayers: Number(this.model.maxPlayers),
    };

    this.cubes.updateCube(this.cubeId, payload).subscribe({
      next: (updated) => {
        this.saving = false;
        this.success = true;
        this.editing = false;

        this.model = { name: updated.name, maxPlayers: updated.maxPlayers };
        this.original = { ...this.model };

        // update context (updates CubeShell header immediately)
        const current = this.ctx.snapshot();
        if (current) {
          this.ctx.setCube({
            ...current,
            name: updated.name,
            maxPlayers: updated.maxPlayers,
          });
        }
      },
      error: (err) => {
        this.saving = false;
        this.error =
          typeof err?.error === 'string' ? err.error : 'Failed to save settings.';
      },
    });
  }

  // ---------- member actions ----------
  leaveCube(): void {
    if (!this.isMemberOnly()) return;

    if (!confirm('Leave this cube?')) return;

    this.deleting = true;
    this.error = null;
    this.success = false;

    this.cubes.leaveCube(this.cubeId).subscribe({
      next: () => {
        this.deleting = false;
        // optionally: navigate away to /my-cubes
        // this.router.navigate(['/my-cubes']);
      },
      error: (err) => {
        this.deleting = false;
        this.error =
          typeof err?.error === 'string' ? err.error : 'Failed to leave cube.';
      },
    });
  }

  archiveCube(): void {
    if (!this.isOwnerOrAdmin()) return;

    const name = this.model.name || this.cube?.name || 'this cube';

    const ok = confirm(
      `Archive "${name}"?\n\n` +
      `This will remove it from My Cubes.\n` +
      `This action cannot be undone.`
    );

    if (!ok) return;

    this.deleting = true;
    this.error = null;
    this.success = false;

    this.cubes.archiveCube(this.cubeId).subscribe({
      next: () => {
        this.deleting = false;

        // clear current cube so shell doesn't show stale header if user navigates back
        this.ctx.clear();

        // redirect to My Cubes
        this.router.navigate(['/my-cubes']);
      },
      error: (err: HttpErrorResponse) => {
        this.deleting = false;
        this.error =
          typeof err.error === 'string'
            ? err.error
            : 'Failed to archive cube.';
      },
    });
  }
}
