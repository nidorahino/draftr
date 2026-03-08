import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { CubeService } from '../../../services/cube.service';
import { UserService } from '../../../services/user.service';
import { CubeCollectionCard } from '../../../models/cube-collection-card';
import { CubeCardDetails } from '../../../models/cube-card-details';
import { CubeMember } from '../../../models/cube-member';
import { CubeContextService } from '../../../services/cube-context.service';
import { CardSearchResult } from '../../../models/card-search-result';
import { Page } from '../../../models/page';
import { CardFilters, DEFAULT_FILTERS } from '../../../models/card-filters';
import { CardDetailsPanelComponent } from '../../../components/card-details-panel/card-details-panel.component';
import { CardBrowserComponent } from '../../../components/card-browser/card-browser.component';
import { CardFiltersPanelComponent } from '../../../components/card-filters-panel/card-filters-panel.component';

type CollectionItem = {
  cardId: number;
  qty: number;
  banned: boolean;
  updatedAt: string;
  details?: CubeCardDetails;
  atk?: number;
  def?: number;
  level?: number;
};

@Component({
  selector: 'app-cube-collection',
  standalone: true,
  imports: [CommonModule, FormsModule, CardDetailsPanelComponent, CardBrowserComponent, CardFiltersPanelComponent],
  templateUrl: './cube-collection.component.html',
    styleUrls: ['./cube-collection.component.css'],
})
export class CubeCollectionComponent implements OnInit {
  cubeId = 0;
  myUserId = 0;

  viewMode: 'list' | 'image' = 'image';

  items: CollectionItem[] = [];
  selected: CollectionItem | null = null;

  loading = true;
  error: string | null = null;

  // edit qty in side panel
  qtyDraft: number | null = null;

  // collection edit
  myRole: string | null = null;
  activeUserId = 0;
  members: CubeMember[] = [];
  membersLoading = false;

  // admin-only pool search (for adding to collection)
  adminToolsOpen = false;

  searchName = '';
  searchPage = 0;
  searchSize = 10;
  searching = false;
  searchError: string | null = null;
  searchResults: CardSearchResult[] = [];
  searchTotalPages = 0;

  selectedSearch: CardSearchResult | null = null;
  addQtyDraft: number | null = 1;

  private search$ = new Subject<string>();

  private selectAfterLoadCardId: number | null = null;

  filters: CardFilters = { ...DEFAULT_FILTERS };
  filteredItems: CollectionItem[] = [];

  attributeOptions: string[] = [];

  frameTypeOptions: string[] = [];
monsterTypeOptions: string[] = [];
spellTypeOptions: string[] = [];
trapTypeOptions: string[] = [];

  private uniq(arr: string[]): string[] {
    return Array.from(new Set(arr)).sort((a, b) => a.localeCompare(b));
  }

  constructor(
    private route: ActivatedRoute,
    private cubes: CubeService,
    private users: UserService,
    private ctx: CubeContextService
  ) {}

  ngOnInit(): void {
    // role from shell
    this.ctx.getCube().subscribe((c) => {
      this.myRole = c?.myRole ?? null;

      // ✅ only load members when cubeId is known
      if (this.cubeId) {
        this.loadMembersIfAdmin(this.cubeId);
      }
    });

    this.route.parent?.paramMap.subscribe((pm) => {
      this.cubeId = Number(pm.get('cubeId'));

      this.loading = true;
      this.error = null;

      this.users.me().subscribe({
        next: (me) => {
          this.myUserId = me.userId;
          this.activeUserId = this.myUserId;
          this.load();

          // ✅ now cubeId is known; if role already known, this will work
          this.loadMembersIfAdmin(this.cubeId);
        },
        error: () => {
          this.error = 'Failed to load user (me).';
          this.loading = false;
        },
      });
    });

    this.search$
    .pipe(debounceTime(300), distinctUntilChanged())
    .subscribe((term) => {
      this.searchPage = 0;
      this.runPoolSearch(term);
    });
  }

