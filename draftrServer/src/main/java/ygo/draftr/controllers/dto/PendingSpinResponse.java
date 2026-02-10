package ygo.draftr.controllers.dto;

public class PendingSpinResponse {
    private boolean loserSpinAvailable;
    private boolean winnerSpinAvailable;
    private Long pendingRoundClaimId;

    public PendingSpinResponse(boolean loserSpinAvailable, boolean winnerSpinAvailable, Long pendingRoundClaimId) {
        this.loserSpinAvailable = loserSpinAvailable;
        this.winnerSpinAvailable = winnerSpinAvailable;
        this.pendingRoundClaimId = pendingRoundClaimId;
    }

    public boolean isLoserSpinAvailable() { return loserSpinAvailable; }
    public boolean isWinnerSpinAvailable() { return winnerSpinAvailable; }
    public Long getPendingRoundClaimId() { return pendingRoundClaimId; }
}