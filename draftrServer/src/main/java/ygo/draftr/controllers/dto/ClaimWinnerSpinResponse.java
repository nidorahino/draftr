package ygo.draftr.controllers.dto;

import java.util.List;

public class ClaimWinnerSpinResponse {

    private List<Long> winnerCardsAdded;

    public ClaimWinnerSpinResponse(List<Long> winnerCardsAdded) {
        this.winnerCardsAdded = winnerCardsAdded;
    }

    public List<Long> getWinnerCardsAdded() {
        return winnerCardsAdded;
    }
}
