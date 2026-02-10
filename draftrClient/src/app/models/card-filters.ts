export type SortKey = 'name' | 'atk' | 'def' | 'level';
export type SortDir = 'asc' | 'desc';

export interface CardFilters {
  q: string;                // name contains
  cardType: string;         // e.g. "Monster", "Spell", "Trap" or your own cardType values
  frameType: string;        // optional if you use frameType
  attribute: string;        // e.g. "DARK"
  race: string;             // monster type like "Dragon"
  levelMin: number | null;
  levelMax: number | null;
  atkMin: number | null;
  atkMax: number | null;
  defMin: number | null;
  defMax: number | null;
  sortKey: SortKey;
  sortDir: SortDir;
  includeBanned?: boolean;  // (pool only usually)
}

export const DEFAULT_FILTERS: CardFilters = {
  q: '',
  cardType: '',
  frameType: '',
  attribute: '',
  race: '',
  levelMin: null,
  levelMax: null,
  atkMin: null,
  atkMax: null,
  defMin: null,
  defMax: null,
  sortKey: 'name',
  sortDir: 'asc',
};
