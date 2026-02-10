import { Component, OnDestroy, OnInit } from '@angular/core';
import { CubeContextService } from '../../../services/cube-context.service';
import { DraftApiService, DraftSession } from '../../../services/draft-api.service';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { interval, Subscription, switchMap, startWith, timer } from 'rxjs';
import { DraftPlayer } from '../../../models/draft-player';
import { MyPackResponse } from '../../../services/draft-api.service';
import { MyPackCard } from '../../../services/draft-api.service';

type CreateDraftForm = {
  draftSize: number; // cards per player
  packSize: number;
};

@Component({
  selector: 'app-cube-draft',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cube-draft.component.html',
  styleUrls: ['./cube-draft.component.css']
})
export class CubeDraftComponent implements OnInit, OnDestroy {
  private sub?: Subscription;
  private lobbyPollSub?: Subscription;
  private sessionsPollSub?: Subscription;
  private myPackPollSub?: Subscription;

  cubeId = 0;

  loading = false;
  error: string | null = null;
  success: string | null = null;

  sessions: DraftSession[] = [];
  isAdmin = false;

  joinedDraftId: number | null = null;

  lobbySession: DraftSession | null = null;
  lobbyPlayers: DraftPlayer[] = [];

  readying = false;
  starting = false;

  myUserId = 0;

  myPack: MyPackResponse | null = null;
  picking = false;
  private lastRoundNo: number | null = null;
  private wasWaiting: boolean = false;


  selectedPackCard: MyPackCard | null = null;
  selectedPackCardId: number | null = null;

  private lastPackId: number | null = null;

  myPicks: MyPackCard[] = [];
  draftComplete = false;
  summarySelected: MyPackCard | null = null;

  form: CreateDraftForm = {
    draftSize: 24,
    packSize: 10
  };

  constructor(
    private cubeCtx: CubeContextService,
    private drafts: DraftApiService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const snap = this.cubeCtx.snapshot();
    if (snap?.cubeId) {
      this.cubeId = snap.cubeId;
      this.isAdmin = ['OWNER', 'ADMIN'].includes((snap.myRole ?? '').toUpperCase().trim());
      this.myUserId = Number(snap.myUserId ?? 0);

      this.refresh();
      this.startSessionsPolling();
      return;
    }

    const id =
      this.route.snapshot.paramMap.get('cubeId')
      ?? this.route.parent?.snapshot.paramMap.get('cubeId')
      ?? this.route.parent?.parent?.snapshot.paramMap.get('cubeId');

    this.cubeId = Number(id);

    this.sub = this.cubeCtx.getCube().subscribe(cube => {
      if (!cube) return;

      if (!this.cubeId && cube.cubeId) {
        this.cubeId = cube.cubeId;
        this.refresh();
        this.startSessionsPolling();
      }

      this.isAdmin = ['OWNER', 'ADMIN'].includes((cube.myRole ?? '').toUpperCase().trim());
      this.myUserId = Number(cube.myUserId ?? 0);
    });

    if (!this.cubeId) {
      this.error = 'Cube not loaded yet. If this page was refreshed, navigate back to the cube and open Draft again.';
      return;
    }

    this.refresh();
    this.startSessionsPolling();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.lobbyPollSub?.unsubscribe();
    this.sessionsPollSub?.unsubscribe();
    this.stopMyPackPolling();
  }

  private startSessionsPolling(): void {
    this.sessionsPollSub?.unsubscribe();
    if (!this.cubeId) return;

    this.sessionsPollSub = interval(5000).pipe(
      startWith(0),
      switchMap(() => this.drafts.getOpenSessions(this.cubeId))
    ).subscribe({
      next: (list) => { this.sessions = list ?? []; },
      error: () => { /* quiet */ }
    });
  }

  refresh(): void {
    if (!this.cubeId) return;

    this.loading = true;
    this.error = null;
    this.success = null;

    this.drafts.getOpenSessions(this.cubeId).subscribe({
      next: (list) => {
        this.sessions = list ?? [];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to load draft sessions.';
      }
    });
  }

  createLobby(): void {
    if (!this.isAdmin || !this.cubeId) return;

    this.loading = true;
    this.error = null;
    this.success = null;

    // IMPORTANT: your DraftApiService.createLobby should accept only these fields now
    this.drafts.createLobby(this.cubeId, {
      draftSize: this.form.draftSize,
      packSize: this.form.packSize
    } as any).subscribe({
      next: (s) => {
        this.loading = false;
        this.success = `Draft lobby created (Session #${s.draftSessionId}).`;

        // ✅ auto-open lobby since creator is auto-joined
        this.openLobby(s.draftSessionId);

        this.refresh();
      },
      error: (err) => {
        this.loading = false;
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to create lobby.';
      }
    });
  }

