import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, firstValueFrom } from 'rxjs';

import { CubeService } from '../../../services/cube.service';
import { CubeCardDetails } from '../../../models/cube-card-details';
import { CubeContextService } from '../../../services/cube-context.service';
import { Page } from '../../../models/page';
import { CardSearchResult } from '../../../models/card-search-result';
import { CardFilters, DEFAULT_FILTERS } from '../../../models/card-filters';
import { CardDetailsPanelComponent } from '../../../components/card-details-panel/card-details-panel.component';
import { CardBrowserComponent } from '../../../components/card-browser/card-browser.component';
import { CardFiltersPanelComponent } from '../../../components/card-filters-panel/card-filters-panel.component';

@Component({
  selector: 'app-cube-cards',
  standalone: true,
  imports: [CommonModule, FormsModule, CardDetailsPanelComponent, CardBrowserComponent, CardFiltersPanelComponent],
  templateUrl: './cube-cards.component.html',
    styleUrls: ['./cube-cards.component.css'],
})
export class CubeCardsComponent implements OnInit {
  cubeId = 0;

  cards: CubeCardDetails[] = [];
  selected: CubeCardDetails | null = null;

  viewMode: 'list' | 'image' = 'list';
  includeBanned = true;

  loading = true;
  error: string | null = null;

  // role gating
  myRole: string | null = null;

  // Admin add form
  addCardId: number | null = null;
  addMaxQty: number | null = 1;

  // Admin per-card edit state
  editMaxQty: Record<number, number> = {};
  banReason: Record<number, string> = {};

  // search
  selectedSearch: CardSearchResult | null = null;
  searchName = '';
  searchPage = 0;
  searchSize = 10;

  searching = false;
  searchError: string | null = null;

  searchResults: CardSearchResult[] = [];
  searchTotalPages = 0;
  private selectAfterLoadCardId: number | null = null;

  frameTypeOptions: string[] = [];
monsterTypeOptions: string[] = [];
spellTypeOptions: string[] = [];
trapTypeOptions: string[] = [];

  // card-browser glue
selectedCardId: number | null = null;

poolId = (c: CubeCardDetails) => c.cardId;

poolName = (c: CubeCardDetails) => c.name ?? '';

poolImage = (c: CubeCardDetails) => c.imageUrl ?? null;

poolBadgeText = (c: CubeCardDetails) => c.banned ? 'BANNED' : `QTY ${c.maxQty}`;

poolBadgeClass = (c: CubeCardDetails) => (c.banned ? 'text-bg-danger' : 'text-bg-dark');

poolRowStyle = (c: CubeCardDetails) => this.getRowStyleForCard(c);

poolShowBannedIcon = (c: CubeCardDetails) => !!c.banned;

  // per-result max qty input
  searchMaxQty: Record<number, number> = {};

  // CSV import
  csvText = '';
  csvFileName: string | null = null;

  csvImporting = false;

  csvPreview = {
    lines: 0,
    parsed: 0,
    valid: 0,
    errors: [] as string[],
    items: [] as { cardId: number; qty: number }[],
  };

  csvImportResult: null | {
    added: number;
    failed: { cardId: number; qty: number; reason: string }[];
  } = null;

  private uniq(arr: string[]): string[] {
    return Array.from(new Set(arr)).sort((a, b) => a.localeCompare(b));
  }

  adminToolOpen: 'search' | 'manual' | 'csv' | null = null;
  adminToolsOpen = false;

  private search$ = new Subject<string>();

  filtersOpen = true;
  filters: CardFilters = { ...DEFAULT_FILTERS };

  // render these instead of `cards`
  filteredCards: CubeCardDetails[] = [];

  // options (auto-derived after load)
  cardTypeOptions: string[] = [];
  attributeOptions: string[] = [];
  raceOptions: string[] = [];

  

  constructor(
    private route: ActivatedRoute,
    private cubes: CubeService,
    private ctx: CubeContextService
  ) {}

  ngOnInit(): void {
    // role from shell
    this.ctx.getCube().subscribe((c) => {
      this.myRole = c?.myRole ?? null;
    });

    // cubeId from parent route
    this.route.parent?.paramMap.subscribe((pm) => {
      this.cubeId = Number(pm.get('cubeId'));
      this.load();
    });

    this.search$
    .pipe(debounceTime(300), distinctUntilChanged())
    .subscribe((term) => {
      this.searchPage = 0;
      this.runSearch(term);
    });
  }

