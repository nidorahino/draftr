import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CardFilters, DEFAULT_FILTERS } from '../../models/card-filters';

@Component({
  selector: 'app-card-filters-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card-filters-panel.component.html',
})
export class CardFiltersPanelComponent implements OnChanges {
  @Input() filters: CardFilters = { ...DEFAULT_FILTERS };
  @Output() filtersChange = new EventEmitter<CardFilters>();

  @Input() frameTypeOptions: string[] = [];
  @Input() attributeOptions: string[] = [];

  @Input() monsterTypeOptions: string[] = [];
  @Input() spellTypeOptions: string[] = [];
  @Input() trapTypeOptions: string[] = [];

  @Input() showing = 0;
  @Input() total = 0;

  @Input() showToggle = false;
  @Input() filtersOpen = true;
  @Output() filtersOpenChange = new EventEmitter<boolean>();

  model: CardFilters = { ...DEFAULT_FILTERS };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['filters']) {
      this.model = { ...(this.filters ?? DEFAULT_FILTERS) };
    }
  }

  onAnyChange(): void {
    this.filtersChange.emit({ ...this.model });
  }

  reset(): void {
    this.model = { ...DEFAULT_FILTERS };
    this.filtersChange.emit({ ...this.model });
  }

  toggle(): void {
    this.filtersOpen = !this.filtersOpen;
    this.filtersOpenChange.emit(this.filtersOpen);
  }

  onFrameTypeChange(): void {
    const ft = this.model.frameType;

    // if Spell/Trap picked, clear monster-only filters
    if (this.isSpellFrame(ft) || this.isTrapFrame(ft)) {
      this.model.attribute = '';
      this.model.levelMin = null;
      this.model.levelMax = null;
      this.model.atkMin = null;
      this.model.atkMax = null;
      this.model.defMin = null;
      this.model.defMax = null;

      // also reset sort to name (since atk/def/level meaningless)
      this.model.sortKey = 'name';
    }

    this.onAnyChange();
  }

  getRaceLabel(): string {
    const ft = this.model.frameType;
    if (this.isSpellFrame(ft)) return 'Spell Type';
    if (this.isTrapFrame(ft)) return 'Trap Type';
    return 'Monster Type';
  }

  getRaceOptions(): string[] {
    const ft = this.model.frameType;
    if (this.isSpellFrame(ft)) return this.spellTypeOptions;
    if (this.isTrapFrame(ft)) return this.trapTypeOptions;
    return this.monsterTypeOptions;
  }

  showMonsterOnly(): boolean {
    const ft = this.model.frameType;
    return !ft || this.isMonsterFrame(ft);
  }

  getActiveFilterCount(): number {
    const f = this.model;

    const nums =
      [f.levelMin, f.levelMax, f.atkMin, f.atkMax, f.defMin, f.defMax].filter(
        v => v !== null && v !== undefined
      ).length;

    const text =
      [f.q, f.frameType, f.attribute, f.race].filter(v => (v ?? '').toString().trim().length > 0)
        .length;

    const sort =
      (f.sortKey && f.sortKey !== 'name' ? 1 : 0) + (f.sortDir && f.sortDir !== 'asc' ? 1 : 0);

    return nums + text + sort;
  }

  // ===== internal helpers =====
  private norm(x: any): string {
    return String(x ?? '').trim().toLowerCase();
  }

  private isSpellFrame(frameType: string | null | undefined): boolean {
    const ft = this.norm(frameType);
    return ft === 'spell' || ft.includes('spell');
  }

  private isTrapFrame(frameType: string | null | undefined): boolean {
    const ft = this.norm(frameType);
    return ft === 'trap' || ft.includes('trap');
  }

  private isMonsterFrame(frameType: string | null | undefined): boolean {
    const ft = this.norm(frameType);
    if (!ft) return true;
    return !this.isSpellFrame(ft) && !this.isTrapFrame(ft);
  }

  formatFrameType(ft: string | null | undefined): string {
  const raw = (ft ?? '').toString().trim();
  if (!raw) return '';

  // effect_pendulum -> effect/pendulum
  const withSlash = raw.replace(/_/g, '/');

  // title-case each segment around /
  return withSlash
    .split('/')
    .map(seg => this.titleCase(seg))
    .join('/');
}

private titleCase(s: string): string {
  const x = (s ?? '').toString().trim();
  if (!x) return x;
  return x.charAt(0).toUpperCase() + x.slice(1).toLowerCase();
}
}