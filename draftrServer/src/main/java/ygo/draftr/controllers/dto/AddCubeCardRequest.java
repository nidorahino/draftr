package ygo.draftr.controllers.dto;

public class AddCubeCardRequest {
    private Long cardId;
    private int maxQty = 1;

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public int getMaxQty() { return maxQty; }
    public void setMaxQty(int maxQty) { this.maxQty = maxQty; }
}
