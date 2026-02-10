package ygo.draftr.controllers.dto;

public class CollectionQtyRequest {
    private Long cardId;
    private int qty;

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
}
