import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { CubeService } from '../../../services/cube.service';
import { CubeMember } from '../../../models/cube-member';
import { CubeContextService } from '../../../services/cube-context.service';

@Component({
  selector: 'app-cube-members',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cube-members.component.html',
    styleUrls: ['./cube-members.component.css'],
})
export class CubeMembersComponent implements OnInit {
  cubeId = 0;

  members: CubeMember[] = [];
  loading = true;
  error: string | null = null;

  myRole: string | null = null;
  myUserId: number | null = null;

  addUsername = '';

  // Role editing
  editingRoleUserId: number | null = null;
  savingRoleForUserId: number | null = null;

  // Wins editing
  editingWinsUserId: number | null = null;
  savingWinsForUserId: number | null = null;

  // Claim loss UI
  showingClaimLoss = false;
  selectedWinnerUserId: number | null = null;
  claimingLoss = false;

  // Pending spins (new flow)
  loserSpinAvailable = false;
  winnerSpinAvailable = false;
  pendingRoundClaimId: number | null = null;

  checkingWheel = false;
  claimingLoserSpin = false;
  claimingWinnerSpin = false;

  // ===== Reward offer UI (winner for now, structured for reuse later) =====
  showingRewardOffer = false;
  rewardTitle = 'Winner Reward — Pick 2';
  rewardPickLimit = 2;

  rewardOfferIds: number[] = [];
  rewardOfferCards: any[] = []; // type to CubeCardDetails[] if you want
  rewardPickedIds = new Set<number>();
  rewardSelectedDetail: any | null = null;

  loadingRewardOffer = false;
  startingWinnerSpin = false;
  applyingReward = false;

  rewardMode: 'WINNER_PICK2' | 'LOSER_BAN' = 'WINNER_PICK2';

  constructor(
    private route: ActivatedRoute,
    private cubes: CubeService,
    private ctx: CubeContextService
  ) {}

  ngOnInit(): void {
    // role + myUserId from shell/context (adjust field if your CubeDetail uses a different name)
    this.ctx.getCube().subscribe((c) => {
      this.myRole = c?.myRole ?? null;
      this.myUserId = (c as any)?.myUserId ?? null;
    });

    // cubeId from parent route
    this.route.parent?.paramMap.subscribe((pm) => {
      this.cubeId = Number(pm.get('cubeId'));
      this.load();
    });
  }

  // ----- Role helpers
  isAdmin(): boolean {
    return this.myRole === 'OWNER' || this.myRole === 'ADMIN';
  }

  isOwner(): boolean {
    return this.myRole === 'OWNER';
  }

  // ----- Load
  load(): void {
    if (!this.cubeId) return;

    this.loading = true;
    this.error = null;

    this.cubes.listMembers(this.cubeId).subscribe({
      next: (rows) => {
        this.members = rows;
        this.loading = false;
        this.refreshPendingSpins();
      },
      error: () => {
        this.error = 'Failed to load members.';
        this.loading = false;
      },
    });
  }

  // ----- Members CRUD
  addMember(): void {
    if (!this.isAdmin()) return;

    const username = (this.addUsername ?? '').trim();
    if (!username) {
      this.error = 'Enter a username.';
      return;
    }

    this.error = null;

    this.cubes.joinOrAddMemberByUsername(this.cubeId, username).subscribe({
      next: () => {
        this.addUsername = '';
        this.load();
      },
      error: () => {
        this.error = 'Failed to add member.';
      },
    });
  }

