export interface CubeMember {
  cubeId: number;
  userId: number;
  username?: string;
  role: string;     // OWNER | ADMIN | MEMBER
  joinedAt: string; // ISO date from backend
  wins: number;
}
