import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { CubeService } from '../../../services/cube.service';
import { CubeCardDetails } from '../../../models/cube-card-details';

@Component({
  selector: 'app-cube-bans',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cube-bans.component.html',
    styleUrls: ['./cube-bans.component.css'],
})
export class CubeBansComponent implements OnInit {
  cubeId = 0;

  bannedCards: CubeCardDetails[] = [];
  selected: CubeCardDetails | null = null;

  viewMode: 'list' | 'image' = 'image';

  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private cubes: CubeService
  ) {}

  ngOnInit(): void {
    this.route.parent?.paramMap.subscribe((pm) => {
      this.cubeId = Number(pm.get('cubeId'));
      this.load();
    });
  }

  load() {
    this.loading = true;
    this.error = null;

    this.cubes.listCardsDetails(this.cubeId, true).subscribe({
      next: (rows) => {
        this.bannedCards = rows.filter((c) => c.banned);

        // keep selection if still present, otherwise select first
        if (this.selected) {
          const stillThere = this.bannedCards.find(x => x.cardId === this.selected!.cardId);
          this.selected = stillThere ?? null;
        }
        if (!this.selected && this.bannedCards.length > 0) {
          this.selected = this.bannedCards[0];
        }

        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load ban list.';
        this.loading = false;
      },
    });
  }

  select(card: CubeCardDetails) {
    this.selected = card;
  }

  isSelected(cardId: number) {
    return this.selected?.cardId === cardId;
  }

  getRowStyleForCard(c: { frameType?: string | null; cardType?: string | null; humanReadableCardType?: string | null }) {
    const ft = (c.frameType ?? '').toLowerCase();
    const type = (c.humanReadableCardType ?? c.cardType ?? '').toLowerCase();

    // prefer frameType
    if (ft.includes('normal'))   return { background: '#FDE075' };   // normal monster
    if (ft.includes('effect'))   return { background: '#F0803C' };   // effect monster
    if (ft.includes('spell'))    return { background: '#00A381' };   // spell
    if (ft.includes('trap'))     return { background: '#C05090' };   // trap
    if (ft.includes('fusion'))   return { background: '#8C61A3' };   // fusion
    if (ft.includes('ritual'))   return { background: '#98C8E0' };   // ritual
    if (ft.includes('synchro'))  return { background: '#FFFFFF' };   // synchro
    if (ft.includes('xyz'))      return { background: '#333333', color: '#fff' }; // xyz
    if (ft.includes('link'))     return { background: '#003366', color: '#fff' }; // link

    // fallback if you only have cardType strings
    if (type.includes('spell'))  return { background: '#00A381' };
    if (type.includes('trap'))   return { background: '#C05090' };
    if (type.includes('fusion')) return { background: '#8C61A3' };
    if (type.includes('ritual')) return { background: '#98C8E0' };
    if (type.includes('synchro'))return { background: '#FFFFFF' };
    if (type.includes('xyz'))    return { background: '#333333', color: '#fff' };
    if (type.includes('link'))   return { background: '#003366', color: '#fff' };
    if (type.includes('monster'))return { background: '#F0803C' };

    return {};
  }
}
