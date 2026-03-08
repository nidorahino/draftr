import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { CubeService } from '../../../services/cube.service';
import { CubeCardDetails } from '../../../models/cube-card-details';
import { CardDetailsPanelComponent } from '../../../components/card-details-panel/card-details-panel.component';
import { CardBrowserComponent } from '../../../components/card-browser/card-browser.component';

@Component({
  selector: 'app-cube-bans',
  standalone: true,
  imports: [CommonModule, CardDetailsPanelComponent, CardBrowserComponent],
  templateUrl: './cube-bans.component.html',
    styleUrls: ['./cube-bans.component.css'],
})
export class CubeBansComponent implements OnInit {
  cubeId = 0;

  bannedCards: CubeCardDetails[] = [];
  selected: CubeCardDetails | null = null;

  viewMode: 'list' | 'image' = 'list';

  loading = true;
  error: string | null = null;

  // browser glue
selectedCardId: number | null = null;

// selectors for app-card-browser
banId = (c: any) => c.cardId;
banName = (c: any) => c.name ?? '';
banImage = (c: any) => c.imageUrl ?? null;

banBadgeText = (_: any) => 'BANNED';
banBadgeClass = (_: any) => 'text-bg-danger';

banRowStyle = (c: any) => this.getRowStyleForCard(c);
banShowBannedIcon = (_: any) => true;

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

      // filter banned only
      this.bannedCards = rows
        .filter(c => c.banned)
        .sort((a: CubeCardDetails, b: CubeCardDetails) => {
          const frameCompare =
            this.getFrameTypeOrder(a.frameType) - this.getFrameTypeOrder(b.frameType);

          if (frameCompare !== 0) return frameCompare;

          return this.compareNullableValues(a.name ?? '', b.name ?? '');
        });

      // --------------------------------------------------
      // PRIMARY: restore selection from selectedCardId
      // --------------------------------------------------
      if (this.selectedCardId != null) {
        this.selected =
          this.bannedCards.find(c => c.cardId === this.selectedCardId) ?? null;
      }

      // --------------------------------------------------
      // SECONDARY: if nothing selected yet, select first
      // --------------------------------------------------
      if (!this.selected && this.bannedCards.length > 0) {
        this.selected = this.bannedCards[0];
      }

      // --------------------------------------------------
      // ALWAYS keep ID and object in sync
      // --------------------------------------------------
      this.selectedCardId = this.selected?.cardId ?? null;

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

  // Pendulum hybrids first
  if (ft.includes('pendulum')) {
    if (ft.includes('normal')) {
      return {
        background: 'linear-gradient(135deg, #F6D36A 0%, #4CB69F 100%)',
        color: '#000'
      };
    }

    if (ft.includes('effect')) {
      return {
        background: 'linear-gradient(135deg, #E79A63 0%, #4CB69F 100%)',
        color: '#000'
      };
    }

    if (ft.includes('fusion')) {
      return {
        background: 'linear-gradient(135deg, #9A76C2 0%, #4CB69F 100%)',
        color: '#000'
      };
    }

    if (ft.includes('synchro')) {
      return {
        background: 'linear-gradient(135deg, #F2F2F2 0%, #4CB69F 100%)',
        color: '#000'
      };
    }

    if (ft.includes('xyz')) {
      return {
        background: 'linear-gradient(135deg, #444444 0%, #4CB69F 100%)',
        color: '#fff'
      };
    }

    if (ft.includes('ritual')) {
      return {
        background: 'linear-gradient(135deg, #4F86C6 0%, #4CB69F 100%)',
        color: '#fff'
      };
    }
  }

  // Standard frames
  if (ft.includes('normal'))   return { background: '#F6D36A', color: '#000' };
  if (ft.includes('effect'))   return { background: '#E79A63', color: '#000' };
  if (ft.includes('spell'))    return { background: '#4CB69F', color: '#000' };
  if (ft.includes('trap'))     return { background: '#B96AA2', color: '#000' };
  if (ft.includes('fusion'))   return { background: '#9A76C2', color: '#000' };
  if (ft.includes('ritual'))   return { background: '#4F86C6', color: '#000' };
  if (ft.includes('synchro'))  return { background: '#F2F2F2', color: '#000' };
  if (ft.includes('xyz'))      return { background: '#444444', color: '#fff' };
  if (ft.includes('link'))     return { background: '#2F5D8C', color: '#fff' };

  return {};
}

  // keep RIGHT panel in sync
onBanSelectedIdChange(id: number | string | null) {
  this.selectedCardId = (id == null ? null : Number(id));
  this.selected = this.selectedCardId == null
    ? null
    : (this.bannedCards.find(c => c.cardId === this.selectedCardId) ?? null);
}

private readonly FRAME_TYPE_ORDER: Record<string, number> = {
  normal: 1,
  effect: 2,
  ritual: 3,
  fusion: 4,
  synchro: 5,
  xyz: 6,
  link: 7,

  normal_pendulum: 8,
  effect_pendulum: 9,
  ritual_pendulum: 10,
  fusion_pendulum: 11,
  synchro_pendulum: 12,
  xyz_pendulum: 13,

  spell: 20,
  trap: 21,
  token: 30,
};

private norm(x: any): string {
  return String(x ?? '').trim().toLowerCase();
}

private getFrameTypeOrder(frameType: string | null | undefined): number {
  const ft = this.norm(frameType);
  return this.FRAME_TYPE_ORDER[ft] ?? 999;
}

private compareNullableValues(a: any, b: any): number {
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;

  return String(a).localeCompare(String(b));
}
}
