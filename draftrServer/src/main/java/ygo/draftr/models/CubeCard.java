package ygo.draftr.models;

import jakarta.persistence.*;

@Entity
@Table(
        name = "cube_card",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cube_id", "card_id"})
)
public class CubeCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cube_card_id")
    private Long cubeCardId;

    @Column(name = "cube_id", nullable = false)
    private Long cubeId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private int maxQty = 1;

    @Column(nullable = false)
    private boolean banned = false;

    public Long getCubeCardId() { return cubeCardId; }
    public void setCubeCardId(Long cubeCardId) { this.cubeCardId = cubeCardId; }

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public int getMaxQty() { return maxQty; }
    public void setMaxQty(int maxQty) { this.maxQty = maxQty; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
}
