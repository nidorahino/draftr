import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CubeSummary } from '../models/cube-summary';
import { CubeDetail } from '../models/cube-detail';
import { CubeCardDetails } from '../models/cube-card-details';
import { CubeMember } from '../models/cube-member';
import { CubeEvent } from '../models/cube-event';
import { Page } from '../models/page';
import { CubeCollectionCard } from '../models/cube-collection-card';
import { CardSearchResult } from '../models/card-search-result';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CubeService {
  private readonly API = `${environment.apiBaseUrl}/api/cubes`;

  constructor(private http: HttpClient) {}

  searchCardsByName(name: string, page = 0, size = 10) {
    const params = new HttpParams()
      .set('name', name)
      .set('page', page)
      .set('size', size);

    return this.http.get<Page<CardSearchResult>>(
      `${environment.apiBaseUrl}/api/cards/search`,
      { params }
    );
  }

  // ---- Cubes
  listMyCubes() {
    return this.http.get<CubeSummary[]>(this.API);
  }

  getCube(cubeId: number) {
    return this.http.get<CubeDetail>(`${this.API}/${cubeId}`);
  }

  updateCube(cubeId: number, req: { name?: string; maxPlayers?: number }) {
    return this.http.put<any>(`/api/cubes/${cubeId}`, req);
  }

  createCube(req: { name: string; maxPlayers: number }) {
    return this.http.post('/api/cubes', req);
  }

  // ---- Cards (read)
  listCardsDetails(cubeId: number, includeBanned = true) {
    return this.http.get<CubeCardDetails[]>(
      `${this.API}/${cubeId}/cards/details`,
      { params: { includeBanned } as any }
    );
  }

  // ---- Cards (admin actions)
  addCard(cubeId: number, cardId: number, maxQty: number) {
    return this.http.post(`${this.API}/${cubeId}/cards`, { cardId, maxQty });
  }

  updateMaxQty(cubeId: number, cardId: number, maxQty: number) {
    return this.http.put(`${this.API}/${cubeId}/cards/${cardId}`, { maxQty });
  }

  removeCard(cubeId: number, cardId: number) {
    return this.http.delete(`${this.API}/${cubeId}/cards/${cardId}`);
  }

  setBanned(cubeId: number, cardId: number, banned: boolean, reason?: string) {
    return this.http.put(`${this.API}/${cubeId}/cards/${cardId}/ban`, {
      banned,
      reason: reason ?? null,
    });
  }

  searchCubePool(cubeId: number, name: string, page = 0, size = 10) {
    const params = new HttpParams()
      .set('name', name)
      .set('page', page)
      .set('size', size);

    return this.http.get<Page<CardSearchResult>>(
      `${this.API}/${cubeId}/cards/pool-search`,
      { params }
    );
  }

  // ---- Members
  listMembers(cubeId: number) {
    return this.http.get<CubeMember[]>(`${this.API}/${cubeId}/members`);
  }

  joinOrAddMemberByUsername(cubeId: number, username: string) {
    return this.http.post(`${this.API}/${cubeId}/members`, { username });
  }

  removeMember(cubeId: number, targetUserId: number) {
    return this.http.delete(`${this.API}/${cubeId}/members/${targetUserId}`);
  }

  archiveCube(cubeId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${cubeId}`);
  }

  leaveCube(cubeId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${cubeId}/members/me`);
  }

  updateMemberRole(cubeId: number, targetUserId: number, role: string) {
    return this.http.patch(
      `${this.API}/${cubeId}/members/${targetUserId}/role`,
      { role }
    );
  }

  updateWins(cubeId: number, userId: number, delta: number) {
    return this.http.patch(
      `${this.API}/${cubeId}/members/${userId}/wins`,
      { delta }
    );
  }


  // ---- Audit log
  getEvents(cubeId: number, page = 0, size = 25) {
    return this.http.get<Page<CubeEvent>>(`${this.API}/${cubeId}/events`, {
      params: { page, size } as any,
    });
  }

  // ---- Collections (per-user, per-cube)
  getCollection(cubeId: number, userId: number) {
    return this.http.get<CubeCollectionCard[]>(
      `${this.API}/${cubeId}/users/${userId}/collection`
    );
  }

  setCollectionQty(cubeId: number, userId: number, cardId: number, qty: number) {
    return this.http.put<CubeCollectionCard | null>(
      `${this.API}/${cubeId}/users/${userId}/collection`,
      { cardId, qty }
    );
  }

  // ---- Round claim / wheels (new)
  getPendingSpins(cubeId: number) {
    return this.http.get<{
      loserSpinAvailable: boolean;
      winnerSpinAvailable: boolean;
      pendingRoundClaimId: number | null;
    }>(`${this.API}/${cubeId}/wheels/pending`);
  }

  claimLoss(cubeId: number, winnerUserId: number) {
    return this.http.post<number>(`${this.API}/${cubeId}/rounds/claim-loss`, { winnerUserId });
  }

  claimLoserSpin(cubeId: number) {
    return this.http.post<void>(`${this.API}/${cubeId}/wheels/claim-loser`, {});
  }

  claimWinnerSpin(cubeId: number) {
    return this.http.post<void>(`${this.API}/${cubeId}/wheels/claim-winner`, {});
  }

  // Winner spin: start (returns offer)
  startWinnerSpin(cubeId: number) {
    return this.http.post<{ roundClaimId: number; offeredCardIds: number[] }>(
      `${this.API}/${cubeId}/wheels/claim-winner`,
      {}
    );
  }

  // Winner spin: apply picks
  applyWinnerSpin(cubeId: number, selectedCardIds: number[]) {
    return this.http.post<void>(
      `${this.API}/${cubeId}/wheels/claim-winner/apply`,
      { selectedCardIds }
    );
  }

  startLoserSpin(cubeId: number) {
    return this.http.post<{ roundClaimId: number; opponentUserId: number; offeredCardIds: number[] }>(
      `${this.API}/${cubeId}/wheels/claim-loser`,
      {}
    );
  }

  applyLoserSpin(cubeId: number, selectedCardIds: number[]) {
    return this.http.post<{ bannedCardId: number }>(
      `${this.API}/${cubeId}/wheels/claim-loser/apply`,
      { selectedCardIds }
    );
  }
}
