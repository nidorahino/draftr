package ygo.draftr.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "round_claim")
public class RoundClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_claim_id")
    private Long roundClaimId;

    @Column(name = "cube_id", nullable = false)
    private Long cubeId;

    @Column(name = "winner_user_id", nullable = false)
    private Long winnerUserId;

    @Column(name = "loser_user_id", nullable = false)
    private Long loserUserId;

    @Column(name = "loser_spin_claimed", nullable = false)
    private boolean loserSpinClaimed = false;

    @Column(name = "winner_spin_claimed", nullable = false)
    private boolean winnerSpinClaimed = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "applied_at")
    private Instant appliedAt;

    // getters/setters

    public Long getRoundClaimId() { return roundClaimId; }
    public void setRoundClaimId(Long roundClaimId) { this.roundClaimId = roundClaimId; }

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public Long getWinnerUserId() { return winnerUserId; }
    public void setWinnerUserId(Long winnerUserId) { this.winnerUserId = winnerUserId; }

    public Long getLoserUserId() { return loserUserId; }
    public void setLoserUserId(Long loserUserId) { this.loserUserId = loserUserId; }

    public boolean isLoserSpinClaimed() { return loserSpinClaimed; }
    public void setLoserSpinClaimed(boolean loserSpinClaimed) { this.loserSpinClaimed = loserSpinClaimed; }

    public boolean isWinnerSpinClaimed() { return winnerSpinClaimed; }
    public void setWinnerSpinClaimed(boolean winnerSpinClaimed) { this.winnerSpinClaimed = winnerSpinClaimed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
}