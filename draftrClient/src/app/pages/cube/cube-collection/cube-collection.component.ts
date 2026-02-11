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
  imports: [CommonModule, FormsModule],
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

  cardTypeOptions: string[] = [];
  attributeOptions: string[] = [];
  raceOptions: string[] = [];

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

            this.cardTypeOptions = this.uniq(details.map(d => d.humanReadableCardType ?? d.cardType).filter(Boolean) as string[]);
            this.attributeOptions = this.uniq(details.map(d => d.attribute).filter(Boolean) as string[]);
            this.raceOptions = this.uniq(details.map(d => d.race).filter(Boolean) as string[]);

            this.applyFilters();

            if (this.selectAfterLoadCardId != null) {
              const added = this.items.find(x => x.cardId === this.selectAfterLoadCardId);
              if (added) {
                this.select(added);
              }
              this.selectAfterLoadCardId = null;
            }

            // keep/initialize selection
            if (this.selected) {
              const stillThere = this.items.find(x => x.cardId === this.selected!.cardId);
              this.selected = stillThere ?? null;
            }
            if (!this.selected && this.items.length > 0) {
              this.select(this.items[0]);
            }

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
    this.qtyDraft = item.qty;
    this.selectedSearch = null;
  }

  isSelected(cardId: number) {
    return this.selected?.cardId === cardId;
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
    this.qtyDraft = null;
    this.load();
    this.selectedSearch = null;
    this.addQtyDraft = 1;
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

  private setMembersUnique(rows: CubeMember[]) {
    const map = new Map<number, CubeMember>();
    for (const r of rows ?? []) map.set(r.userId, r);
    this.members = Array.from(map.values());
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
    this.selected = null;      // ✅ makes right panel show search card
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

    const inRange = (n: number | null | undefined, min: number | null, max: number | null) => {
      if (n == null) return false;
      if (min != null && n < min) return false;
      if (max != null && n > max) return false;
      return true;
    };

    let list = [...this.items];

    // name search
    if (f.q.trim()) list = list.filter(it => contains(it.details?.name, f.q.trim()));

    // type/attribute/race
    if (f.cardType) list = list.filter(it => (it.details?.humanReadableCardType ?? it.details?.cardType) === f.cardType);
    if (f.attribute) list = list.filter(it => it.details?.attribute === f.attribute);
    if (f.race) list = list.filter(it => it.details?.race === f.race);

    // numeric ranges
    const lvlActive = f.levelMin != null || f.levelMax != null;
    const atkActive = f.atkMin != null || f.atkMax != null;
    const defActive = f.defMin != null || f.defMax != null;

    if (lvlActive) list = list.filter(it => inRange((it.details as any)?.level, f.levelMin, f.levelMax));
    if (atkActive) list = list.filter(it => inRange((it.details as any)?.atk, f.atkMin, f.atkMax));
    if (defActive) list = list.filter(it => inRange((it.details as any)?.def, f.defMin, f.defMax));

    // sort
    const dir = f.sortDir === 'asc' ? 1 : -1;
    list.sort((a: any, b: any) => {
      const ad = a.details ?? {};
      const bd = b.details ?? {};

      const av = f.sortKey === 'name' ? (ad.name ?? '') : ad[f.sortKey];
      const bv = f.sortKey === 'name' ? (bd.name ?? '') : bd[f.sortKey];

      if (av == null && bv == null) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;

      if (typeof av === 'string') return av.localeCompare(String(bv)) * dir;
      return (Number(av) - Number(bv)) * dir;
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
