package ygo.draftr.controllers.dto;

import java.time.Instant;

public class CubeCollectionCardResponse {

    private Long cubeId;
    private Long userId;
    private Long cardId;
    private int qty;
    private Instant updatedAt;
    private boolean banned; // derived from CubeCard

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
}
