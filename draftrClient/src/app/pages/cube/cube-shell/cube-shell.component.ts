import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';

import { CubeService } from '../../../services/cube.service';
import { CubeDetail } from '../../../models/cube-detail';
import { CubeContextService } from '../../../services/cube-context.service';

@Component({
  selector: 'app-cube-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet],
  templateUrl: './cube-shell.component.html',
})
export class CubeShellComponent implements OnInit, OnDestroy {
  cube: CubeDetail | null = null;
  loading = true;
  error: string | null = null;

  private sub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private cubes: CubeService,
    private ctx: CubeContextService
  ) {}

  ngOnInit(): void {
    // ✅ keep shell header in sync with context updates
    this.sub = this.ctx.getCube().subscribe((c) => {
      this.cube = c;
    });

    const cubeId = Number(this.route.snapshot.paramMap.get('cubeId'));

    this.cubes.getCube(cubeId).subscribe({
      next: (c) => {
        this.ctx.setCube(c);
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load cube.';
        this.loading = false;
      },
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  isAdmin(): boolean {
    return this.cube?.myRole === 'OWNER' || this.cube?.myRole === 'ADMIN';
  }
}
