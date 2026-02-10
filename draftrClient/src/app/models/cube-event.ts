export interface CubeEvent {
  cubeEventId: number;
  cubeId: number;
  eventType: string;
  actorUserId: number;
  createdAt: string;
  summary: string;
  payload?: string;
  cardId?: number;
  targetUserId?: number;
}
