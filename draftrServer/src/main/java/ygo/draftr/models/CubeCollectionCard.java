package ygo.draftr.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "cube_collection_card",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cube_id", "user_id", "card_id"})
)
public class CubeCollectionCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cube_collection_card_id")
    private Long cubeCollectionCardId;

    @Column(name = "cube_id", nullable = false)
    private Long cubeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private int qty;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getCubeCollectionCardId() { return cubeCollectionCardId; }
    public void setCubeCollectionCardId(Long cubeCollectionCardId) { this.cubeCollectionCardId = cubeCollectionCardId; }

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
}