  isAdmin(): boolean {
    return this.myRole === 'OWNER' || this.myRole === 'ADMIN';
  }

  onPoolSelectedIdChange(id: number | string | null) {
  this.selectedCardId = (id == null ? null : Number(id));
  this.selected = this.selectedCardId == null
    ? null
    : (this.cards.find(c => c.cardId === this.selectedCardId) ?? null);

  if (this.selected) this.selectedSearch = null;
}

onFiltersChange(f: CardFilters) {
  this.filters = { ...f };
  this.applyFilters();
}

  load() {
    this.loading = true;
    this.error = null;

    this.cubes.listCardsDetails(this.cubeId, this.includeBanned).subscribe({
      next: (rows) => {
        this.cards = rows;

this.frameTypeOptions = this.uniq(
  rows.map(x => x.frameType).filter(Boolean) as string[]
);

this.attributeOptions = this.uniq(
  rows.filter(x => this.isMonsterFrame(x.frameType))
      .map(x => x.attribute)
      .filter(Boolean) as string[]
);

this.monsterTypeOptions = this.uniq(
  rows.filter(x => this.isMonsterFrame(x.frameType))
      .map(x => x.race)
      .filter(Boolean) as string[]
);

this.spellTypeOptions = this.uniq(
  rows.filter(x => this.isSpellFrame(x.frameType))
      .map(x => x.race)
      .filter(Boolean) as string[]
);

this.trapTypeOptions = this.uniq(
  rows.filter(x => this.isTrapFrame(x.frameType))
      .map(x => x.race)
      .filter(Boolean) as string[]
);

        this.applyFilters();

        if (this.selectAfterLoadCardId != null) {
          const added = rows.find(x => x.cardId === this.selectAfterLoadCardId);
          if (added) {
            this.selected = added;
            this.selectedSearch = null;
          }
          this.selectAfterLoadCardId = null;
        }

        this.editMaxQty = {};
        for (const c of rows) this.editMaxQty[c.cardId] = c.maxQty;

        if (this.selected) {
          const stillThere = rows.find(x => x.cardId === this.selected!.cardId);
          this.selected = stillThere ?? null;
        }

        if (!this.selected && rows.length > 0) {
          this.selected = rows[0];
        }
this.selectedCardId = this.selected?.cardId ?? null;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load card pool.';
        this.loading = false;
      },
    });
  }

select(card: CubeCardDetails) {
  this.selected = card;
  this.selectedCardId = card.cardId;
  this.selectedSearch = null;
}

  isSelected(cardId: number) {
    return this.selected?.cardId === cardId;
  }

  // ---- Admin actions
  addCard() {
    if (!this.isAdmin()) return;

    const cardId = Number(this.addCardId);
    const maxQty = Number(this.addMaxQty);

    if (!cardId || !maxQty || maxQty < 1) {
      this.error = 'Enter a valid cardId and maxQty (>= 1).';
      return;
    }

    this.error = null;

    this.cubes.addCard(this.cubeId, cardId, maxQty).subscribe({
      next: () => {
        this.addCardId = null;
        this.addMaxQty = 1;
        this.load();
      },
      error: () => (this.error = 'Failed to add card.'),
    });
  }

