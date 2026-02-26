import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CubeCardDetails } from '../../models/cube-card-details';
import { CardSearchResult } from '../../models/card-search-result';

type DetailsCard = CubeCardDetails | CardSearchResult;

function isCubeCardDetails(c: DetailsCard): c is CubeCardDetails {
  return (c as CubeCardDetails).cardId !== undefined;
}

@Component({
  selector: 'app-card-details-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-details-panel.component.html',
  styleUrls: ['./card-details-panel.component.css'],
})
export class CardDetailsPanelComponent {
  @Input() card: DetailsCard | null = null;

  // shared
  get displayId(): number | null {
    if (!this.card) return null;
    return isCubeCardDetails(this.card) ? this.card.cardId : this.card.id;
  }

  get displayName(): string {
    if (!this.card) return '';
    const id = this.displayId;
    return this.card.name ?? (id != null ? `Card #${id}` : '');
  }

  get displayType(): string {
    if (!this.card) return '';
    return this.card.humanReadableCardType ?? this.card.cardType ?? '';
  }

  get imageUrl(): string | null {
    return this.card?.imageUrl ?? null;
  }

  // pool-only convenience
  get pool(): CubeCardDetails | null {
    if (!this.card) return null;
    return isCubeCardDetails(this.card) ? this.card : null;
  }

    get poolLevel(): number | null {
    return this.pool?.level ?? null;
  }

  get isXyz(): boolean {
    const p = this.pool;
    if (!p) return false;

    const ft = (p.frameType ?? '').toLowerCase();
    const type = (p.humanReadableCardType ?? p.cardType ?? '').toLowerCase();

    return ft.includes('xyz') || type.includes('xyz');
  }

    get attributeAlt(): string {
    // For tooltip/alt text
    const p = this.pool;

    // Spell / Trap come from cardType string
    const type = (this.displayType ?? '').toLowerCase();

    if (type.includes('spell')) return 'Spell';
    if (type.includes('trap')) return 'Trap';

    const attr = (p?.attribute ?? '').toLowerCase();
    return attr ? attr.toUpperCase() : 'Attribute';
  }

  get attributeIconUrl(): string {
    const p = this.pool;

    // Spell / Trap override attribute icons
    const type = (this.displayType ?? '').toLowerCase();
    if (type.includes('spell')) return 'assets/icons/spell.png';
    if (type.includes('trap')) return 'assets/icons/trap.png';

    // Monsters: use attribute field (LIGHT/DARK/etc)
    const raw = (p?.attribute ?? '').trim().toLowerCase();

    // expected: dark, divine, earth, fire, light, water, wind
    // fallback to light if something unexpected happens
    switch (raw) {
      case 'dark': return 'assets/icons/dark.png';
      case 'divine': return 'assets/icons/divine.png';
      case 'earth': return 'assets/icons/earth.png';
      case 'fire': return 'assets/icons/fire.png';
      case 'light': return 'assets/icons/light.png';
      case 'water': return 'assets/icons/water.png';
      case 'wind': return 'assets/icons/wind.png';
      default: return 'assets/icons/light.png';
    }
  }

  cleanDesc(desc?: string | null): string {

  if (!desc) return 'No description.';

  return desc
    .replace(/^[\s\u200B\u200C\u200D\uFEFF\u00A0]+/, '') // remove leading invisible junk
    .replace(/[\u200B\u200C\u200D\uFEFF]/g, '')         // remove zero-width inside text
    .trim();
}

formatTypeline(
  typeline?: string | null,
  frameType?: string | null,
  race?: string | null
): string {

  // MONSTERS
  if (typeline && typeline.trim().length > 0) {
    return '[' +
      typeline
        .split(',')
        .map(t => t.trim().toUpperCase())
        .join('/') +
      ']';
  }

  if (!frameType) return '';

  const ft = frameType.toUpperCase();

  // SPELL
  if (ft === 'SPELL') {
    if (!race || race.toUpperCase() === 'NORMAL') return '[SPELL]';
    return `[${race.toUpperCase()} SPELL]`;
  }

  // TRAP
  if (ft === 'TRAP') {
    if (!race || race.toUpperCase() === 'NORMAL') return '[TRAP]';
    return `[${race.toUpperCase()} TRAP]`;
  }

  return '';
}

get formattedTypeline(): string {
  return this.formatTypeline(
    this.pool?.typeline,
    this.card?.frameType ?? (this.pool as any)?.frameType,
    this.card?.race ?? (this.pool as any)?.race
  );
}
}
