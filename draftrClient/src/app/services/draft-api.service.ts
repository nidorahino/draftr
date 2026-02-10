import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DraftPlayer } from '../models/draft-player';
import { environment } from '../../environments/environment';

export type DraftStatus = 'LOBBY' | 'RUNNING' | 'COMPLETED' | 'CANCELLED';

export interface DraftSession {
  draftSessionId: number;
  cubeId: number;
  createdByUserId: number;
  status: DraftStatus;

  draftSize: number;
  packSize: number;

  currentWave?: number;
  currentPickNo?: number;

  createdAt: string;
  startedAt?: string | null;
  endedAt?: string | null;
}

export interface CreateDraftRequest {
  // cards PER PLAYER
  draftSize: number;
  packSize: number;
}

export interface MyPackCard {
  draftPackCardId: number;
  cardId: number;
  slotNo: number;

  name?: string;
  imageUrl?: string;
  humanReadableCardType?: string;
  description?: string;
  atk?: number;
  def?: number;
  level?: number;
}

export interface MyPackResponse {
  draftPackId: number | null;
  roundNo: number;
  direction: string | null;
  cards: MyPackCard[];
  waiting?: boolean;
}


@Injectable({ providedIn: 'root' })
export class DraftApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  constructor(private http: HttpClient) {}

  getOpenSessions(cubeId: number): Observable<DraftSession[]> {
    return this.http.get<DraftSession[]>(`${this.baseUrl}/cubes/${cubeId}/drafts/open`);
  }

  createLobby(cubeId: number, req: CreateDraftRequest): Observable<DraftSession> {
    return this.http.post<DraftSession>(`${this.baseUrl}/cubes/${cubeId}/drafts`, req);
  }

  join(draftId: number): Observable<DraftPlayer> {
    return this.http.post<DraftPlayer>(`${this.baseUrl}/drafts/${draftId}/join`, {});
  }

  cancel(draftId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/drafts/${draftId}/cancel`, {});
  }

  getState(draftId: number): Observable<{ session: DraftSession; players: DraftPlayer[] }> {
    return this.http.get<{ session: DraftSession; players: DraftPlayer[] }>(
      `${this.baseUrl}/drafts/${draftId}`
    );
  }

  setReady(draftId: number, ready: boolean): Observable<DraftPlayer> {
    return this.http.post<DraftPlayer>(`${this.baseUrl}/drafts/${draftId}/ready`, { ready });
  }

  start(draftId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/drafts/${draftId}/start`, {});
  }

  getMyPack(draftId: number): Observable<MyPackResponse> {
    return this.http.get<MyPackResponse>(`${this.baseUrl}/drafts/${draftId}/me/pack`);
  }

  pick(draftId: number, draftPackCardId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/drafts/${draftId}/pick`, { draftPackCardId });
  }

  getMyPicks(draftId: number): Observable<MyPackCard[]> {
    return this.http.get<MyPackCard[]>(`${this.baseUrl}/drafts/${draftId}/me/picks`);
  }

}
