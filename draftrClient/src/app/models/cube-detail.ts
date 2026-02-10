export interface CubeDetail {
  cubeId: number;
  name: string;
  ownerUserId: number;
  maxPlayers: number;
  createdAt: string;
  myRole: string; // OWNER | ADMIN | MEMBER
  myUserId: number;
}
