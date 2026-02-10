export interface CardSearchResult {
  id: number;
  name: string;
  cardType?: string | null;
  humanReadableCardType?: string | null;
  archetype?: string | null;
  imageUrl?: string | null;
}