  onFiltersChange(f: CardFilters) {
  this.filters = { ...f };
  this.applyFilters();
}

load() {
  this.loading = true;
  this.error = null;

  // 1) load card details (for names/images/types)
  this.cubes.listCardsDetails(this.cubeId, true).subscribe({
    next: (detailsRows: CubeCardDetails[]) => {
      const byId = new Map<number, CubeCardDetails>();
      for (const d of detailsRows) byId.set(d.cardId, d);

      // 2) load the active user's collection
      this.cubes.getCollection(this.cubeId, this.activeUserId).subscribe({
        next: (rows: CubeCollectionCard[]) => {
          this.items = rows.map((r) => ({
            cardId: r.cardId,
            qty: r.qty,
            banned: r.banned,
            updatedAt: r.updatedAt,
            details: byId.get(r.cardId),
          }));

          const details = this.items.map(x => x.details).filter(Boolean) as CubeCardDetails[];

this.frameTypeOptions = this.uniq(
  details.map(d => d.frameType).filter(Boolean) as string[]
);

this.attributeOptions = this.uniq(
  details.filter(d => this.isMonsterFrame(d.frameType))
         .map(d => d.attribute)
         .filter(Boolean) as string[]
);

this.monsterTypeOptions = this.uniq(
  details.filter(d => this.isMonsterFrame(d.frameType))
         .map(d => d.race)
         .filter(Boolean) as string[]
);

this.spellTypeOptions = this.uniq(
  details.filter(d => this.isSpellFrame(d.frameType))
         .map(d => d.race)
         .filter(Boolean) as string[]
);

this.trapTypeOptions = this.uniq(
  details.filter(d => this.isTrapFrame(d.frameType))
         .map(d => d.race)
         .filter(Boolean) as string[]
);

          this.applyFilters();

          // if we added from search, select it after refresh
          if (this.selectAfterLoadCardId != null) {
            const added = this.items.find(x => x.cardId === this.selectAfterLoadCardId);
            if (added) {
              this.select(added);
            }
            this.selectAfterLoadCardId = null;
          }

          // ---- selection sync for CardBrowser ----
          if (this.selectedCardId != null) {
            this.selected = this.items.find(x => x.cardId === this.selectedCardId) ?? null;
            if (this.selected) this.qtyDraft = this.selected.qty;
          }

          if (!this.selected && this.items.length > 0) {
            this.select(this.items[0]);
          }

          this.selectedCardId = this.selected?.cardId ?? null;

          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load collection.';
          this.loading = false;
        },
      });
    },
    error: () => {
      this.error = 'Failed to load card details.';
      this.loading = false;
    },
  });
}

select(item: CollectionItem) {
  this.selected = item;
  this.selectedCardId = item.cardId;
  this.qtyDraft = item.qty;
  this.selectedSearch = null;
}

  saveQty() {
    if (!this.selected) return;

    const qty = Number(this.qtyDraft ?? 0);
    if (qty < 0) {
      this.error = 'Qty cannot be negative.';
      return;
    }

    this.error = null;

    const cardId = this.selected.cardId;

    this.cubes.setCollectionQty(this.cubeId, this.activeUserId, cardId, qty).subscribe({
      next: (updated) => {
        // ✅ qty=0 => backend returns 204 => updated is null/undefined
        if (!updated) {
          this.items = this.items.filter(x => x.cardId !== cardId);
          this.selected = null;
          this.qtyDraft = null;
          return;
        }

        // normal update
        this.selected!.qty = updated.qty;
        this.selected!.updatedAt = updated.updatedAt;
        this.selected!.banned = updated.banned;

        const idx = this.items.findIndex(x => x.cardId === updated.cardId);
        if (idx >= 0) {
          this.items[idx] = {
            ...this.items[idx],
            qty: updated.qty,
            updatedAt: updated.updatedAt,
            banned: updated.banned,
          };
        }
      },
      error: (err) => {
        // if your backend returns an error body/message, show it
        this.error = err?.error ?? 'Failed to save qty.';
      },
    });
  }

