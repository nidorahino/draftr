package ygo.draftr.controllers.dto;

import java.util.List;

public class ApplyLoserPickRequest {
    private List<Long> selectedCardIds;

    public List<Long> getSelectedCardIds() { return selectedCardIds; }
    public void setSelectedCardIds(List<Long> selectedCardIds) { this.selectedCardIds = selectedCardIds; }
}
