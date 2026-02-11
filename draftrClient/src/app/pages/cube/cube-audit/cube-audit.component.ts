import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CommonModule } from '@angular/common';

import { CubeService } from '../../../services/cube.service';
import { CubeMember } from '../../../models/cube-member';
import { CubeCardDetails } from '../../../models/cube-card-details';
import { CubeEvent } from '../../../models/cube-event';
import { Page } from '../../../models/page';

@Component({
  selector: 'app-cube-audit',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cube-audit.component.html',
  styleUrls: ['./cube-audit.component.css'],
})
export class CubeAuditComponent implements OnInit {
  cubeId = 0;

  events: CubeEvent[] = [];

  page = 0;
  size = 25;
  totalPages = 0;

  loading = true;
  error: string | null = null;

  // lookups
  private userNameById = new Map<number, string>();
  private cardNameById = new Map<number, string>();

  constructor(
    private route: ActivatedRoute,
    private cubes: CubeService
  ) {}

  ngOnInit(): void {
    this.route.parent?.paramMap.subscribe((pm) => {
      this.cubeId = Number(pm.get('cubeId'));
      this.page = 0;
      this.load();
    });
  }

  load() {
    this.loading = true;
    this.error = null;

    // Load events + members + card details in parallel
    forkJoin({
      events: this.cubes.getEvents(this.cubeId, this.page, this.size),
      members: this.cubes.listMembers(this.cubeId),
      cards: this.cubes.listCardsDetails(this.cubeId, true),
    }).subscribe({
      next: ({ events, members, cards }) => {
        // events
        const p = events as Page<CubeEvent>;
        this.events = p.content ?? [];
        this.totalPages = p.totalPages ?? 0;

        // userId -> username
        this.userNameById.clear();
        for (const m of (members ?? []) as CubeMember[]) {
          const name = (m.username ?? '').trim();
          this.userNameById.set(m.userId, name ? name : `User #${m.userId}`);
        }

        // cardId -> card name
        this.cardNameById.clear();
        for (const c of (cards ?? []) as CubeCardDetails[]) {
          const name = (c.name ?? '').trim();
          this.cardNameById.set(c.cardId, name ? name : `Card #${c.cardId}`);
        }

        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load audit log.';
        this.loading = false;
      },
    });
  }

  actorName(id: number | null | undefined): string {
    if (!id && id !== 0) return 'Unknown';
    return this.userNameById.get(Number(id)) ?? `User #${id}`;
    }

  targetName(id: number | null | undefined): string {
    if (!id && id !== 0) return '—';
    return this.userNameById.get(Number(id)) ?? `User #${id}`;
  }

  cardName(cardId: number | null | undefined): string {
    if (!cardId && cardId !== 0) return '—';
    return this.cardNameById.get(Number(cardId)) ?? `Card #${cardId}`;
  }

  prev() {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  next() {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.load();
    }
  }
}