  isAdmin(): boolean {
    return this.myRole === 'OWNER' || this.myRole === 'ADMIN';
  }

  canEditActiveUser(): boolean {
    return this.activeUserId === this.myUserId || this.isAdmin();
  }

switchUser(userId: number) {
  this.activeUserId = Number(userId);

  this.selected = null;
  this.selectedCardId = null; // ✅ important for CardBrowser highlight
  this.qtyDraft = null;

  this.selectedSearch = null;
  this.addQtyDraft = 1;

  this.load();
}

  loadMembersIfAdmin(cubeId: number) {
    if (!this.isAdmin() || !cubeId) return;

    this.membersLoading = true;
    this.cubes.listMembers(cubeId).subscribe({
      next: (rows) => {
        this.members = rows ?? [];
        this.membersLoading = false;
      },
      error: () => {
        this.membersLoading = false;
      },
    });
  }

  toggleAdminTools() {
    this.adminToolsOpen = !this.adminToolsOpen;
    if (!this.adminToolsOpen) {
      // optional: clear search when closing
      this.searchName = '';
      this.searchResults = [];
      this.searchTotalPages = 0;
      this.searchError = null;
      this.searching = false;
      this.selectedSearch = null;
      this.addQtyDraft = 1;
    }
  }

  onSearchNameChange(val: string) {
    this.searchName = val;
    const term = (val ?? '').trim();

    if (!term) {
      this.searchResults = [];
      this.searchTotalPages = 0;
      this.searchError = null;
      return;
    }

    if (term.length < 2) {
      this.searchResults = [];
      this.searchTotalPages = 0;
      this.searchError = 'Type at least 2 characters.';
      return;
    }

    this.searchError = null;
    this.search$.next(term);
  }

  runPoolSearch(term: string) {
    const trimmed = (term ?? '').trim();
    if (trimmed.length < 2) return;

    // admins only
    if (!this.isAdmin()) return;

    this.searching = true;
    this.searchError = null;

    this.cubes.searchCubePool(this.cubeId, trimmed, this.searchPage, this.searchSize).subscribe({
      next: (page: Page<CardSearchResult>) => {
        this.searchResults = page.content ?? [];
        this.searchTotalPages = page.totalPages ?? 0;
        this.searching = false;
      },
      error: (err) => {
        this.searchError = err?.error ?? 'Failed to search cube pool.';
        this.searching = false;
      }
    });
  }

  nextSearchPage() {
    if (this.searchPage + 1 >= this.searchTotalPages) return;
    this.searchPage++;
    this.runPoolSearch(this.searchName);
  }

  prevSearchPage() {
    if (this.searchPage <= 0) return;
    this.searchPage--;
    this.runPoolSearch(this.searchName);
  }

selectSearch(r: CardSearchResult) {
  this.selectedSearch = r;

  this.selected = null;
  this.selectedCardId = null; // ✅ de-highlight CardBrowser selection
  this.qtyDraft = null;

  this.addQtyDraft = 1;
}

  isSelectedSearch(id: number) {
    return this.selectedSearch?.id === id;
  }

