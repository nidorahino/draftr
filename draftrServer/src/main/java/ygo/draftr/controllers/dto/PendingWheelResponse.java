package ygo.draftr.controllers.dto;

public class PendingWheelResponse {
    private boolean winnerSpinAvailable;

    public PendingWheelResponse(boolean winnerSpinAvailable) {
        this.winnerSpinAvailable = winnerSpinAvailable;
    }

    public boolean isWinnerSpinAvailable() { return winnerSpinAvailable; }
}