  saveMaxQty(cardId: number) {
    if (!this.isAdmin()) return;

    const maxQty = Number(this.editMaxQty[cardId]);
    if (!maxQty || maxQty < 1) {
      this.error = 'maxQty must be >= 1.';
      return;
    }

    this.error = null;

    this.cubes.updateMaxQty(this.cubeId, cardId, maxQty).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Failed to update maxQty.'),
    });
  }

  toggleBan(cardId: number, currentlyBanned: boolean) {
    if (!this.isAdmin()) return;

    const banned = !currentlyBanned;
    const reason = (this.banReason[cardId] ?? '').trim();

    this.error = null;

    this.cubes.setBanned(this.cubeId, cardId, banned, reason || undefined).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Failed to update ban status.'),
    });
  }

  remove(cardId: number) {
    if (!this.isAdmin()) return;

    if (!confirm(`Remove card ${cardId} from cube?`)) return;

    this.error = null;

    this.cubes.removeCard(this.cubeId, cardId).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Failed to remove card.'),
    });
  }

  // search
  onSearchNameChange(val: string) {
    this.searchName = val;
    const term = (val ?? '').trim();

    // keep UI clean: clear results if empty
    if (!term) {
      this.searchResults = [];
      this.searchTotalPages = 0;
      this.searchError = null;
      return;
    }

    // your backend enforces min length 2 for /search
    if (term.length < 2) {
      this.searchResults = [];
      this.searchTotalPages = 0;
      this.searchError = 'Type at least 2 characters.';
      return;
    }

    this.searchError = null;
    this.search$.next(term);
  }

  runSearch(term: string) {
    const trimmed = term.trim();
    if (trimmed.length < 2) return;

    this.searching = true;
    this.searchError = null;

    this.cubes.searchCardsByName(trimmed, this.searchPage, this.searchSize).subscribe({
      next: (page: Page<CardSearchResult>) => {
        this.searchResults = page.content ?? [];
        this.searchTotalPages = page.totalPages ?? 0;

        // default max qty for results
        for (const r of this.searchResults) {
          if (this.searchMaxQty[r.id] == null) this.searchMaxQty[r.id] = 1;
        }

        this.searching = false;
      },
      error: (err) => {
        // if backend returns 400 for short term, show message
        this.searchError = err?.error ?? 'Failed to search cards.';
        this.searching = false;
      },
    });
  }

  nextSearchPage() {
    if (this.searchPage + 1 >= this.searchTotalPages) return;
    this.searchPage++;
    this.runSearch(this.searchName);
  }

  prevSearchPage() {
    if (this.searchPage <= 0) return;
    this.searchPage--;
    this.runSearch(this.searchName);
  }

  addFromSearch(card: CardSearchResult) {
    if (!this.isAdmin()) return;

    const maxQty = Number(this.searchMaxQty[card.id] ?? 1);
    if (!maxQty || maxQty < 1) {
      this.error = 'maxQty must be >= 1.';
      return;
    }

    this.error = null;

    this.cubes.addCard(this.cubeId, card.id, maxQty).subscribe({
      next: () => {
        this.selectAfterLoadCardId = card.id;

        // refresh cube pool
        this.load();

        // optional: remove added card from search list for nicer UX
        this.searchResults = this.searchResults.filter((x) => x.id !== card.id);
      },
      error: (err) => {
        this.error = err?.error ?? 'Failed to add card.';
      },
    });
  }

  openTool(which: 'search' | 'manual' | 'csv') {
    this.adminToolOpen = which;
  }

  closeTool() {
    this.adminToolOpen = null;
  }

  closeToolAndClearSearch() {
    // optional: if you want closing to clear search state
    this.searchName = '';
    this.searchResults = [];
    this.searchTotalPages = 0;
    this.searchError = null;
    this.searching = false;
    this.adminToolOpen = null;
  }

  selectSearch(r: CardSearchResult) {
    this.selectedSearch = r;
    this.selected = null; // ✅ ensures right panel shows search details
    if (this.searchMaxQty[r.id] == null) this.searchMaxQty[r.id] = 1;
  }

  isSelectedSearch(id: number) {
    return this.selectedSearch?.id === id;
  }

  // Add to cube from the RIGHT panel when selectedSearch is shown
  addSelectedSearch() {
    if (!this.isAdmin() || !this.selectedSearch) return;

    const cardId = this.selectedSearch.id;
    const maxQty = Number(this.searchMaxQty[cardId] ?? 1);

    if (!maxQty || maxQty < 1) {
      this.error = 'maxQty must be >= 1.';
      return;
    }

    this.error = null;

    this.cubes.addCard(this.cubeId, cardId, maxQty).subscribe({
      next: () => {
        this.load();
        // optional: after adding, keep showing it or switch back to cube selection
        // this.selectedSearch = null;
      },
      error: (err) => {
        this.error = err?.error ?? 'Failed to add card.';
      },
    });
  }

  toggleAdminTools() {
    this.adminToolsOpen = !this.adminToolsOpen;
    if (!this.adminToolsOpen) {
      this.adminToolOpen = null; // collapse also closes any open tool
    }
  }

  closeAdminTools() {
    this.adminToolsOpen = false;
    this.adminToolOpen = null;
  }

  parseCsvPreview(): void {
    // reset
    this.csvPreview = {
      lines: 0,
      parsed: 0,
      valid: 0,
      errors: [],
      items: [],
    };

    let text = (this.csvText || '');

    // remove BOM if present
    text = text.replace(/^\uFEFF/, '').trim();
    if (!text) return;

    // normalize line endings:
    // - \r\n -> \n
    // - \r   -> \n  (important!)
    text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

    // split by newline
    let lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);

    // FALLBACK: if we still only have 1 line but it contains many records,
    // split into "pseudo-lines" by finding where a new record starts: <digits>,
    if (lines.length === 1) {
      const one = lines[0];

      // find indices where a record likely starts (e.g. 44256816,"Name"...)
      const starts: number[] = [];
      const re = /(^|\s)(\d{5,})\s*,/g;
      let m: RegExpExecArray | null;

      while ((m = re.exec(one)) !== null) {
        const startIndex = m.index + (m[1]?.length ?? 0); // skip leading whitespace if matched
        starts.push(startIndex);
      }

      // if we found multiple record starts, slice them into separate lines
      if (starts.length > 1) {
        const sliced: string[] = [];
        for (let i = 0; i < starts.length; i++) {
          const from = starts[i];
          const to = i + 1 < starts.length ? starts[i + 1] : one.length;
          const piece = one.slice(from, to).trim();
          if (piece) sliced.push(piece);
        }
        lines = sliced;
      }
    }

    this.csvPreview.lines = lines.length;

    // consolidate duplicates: cardId -> qty sum
    const map = new Map<number, number>();

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      const firstComma = line.indexOf(',');
      const lastComma = line.lastIndexOf(',');

      if (firstComma < 1 || lastComma <= firstComma) {
        this.csvPreview.errors.push(`Line ${i + 1}: invalid format`);
        continue;
      }

      const idStr = line.substring(0, firstComma).replace(/"/g, '').trim();
      const qtyStr = line.substring(lastComma + 1).replace(/"/g, '').trim();

      const cardId = Number(idStr);
      const qty = Number(qtyStr);

      this.csvPreview.parsed++;

      if (!Number.isInteger(cardId) || cardId <= 0) {
        this.csvPreview.errors.push(`Line ${i + 1}: invalid card id`);
        continue;
      }
      if (!Number.isInteger(qty) || qty <= 0 || qty > 99) {
        this.csvPreview.errors.push(`Line ${i + 1}: invalid qty`);
        continue;
      }

      map.set(cardId, (map.get(cardId) ?? 0) + qty);
    }

    this.csvPreview.items = Array.from(map.entries()).map(([cardId, qty]) => ({ cardId, qty }));
    this.csvPreview.valid = this.csvPreview.items.length;
  }

  clearCsv(): void {
    this.csvText = '';
    this.csvFileName = null;
    this.csvImportResult = null;
    this.csvPreview = { lines: 0, parsed: 0, valid: 0, errors: [], items: [] };
  }

  async importCsv(): Promise<void> {
    if (!this.isAdmin()) return;

    this.parseCsvPreview();
    if (this.csvPreview.items.length === 0) return;

    this.csvImporting = true;
    this.csvImportResult = { added: 0, failed: [] };

    // Import one-by-one (easy, safe). If you later want speed, we can batch endpoint.
    for (const item of this.csvPreview.items) {
      const cardId = item.cardId;
      const qty = item.qty;

      try {
        await firstValueFrom(this.cubes.addCard(this.cubeId, cardId, qty));
        this.csvImportResult.added++;
      } catch (err: any) {
        const msg =
          typeof err?.error === 'string'
            ? err.error
            : (err?.status === 404 ? 'card not found' : 'failed to add');

        this.csvImportResult.failed.push({ cardId, qty, reason: msg });
      }
    }

    this.csvImporting = false;

    // refresh pool
    this.load();
  }

  onCsvFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) return;

    // optional guardrails
    const maxBytes = 2_000_000; // 2 MB
    if (file.size > maxBytes) {
      this.error = 'CSV file is too large (max 2MB).';
      input.value = '';
      return;
    }

    this.csvFileName = file.name;
    this.csvImportResult = null;

    const reader = new FileReader();

    reader.onload = () => {
      // normalize to string
      const text = String(reader.result ?? '');
      this.csvText = text;
      this.parseCsvPreview();
    };

    reader.onerror = () => {
      this.error = 'Failed to read CSV file.';
    };

    reader.readAsText(file);
  }

  resetFilters() {
    this.filters = { ...DEFAULT_FILTERS };
    this.applyFilters();
  }