  addSelectedSearchToCollection() {
    if (!this.isAdmin() || !this.selectedSearch) return;

    const qty = Number(this.addQtyDraft ?? 1);
    if (!Number.isFinite(qty) || qty < 1) {
      this.error = 'Qty must be >= 1.';
      return;
    }

    this.error = null;

    const cardId = this.selectedSearch.id;

    // set qty for ACTIVE user being viewed
    this.cubes.setCollectionQty(this.cubeId, this.activeUserId, cardId, qty).subscribe({
      next: () => {
        // after save, reload collection and select the new/updated item
        this.selectAfterLoadCardId = cardId;
        this.load();

        // optional: keep search selection, or clear it
        // this.selectedSearch = null;
      },
      error: (err) => {
        this.error = err?.error ?? 'Failed to add to collection.';
      }
    });
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

  let list = [...this.items];

  // Search
  if (f.q.trim()) list = list.filter(it => contains(it.details?.name, f.q.trim()));

  // Frame Type (primary)
  if (f.frameType) {
    list = list.filter(it => eqi(it.details?.frameType, f.frameType));
  }

  // Attribute (monsters only)
  if (f.attribute) {
    list = list.filter(it =>
      this.isMonsterFrame(it.details?.frameType) &&
      eqi(it.details?.attribute, f.attribute)
    );
  }

  // Race/Subtype (contextual: monster type OR spell type OR trap type)
  if (f.race) {
    list = list.filter(it => eqi(it.details?.race, f.race));
  }

  // Stats (only meaningful for monsters)
  const lvlActive = f.levelMin != null || f.levelMax != null;
  const atkActive = f.atkMin != null || f.atkMax != null;
  const defActive = f.defMin != null || f.defMax != null;

  const usingStats = lvlActive || atkActive || defActive;

  if (usingStats) {
    list = list.filter(it => this.isMonsterFrame(it.details?.frameType));

    if (lvlActive) list = list.filter(it => inRange((it.details as any)?.level, f.levelMin, f.levelMax));
    if (atkActive) list = list.filter(it => inRange((it.details as any)?.atk, f.atkMin, f.atkMax));
    if (defActive) list = list.filter(it => inRange((it.details as any)?.def, f.defMin, f.defMax));
  }

const dir = f.sortDir === 'asc' ? 1 : -1;

list.sort((a: CollectionItem, b: CollectionItem) => {
  const ad: Partial<CubeCardDetails> = a.details ?? {};
  const bd: Partial<CubeCardDetails> = b.details ?? {};

  // 1 frame type grouping
  const frameCompare =
    this.getFrameTypeOrder(ad.frameType) - this.getFrameTypeOrder(bd.frameType);

  if (frameCompare !== 0) return frameCompare;

  // 2 chosen sort key
  const av = f.sortKey === 'name' ? (ad.name ?? '') : (ad as any)[f.sortKey];
  const bv = f.sortKey === 'name' ? (bd.name ?? '') : (bd as any)[f.sortKey];

  const valueCompare = this.compareNullableValues(av, bv) * dir;
  if (valueCompare !== 0) return valueCompare;

  // 3 stable fallback
  return this.compareNullableValues(ad.name ?? '', bd.name ?? '');
});

  this.filteredItems = list;
}

  resetFilters() {
    this.filters = { ...DEFAULT_FILTERS };
    this.applyFilters();
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

  getSelectedDetailsCard(): CubeCardDetails | CardSearchResult | null {
  if (!this.selected) return null;

  if (this.selected.details) {
    return this.selected.details;
  }

  // fallback minimal object if details missing
  return {
    id: this.selected.cardId,
    name: `Card #${this.selected.cardId}`,
    imageUrl: null,
    cardType: null,
    humanReadableCardType: null,
    archetype: null,
  };
}

// ===== card-browser glue =====
selectedCardId: number | null = null;

// browser selectors (CollectionItem -> display)
colId = (it: CollectionItem) => it.cardId;

colName = (it: CollectionItem) => it.details?.name ?? (`Card #${it.cardId}`);

colImage = (it: CollectionItem) => it.details?.imageUrl ?? null;

colBadgeText = (it: CollectionItem) => it.banned ? 'BANNED' : `QTY ${it.qty}`;

colBadgeClass = (it: CollectionItem) => it.banned ? 'text-bg-danger' : 'text-bg-dark';

colRowStyle = (it: CollectionItem) => this.getRowStyleForCard(it.details ?? {});

colShowBannedIcon = (it: CollectionItem) => !!it.banned;

// keep RIGHT panel in sync
onCollectionSelectedIdChange(id: number | string | null) {
  this.selectedCardId = (id == null ? null : Number(id));
  this.selected = this.selectedCardId == null
    ? null
    : (this.items.find(x => x.cardId === this.selectedCardId) ?? null);

  if (this.selected) {
    this.qtyDraft = this.selected.qty;
    this.selectedSearch = null;
  }
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
