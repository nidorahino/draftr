package ygo.draftr.controllers.dto;

import java.util.List;

public class ApplyWinnerPickResponse {
    private List<Long> addedCardIds;

    public ApplyWinnerPickResponse() { }

    public ApplyWinnerPickResponse(List<Long> addedCardIds) {
        this.addedCardIds = addedCardIds;
    }

    public List<Long> getAddedCardIds() { return addedCardIds; }
    public void setAddedCardIds(List<Long> addedCardIds) { this.addedCardIds = addedCardIds; }
}