  join(session: DraftSession): void {
    this.loading = true;
    this.error = null;
    this.success = null;

    this.drafts.join(session.draftSessionId).subscribe({
      next: () => {
        this.loading = false;
        this.success = `Joined draft session #${session.draftSessionId}.`;
        this.openLobby(session.draftSessionId);
      },
      error: (err) => {
        this.loading = false;
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to join session.';
      }
    });
  }

  cancel(session: DraftSession): void {
    if (!this.canCancel(session)) return;

    const msg = session.status === 'RUNNING'
      ? 'Cancel this running draft? This cannot be undone.'
      : 'Close this lobby? This cannot be undone.';

    if (!confirm(msg)) return;

    this.loading = true;
    this.error = null;
    this.success = null;

    this.drafts.cancel(session.draftSessionId).subscribe({
      next: () => {
        this.loading = false;
        this.success = `Closed session #${session.draftSessionId}.`;
        if (this.joinedDraftId === session.draftSessionId) {
          this.leaveLobbyView();
        }
        this.refresh();
      },
      error: (err) => {
        this.loading = false;
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to close session.';
      }
    });
  }

  canCancel(_s: DraftSession): boolean {
    return this.isAdmin;
  }

  isJoined(sessionId: number): boolean {
    return this.joinedDraftId === sessionId;
  }

  openLobby(draftId: number): void {
    this.joinedDraftId = draftId;
    this.sessionsPollSub?.unsubscribe();
    this.sessionsPollSub = undefined;
    this.startLobbyPolling(draftId);
  }

  leaveLobbyView(): void {
    this.joinedDraftId = null;
    this.lobbySession = null;
    this.lobbyPlayers = [];
    this.lobbyPollSub?.unsubscribe();
    this.lobbyPollSub = undefined;

    this.stopMyPackPolling();
    this.myPack = null;

    this.lastPackId = null;

    this.lastRoundNo = null;
    this.wasWaiting = false;

    this.clearPackSelection();
  }

  private startLobbyPolling(draftId: number): void {
    this.lobbyPollSub?.unsubscribe();

    this.lobbyPollSub = interval(750).pipe(
      startWith(0),
      switchMap(() => this.drafts.getState(draftId))
    ).subscribe({
      next: (state) => {
        this.lobbySession = state.session;
        this.lobbyPlayers = state.players ?? [];

        if (this.lobbySession?.status === 'RUNNING') {
          // only poll pack if we’re waiting (or pack not loaded yet)
          if (!this.myPack || this.isWaitingPack(this.myPack)) {
            this.startMyPackPolling();
          }
        } else {
          this.stopMyPackPolling();
          this.myPack = null;

          this.lastPackId = null;

          this.lastRoundNo = null;
          this.wasWaiting = false;

          this.clearPackSelection();
        }

        if (this.lobbySession && !['LOBBY', 'RUNNING'].includes(this.lobbySession.status as any)) {
          this.lobbyPollSub?.unsubscribe();
          this.lobbyPollSub = undefined;

          this.stopMyPackPolling();
          this.clearPackSelection();
          // keep lobbySession on screen so users can see final state
        }

        if (this.lobbySession?.status === 'RUNNING') {
          // load picks occasionally (or only after each pick)
          this.loadMyPicks();
        }

        if (this.lobbySession?.status === 'COMPLETED') {
          this.draftComplete = true;
          this.loadMyPicks(); // final refresh
        }
      },
      error: (err) => {
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to load lobby.';
        this.leaveLobbyView();
      }
    });
  }

  allReady(): boolean {
    return this.lobbyPlayers.length >= 2 && this.lobbyPlayers.every(p => p.ready);
  }

  canStart(): boolean {
    return this.isLobbyOwner() && this.lobbySession?.status === 'LOBBY' && this.allReady();
  }

  myPlayer(): DraftPlayer | null {
    if (!this.myUserId) return null;
    const myId = Number(this.myUserId);
    return this.lobbyPlayers.find(p => Number(p.userId) === myId) ?? null;
  }

  private loadMyPicks(): void {
    if (!this.joinedDraftId) return;

    this.drafts.getMyPicks(this.joinedDraftId).subscribe({
      next: (cards) => {
        this.myPicks = cards ?? [];
        // keep summary selection valid
        if (this.summarySelected) {
          const stillThere = this.myPicks.some(c => c.draftPackCardId === this.summarySelected!.draftPackCardId);
          if (!stillThere) this.summarySelected = null;
        }
      },
      error: () => { /* quiet */ }
    });
  }

