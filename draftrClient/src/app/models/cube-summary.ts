export interface CubeSummary {
  cubeId: number;
  name: string;
  maxPlayers: number;
  role: string;
  createdAt: string;

  memberCount?: number;

  players?: Array<{
    userId: number;
    name: string;
    score?: number;
    isYou?: boolean;
  }>;

  myScore?: number;
  theirScore?: number;

  updatedAt?: string;
}
