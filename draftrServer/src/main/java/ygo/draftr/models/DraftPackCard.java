package ygo.draftr.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_pack_card",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pack_slot",
                        columnNames = {"draft_pack_id", "slot_no"}),
                @UniqueConstraint(name = "uk_pack_pool_card",
                        columnNames = {"draft_pack_id", "draft_pool_card_id"})
        })
public class DraftPackCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_pack_card_id")
    private Long draftPackCardId;

    @Column(name = "draft_pack_id", nullable = false)
    private Long draftPackId;

    @Column(name = "slot_no", nullable = false)
    private Integer slotNo; // 0..packSize-1

    @Column(name = "draft_pool_card_id", nullable = false)
    private Long draftPoolCardId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    // when picked, mark removed instead of deleting
    @Column(name = "picked_by_user_id")
    private Long pickedByUserId;

    @Column(name = "picked_at")
    private Instant pickedAt;

    // getters/setters

    public Long getDraftPackCardId() {
        return draftPackCardId;
    }

    public void setDraftPackCardId(Long draftPackCardId) {
        this.draftPackCardId = draftPackCardId;
    }

    public Long getDraftPackId() {
        return draftPackId;
    }

    public void setDraftPackId(Long draftPackId) {
        this.draftPackId = draftPackId;
    }

    public Integer getSlotNo() {
        return slotNo;
    }

    public void setSlotNo(Integer slotNo) {
        this.slotNo = slotNo;
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

    public Long getPickedByUserId() {
        return pickedByUserId;
    }

    public void setPickedByUserId(Long pickedByUserId) {
        this.pickedByUserId = pickedByUserId;
    }

    public Instant getPickedAt() {
        return pickedAt;
    }

    public void setPickedAt(Instant pickedAt) {
        this.pickedAt = pickedAt;
    }
}