  toggleReady(): void {
    if (!this.joinedDraftId) return;

    const me = this.myPlayer();
    if (!me) {
      this.error = 'Could not identify your player row.';
      return;
    }
    if (this.lobbySession?.status !== 'LOBBY') return;

    const nextReady = !me.ready;
    me.ready = nextReady; // optimistic

    this.readying = true;
    this.error = null;

    this.drafts.setReady(this.joinedDraftId, nextReady).subscribe({
      next: () => { this.readying = false; },
      error: (err) => {
        this.readying = false;
        me.ready = !nextReady;
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to update ready status.';
      }
    });
  }

  startDraft(): void {
    if (!this.joinedDraftId) return;
    if (!this.canStart()) return;

    if (!confirm('Start draft? This will generate packs and cannot be undone.')) return;

    this.starting = true;
    this.error = null;
    this.success = null;

    this.drafts.start(this.joinedDraftId).subscribe({
      next: () => {
        this.starting = false;
        this.success = 'Draft started.';
      },
      error: (err) => {
        this.starting = false;
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to start draft.';
      }
    });
  }

  private startMyPackPolling(): void {
    if (!this.joinedDraftId) return;
    if (this.myPackPollSub) return;

    const draftId = this.joinedDraftId;

    const poll = () => {
      // stop if draft not running / left view
      if (!this.joinedDraftId || this.lobbySession?.status !== 'RUNNING') {
        this.stopMyPackPolling();
        return;
      }

      this.myPackPollSub = this.drafts.getMyPack(draftId).subscribe({
        next: (pack) => {
          this.myPack = pack;

          const currentPackId = pack?.draftPackId ?? null;
          const currentRoundNo = pack?.roundNo ?? null;

          // clear selection on pack/round change
          if (this.lastPackId !== currentPackId || this.lastRoundNo !== currentRoundNo) {
            this.lastPackId = currentPackId;
            this.lastRoundNo = currentRoundNo;
            this.clearPackSelection();
          }

          // ✅ if we are STILL waiting, poll again
          if (this.isWaitingPack(pack)) {
            setTimeout(poll, 750);
            return;
          }

          // ✅ got a pack (or draft ended); stop polling to prevent blinking
          this.stopMyPackPolling();
        },
        error: () => {
          // backoff if errors
          setTimeout(poll, 1500);
        }
      });
    };

    poll();
  }

  private stopMyPackPolling(): void {
    this.myPackPollSub?.unsubscribe();
    this.myPackPollSub = undefined;
  }

  isPackSelected(draftPackCardId: number): boolean {
    return this.selectedPackCardId === draftPackCardId;
  }

  selectPackCard(c: MyPackCard): void {
    this.selectedPackCard = c;
    this.selectedPackCardId = c.draftPackCardId;
  }

  clearPackSelection(): void {
    this.selectedPackCard = null;
    this.selectedPackCardId = null;
  }

  lobbyOwnerName(): string {
    if (!this.lobbySession) return '';
    const ownerId = Number(this.lobbySession.createdByUserId);
    const p = this.lobbyPlayers.find(x => Number(x.userId) === ownerId);
    return p?.username ?? `User #${ownerId}`;
  }

  private isWaitingPack(p: MyPackResponse | null): boolean {
    // If you added pack.waiting, use that instead:
    // return !!p?.waiting;

    return !!p && (p.draftPackId == null) && !(p.cards?.length);
  }

  confirmPick(): void {
    if (!this.joinedDraftId) return;
    if (!this.selectedPackCardId) return;
    if (this.picking) return;
    if (!this.myPack?.draftPackId) return;

    this.picking = true;
    this.error = null;

    const pickId = this.selectedPackCardId;

    this.drafts.pick(this.joinedDraftId, pickId).subscribe({
      next: () => {
        this.picking = false;
        this.clearPackSelection();

        // freeze UI into waiting immediately to avoid "stale pack" confusion
        if (this.myPack) {
          this.myPack = { ...this.myPack, draftPackId: null, cards: [] };
        }

        this.loadMyPicks();

        // now poll until next pack arrives
        this.startMyPackPolling();
      },
      error: (err) => {
        console.log('PICK ERROR', err.status, err.error);
        this.picking = false;
        this.error = typeof err?.error === 'string' ? err.error : 'Failed to pick card.';
      }
    });
  }

  trackByPackCardId(_i: number, c: any): number {
    return c.draftPackCardId;
  }

  isLobbyOwner(): boolean {
    return !!this.lobbySession && Number(this.lobbySession.createdByUserId) === Number(this.myUserId);
  }

  isOwnerPlayer(p: DraftPlayer): boolean {
    return !!this.lobbySession && Number(p.userId) === Number(this.lobbySession.createdByUserId);
  }

  selectSummaryCard(c: MyPackCard): void {
    this.summarySelected = c;
  }

}
