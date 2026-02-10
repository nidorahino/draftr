package ygo.draftr.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_pick",
        uniqueConstraints = {
                // prevents double-picking same step
                @UniqueConstraint(name = "uk_pick_session_round_pick_user",
                        columnNames = {"draft_session_id", "round_no", "pick_no", "user_id"}),
                // prevents same pool card being picked twice
                @UniqueConstraint(name = "uk_pick_session_pool",
                        columnNames = {"draft_session_id", "draft_pool_card_id"})
        })
public class DraftPick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_pick_id")
    private Long draftPickId;

    @Column(name = "draft_session_id", nullable = false)
    private Long draftSessionId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "pick_no", nullable = false)
    private Integer pickNo; // 0..packSize-1

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "draft_pack_id", nullable = false)
    private Long draftPackId;

    @Column(name = "draft_pool_card_id", nullable = false)
    private Long draftPoolCardId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "picked_at", nullable = false)
    private Instant pickedAt;

    // getters/setters

    public Long getDraftPickId() {
        return draftPickId;
    }

    public void setDraftPickId(Long draftPickId) {
        this.draftPickId = draftPickId;
    }

    public Long getDraftSessionId() {
        return draftSessionId;
    }

    public void setDraftSessionId(Long draftSessionId) {
        this.draftSessionId = draftSessionId;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getPickNo() {
        return pickNo;
    }

    public void setPickNo(Integer pickNo) {
        this.pickNo = pickNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDraftPackId() {
        return draftPackId;
    }

    public void setDraftPackId(Long draftPackId) {
        this.draftPackId = draftPackId;
    }

    public Long getDraftPoolCardId() {
        return draftPoolCardId;
    }

    public void setDraftPoolCardId(Long draftPoolCardId) {
        this.draftPoolCardId = draftPoolCardId;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Instant getPickedAt() {
        return pickedAt;
    }

    public void setPickedAt(Instant pickedAt) {
        this.pickedAt = pickedAt;
    }
}