applyFilters() {
  const f = this.filters;

  const contains = (v: string | null | undefined, q: string) =>
    (v ?? '').toLowerCase().includes(q.toLowerCase());

  const eqi = (a: any, b: any) =>
    String(a ?? '').trim().toLowerCase() === String(b ?? '').trim().toLowerCase();

  const inRange = (n: number | null | undefined, min: number | null, max: number | null) => {
    if (n == null) return false;
    if (min != null && n < min) return false;
    if (max != null && n > max) return false;
    return true;
  };

  let list = [...this.cards];

  // Name search
  if (f.q.trim()) {
    list = list.filter(c => contains(c.name, f.q.trim()));
  }

  // Frame Type
  if (f.frameType) {
    list = list.filter(c => eqi(c.frameType, f.frameType));
  }

  // Attribute (monsters only)
  if (f.attribute) {
    list = list.filter(c =>
      this.isMonsterFrame(c.frameType) &&
      eqi(c.attribute, f.attribute)
    );
  }

  // Race / subtype
  if (f.race) {
    list = list.filter(c => eqi(c.race, f.race));
  }

  // Stats only for monsters
  const lvlActive = f.levelMin != null || f.levelMax != null;
  const atkActive = f.atkMin != null || f.atkMax != null;
  const defActive = f.defMin != null || f.defMax != null;

  const usingStats = lvlActive || atkActive || defActive;

  if (usingStats) {
    list = list.filter(c => this.isMonsterFrame(c.frameType));

    if (lvlActive) list = list.filter(c => inRange(c.level, f.levelMin, f.levelMax));
    if (atkActive) list = list.filter(c => inRange(c.atk, f.atkMin, f.atkMax));
    if (defActive) list = list.filter(c => inRange(c.def, f.defMin, f.defMax));
  }

  // Sort: frame type first, chosen field second
  const dir = f.sortDir === 'asc' ? 1 : -1;

  list.sort((a: CubeCardDetails, b: CubeCardDetails) => {
    const frameCompare =
      this.getFrameTypeOrder(a.frameType) - this.getFrameTypeOrder(b.frameType);

    if (frameCompare !== 0) return frameCompare;

    const av = f.sortKey === 'name' ? (a.name ?? '') : (a as any)[f.sortKey];
    const bv = f.sortKey === 'name' ? (b.name ?? '') : (b as any)[f.sortKey];

    const valueCompare = this.compareNullableValues(av, bv) * dir;
    if (valueCompare !== 0) return valueCompare;

    return this.compareNullableValues(a.name ?? '', b.name ?? '');
  });

  this.filteredCards = list;
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

getActiveFilterCount(): number {
  const f = this.filters;

  const nums = [
    f.levelMin, f.levelMax, f.atkMin, f.atkMax, f.defMin, f.defMax
  ].filter(v => v !== null && v !== undefined).length;

const text = [
  f.q, f.frameType, f.attribute, f.race
].filter(v => (v ?? '').toString().trim().length > 0).length;

  const sort = (f.sortKey && f.sortKey !== 'name' ? 1 : 0) + (f.sortDir && f.sortDir !== 'asc' ? 1 : 0);

  return nums + text + sort;
}

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

private getFrameTypeOrder(frameType: string | null | undefined): number {
  const ft = this.norm(frameType);
  return this.FRAME_TYPE_ORDER[ft] ?? 999;
}

private compareNullableValues(a: any, b: any): number {
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;

  if (typeof a === 'string' || typeof b === 'string') {
    return String(a).localeCompare(String(b));
  }

  return Number(a) - Number(b);
}
}
