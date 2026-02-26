import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type CardBrowserViewMode = 'list' | 'image';

@Component({
  selector: 'app-card-browser',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card-browser.component.html',
  styleUrls: ['./card-browser.component.css']
})
export class CardBrowserComponent<TItem> {
  // Header
  @Input() title = 'Cards';
  @Input() subtitle?: string;

  // State
  @Input() loading = false;
  @Input() error: string | null = null;
  @Input() emptyText = 'No items.';

  // Data
  @Input() items: TItem[] = [];

  // View mode (two-way)
  @Input() viewMode: CardBrowserViewMode = 'list';
  @Output() viewModeChange = new EventEmitter<CardBrowserViewMode>();

  // Selection
  @Input() selectedId: number | string | null = null;
  @Output() selectedIdChange = new EventEmitter<number | string | null>();

  // Field selectors (tell the component how to read your item shape)
  @Input({ required: true }) getId!: (it: TItem) => number | string;
  @Input() getName: (it: TItem) => string = (it: any) => it?.name ?? '';
  @Input() getImageUrl: (it: TItem) => string | null = (it: any) => it?.imageUrl ?? null;
  @Input() getBadgeText: (it: TItem) => string | null = () => null;
  @Input() getBadgeClass: (it: TItem) => string | null = () => null;
  @Input() getSubtext: (it: TItem) => string | null = () => null;

  // Style hooks
  @Input() getRowStyle: (it: TItem) => Record<string, any> | null = () => null;
  @Input() showBannedIcon: (it: TItem) => boolean = (it: any) => !!it?.banned;

  // Layout knobs
  @Input() gridColsClass = 'col-6 col-sm-4 col-md-3 col-lg-2';

  setMode(mode: CardBrowserViewMode) {
    if (this.viewMode !== mode) {
      this.viewMode = mode;
      this.viewModeChange.emit(mode);
    }
  }

  isSelected(it: TItem): boolean {
    const id = this.getId(it);
    return this.selectedId !== null && String(this.selectedId) === String(id);
  }

  select(it: TItem) {
    const id = this.getId(it);
    this.selectedId = id;
    this.selectedIdChange.emit(id);
  }
}