  removeMember(targetUserId: number): void {
    if (!this.isAdmin()) return;

    if (!confirm(`Remove user ${targetUserId} from this cube?`)) return;

    this.error = null;

    this.cubes.removeMember(this.cubeId, targetUserId).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Failed to remove member.'),
    });
  }

  // ----- Role editing
  toggleRoleEditor(member: CubeMember): void {
    if (!this.isOwner()) return;
    if (member.role === 'OWNER') return;

    this.editingRoleUserId =
      this.editingRoleUserId === member.userId ? null : member.userId;
  }

  closeRoleEditor(): void {
    this.editingRoleUserId = null;
  }

  changeRole(targetUserId: number, newRole: string): void {
    if (!this.isOwner()) return;
    if (newRole !== 'ADMIN' && newRole !== 'MEMBER') return;

    this.error = null;
    this.savingRoleForUserId = targetUserId;

    this.cubes.updateMemberRole(this.cubeId, targetUserId, newRole).subscribe({
      next: () => {
        this.savingRoleForUserId = null;
        this.editingRoleUserId = null;
        this.load();
      },
      error: (err) => {
        this.savingRoleForUserId = null;
        this.editingRoleUserId = null;

        this.error =
          typeof err?.error === 'string' ? err.error : 'Failed to update role.';
        this.load();
      },
    });
  }

  // ----- Wins editing
  toggleWinsEditor(m: CubeMember): void {
    if (!this.isAdmin()) return;
    this.editingWinsUserId = this.editingWinsUserId === m.userId ? null : m.userId;
  }

  closeWinsEditor(): void {
    this.editingWinsUserId = null;
  }

  changeWins(userId: number, delta: number): void {
    if (!this.isAdmin()) return;

    this.error = null;
    this.savingWinsForUserId = userId;

    this.cubes.updateWins(this.cubeId, userId, delta).subscribe({
      next: () => {
        this.savingWinsForUserId = null;
        this.load();
      },
      error: (err) => {
        this.savingWinsForUserId = null;
        this.error =
          typeof err?.error === 'string' ? err.error : 'Failed to update wins.';
      },
    });
  }

  // ----- Claim Loss (creates pending round)
  canClaimLoss(): boolean {
    return !!this.myUserId && this.members.length > 1;
  }

  winnersForDropdown(): CubeMember[] {
    if (!this.myUserId) return this.members;
    return this.members.filter((m) => m.userId !== this.myUserId);
  }

  openClaimLoss(): void {
    this.showingClaimLoss = true;
    this.selectedWinnerUserId = null;
    this.error = null;
  }

  cancelClaimLoss(): void {
    this.showingClaimLoss = false;
    this.selectedWinnerUserId = null;
  }

  claimLoss(): void {
    if (!this.selectedWinnerUserId) {
      this.error = 'Select who won.';
      return;
    }

    this.claimingLoss = true;
    this.error = null;

    this.cubes.claimLoss(this.cubeId, this.selectedWinnerUserId).subscribe({
      next: () => {
        this.claimingLoss = false;
        this.cancelClaimLoss();
        this.load(); // refresh members + pending spins
      },
      error: (err) => {
        this.claimingLoss = false;
        this.error =
          typeof err?.error === 'string' ? err.error : 'Failed to claim loss.';
      },
    });
  }

  // ----- Pending spins
  refreshPendingSpins(): void {
    if (!this.cubeId) return;

    this.checkingWheel = true;

    this.cubes.getPendingSpins(this.cubeId).subscribe({
      next: (r) => {
        this.loserSpinAvailable = !!r?.loserSpinAvailable;
        this.winnerSpinAvailable = !!r?.winnerSpinAvailable;
        this.pendingRoundClaimId = r?.pendingRoundClaimId ?? null;
        this.checkingWheel = false;
      },
      error: () => {
        this.loserSpinAvailable = false;
        this.winnerSpinAvailable = false;
        this.pendingRoundClaimId = null;
        this.checkingWheel = false;
      },
    });
  }

  claimLoserSpin(): void {
    this.claimingLoserSpin = true;
    this.error = null;

    this.cubes.claimLoserSpin(this.cubeId).subscribe({
      next: () => {
        this.claimingLoserSpin = false;
        this.load(); // win may apply if winner also claimed
      },
      error: (err) => {
        this.claimingLoserSpin = false;
        this.error =
          typeof err?.error === 'string'
            ? err.error
            : 'Failed to claim loser spin.';
      },
    });
  }

  openWinnerSpin(): void {
    this.rewardMode = 'WINNER_PICK2';
    this.error = null;
    this.startingWinnerSpin = true;

    this.cubes.startWinnerSpin(this.cubeId).subscribe({
      next: (resp) => {
        this.startingWinnerSpin = false;

        const ids = resp?.offeredCardIds ?? [];

        // configure reward UI (winner pick-2)
        this.rewardTitle = 'Winner Reward — Pick 2';
        this.rewardPickLimit = 2;

        // reset + open
        this.rewardOfferIds = ids;
        this.rewardOfferCards = [];
        this.rewardPickedIds.clear();
        this.rewardSelectedDetail = null;

        this.showingRewardOffer = true;

        // load details to show images + details panel
        this.loadRewardOfferDetails();
      },
      error: (err) => {
        this.startingWinnerSpin = false;
        this.error =
          typeof err?.error === 'string'
            ? err.error
            : 'Failed to start winner spin.';
      },
    });
  }

  private loadRewardOfferDetails(): void {
    this.loadingRewardOffer = true;

    // offer excludes banned, but includeBanned=true is fine
    this.cubes.listCardsDetails(this.cubeId, true).subscribe({
      next: (all) => {
        const byId = new Map<number, any>();
        for (const c of all ?? []) byId.set(c.cardId, c);

        this.rewardOfferCards = (this.rewardOfferIds ?? [])
          .map((id) => byId.get(id))
          .filter(Boolean);

        this.rewardSelectedDetail =
          this.rewardOfferCards.length > 0 ? this.rewardOfferCards[0] : null;

        this.loadingRewardOffer = false;
      },
      error: () => {
        this.rewardOfferCards = [];
        this.rewardSelectedDetail = null;
        this.loadingRewardOffer = false;
        this.error = 'Failed to load offer card details.';
      },
    });
  }

  selectRewardDetail(card: any): void {
    this.rewardSelectedDetail = card;
  }

  isPicked(cardId: number): boolean {
    return this.rewardPickedIds.has(cardId);
  }

