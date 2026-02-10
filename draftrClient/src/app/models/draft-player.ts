export interface DraftPlayer {
  draftPlayerId: number;
  draftSessionId: number;
  userId: number;
  seatNo: number;
  ready: boolean;
  joinedAt: string;
  username: string;
}