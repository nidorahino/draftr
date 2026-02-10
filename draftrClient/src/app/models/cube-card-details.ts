export interface CubeCardDetails {
  cubeId: number;
  cardId: number;
  maxQty: number;
  banned: boolean;

  name?: string;
  cardType?: string;
  humanReadableCardType?: string;
  frameType?: string;
  race?: string;
  archetype?: string;
  typeline?: string;
  attribute?: string;
  level?: number;
  atk?: number;
  def?: number;
  imageUrl?: string;
  description?: string;
}
