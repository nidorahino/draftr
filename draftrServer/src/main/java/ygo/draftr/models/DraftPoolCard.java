package ygo.draftr.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_pool_card",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_draft_pool_session_seq",
                        columnNames = {"draft_session_id", "seq"})
        })
public class DraftPoolCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_pool_card_id")
    private Long draftPoolCardId;

    @Column(name = "draft_session_id", nullable = false)
    private Long draftSessionId;

    @Column(name = "seq", nullable = false)
    private Integer seq; // 0..draftSize-1 (stable ordering)

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    // optional if you need traceability to a cube_card row
    @Column(name = "cube_card_id")
    private Long cubeCardId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // getters/setters

    public Long getDraftPoolCardId() {
        return draftPoolCardId;
    }

    public void setDraftPoolCardId(Long draftPoolCardId) {
        this.draftPoolCardId = draftPoolCardId;
    }

    public Long getDraftSessionId() {
        return draftSessionId;
    }

    public void setDraftSessionId(Long draftSessionId) {
        this.draftSessionId = draftSessionId;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Long getCubeCardId() {
        return cubeCardId;
    }

    public void setCubeCardId(Long cubeCardId) {
        this.cubeCardId = cubeCardId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
