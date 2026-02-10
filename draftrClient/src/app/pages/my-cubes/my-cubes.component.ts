import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CubeService } from '../../services/cube.service';
import { CubeSummary } from '../../models/cube-summary';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-my-cubes',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './my-cubes.component.html',
  styleUrl: './my-cubes.component.css',
})
export class MyCubesComponent implements OnInit {
  cubes: CubeSummary[] = [];
  loading = true;
  error: string | null = null;

  showCreate = false;
  creating = false;
  createError: string | null = null;

  createModel = {
    name: '',
    maxPlayers: 8,
  };

  constructor(private cubesApi: CubeService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    this.cubesApi.listMyCubes().subscribe({
      next: (data) => {
        this.cubes = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load cubes. Are you logged in?';
        this.loading = false;
      },
    });
  }

  toggleCreate(): void {
    this.showCreate = !this.showCreate;
    this.createError = null;

    if (!this.showCreate) {
      this.createModel = { name: '', maxPlayers: 8 };
    }
  }

  createCube(): void {
    this.creating = true;
    this.createError = null;

    const payload = {
      name: this.createModel.name.trim(),
      maxPlayers: Number(this.createModel.maxPlayers),
    };

    this.cubesApi.createCube(payload).subscribe({
      next: () => {
        this.creating = false;
        this.showCreate = false;
        this.createModel = { name: '', maxPlayers: 8 };
        this.load();
      },
      error: (err) => {
        this.creating = false;

        // if backend returns plain text message on 400
        const msg =
          typeof err?.error === 'string'
            ? err.error
            : 'Failed to create cube.';

        this.createError = msg;
      },
    });
  }
}