togglePick(cardId: number): void {
  if (this.applyingReward) return;

  if (this.rewardPickedIds.has(cardId)) {
    this.rewardPickedIds.delete(cardId);
    return;
  }

  // enforce max picks
  if (this.rewardPickedIds.size >= this.maxPicks()) return;

  this.rewardPickedIds.add(cardId);
}

confirmReward(): void {
  const count = this.rewardPickedIds.size;

  // Validate picks based on mode
  if (this.rewardMode === 'LOSER_BAN') {
    // loser: pick 1..rewardPickLimit (<= 8)
    if (count < 1 || count > this.rewardPickLimit) {
      this.error = `Pick between 1 and ${this.rewardPickLimit} cards.`;
      return;
    }
  } else {
    // winner: must pick exactly rewardPickLimit (2)
    if (count !== this.rewardPickLimit) {
      this.error = `Pick exactly ${this.rewardPickLimit} cards.`;
      return;
    }
  }

  const selected = Array.from(this.rewardPickedIds);

  this.error = null;
  this.applyingReward = true;

  const done = () => {
    this.applyingReward = false;
    this.closeRewardOffer();
    this.load();
  };

  const fail = (err: any) => {
    this.applyingReward = false;
    this.error =
      typeof err?.error === 'string' ? err.error : 'Failed to apply reward.';
  };

  if (this.rewardMode === 'WINNER_PICK2') {
    this.cubes.applyWinnerSpin(this.cubeId, selected).subscribe({
      next: done,
      error: fail,
    });
  } else {
    this.cubes.applyLoserSpin(this.cubeId, selected).subscribe({
      next: (_r) => {
        // optional: show a quick message
        // this.successMessage = `Banned card #${_r?.bannedCardId}`;
        done();
      },
      error: fail,
    });
  }
}

  closeRewardOffer(): void {
    this.showingRewardOffer = false;
    this.rewardOfferIds = [];
    this.rewardOfferCards = [];
    this.rewardPickedIds.clear();
    this.rewardSelectedDetail = null;
  }

  openLoserSpin(): void {
    this.rewardMode = 'LOSER_BAN';
    this.error = null;
    this.loadingRewardOffer = true;

    this.cubes.startLoserSpin(this.cubeId).subscribe({
      next: (resp) => {
        const ids = resp?.offeredCardIds ?? [];

        this.rewardTitle = 'Loser Wheel — Pick up to 8 (1 will be banned at random)';
        this.rewardPickLimit = Math.min(8, ids.length);

        this.rewardOfferIds = ids;
        this.rewardOfferCards = [];
        this.rewardPickedIds.clear();
        this.rewardSelectedDetail = null;

        this.showingRewardOffer = true;

        // load card details for images + right panel (same as winner)
        this.loadRewardOfferDetails();

        this.loadingRewardOffer = false;
      },
      error: (err) => {
        this.loadingRewardOffer = false;
        this.error =
          typeof err?.error === 'string'
            ? err.error
            : 'Failed to start loser spin.';
      },
    });
  }

  private minPicks(): number {
  return this.rewardMode === 'LOSER_BAN' ? 1 : this.rewardPickLimit;
}

private maxPicks(): number {
  return this.rewardPickLimit;
}

canConfirmReward(): boolean {
  if (this.rewardMode === 'LOSER_BAN') {
    return this.rewardPickedIds.size >= 1 && this.rewardPickedIds.size <= this.rewardPickLimit;
  }
  return this.rewardPickedIds.size === this.rewardPickLimit;
}
}
