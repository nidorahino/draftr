package ygo.draftr.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_pack",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_draft_pack_session_round_owner",
                        columnNames = {"draft_session_id", "round_no", "initial_owner_seat_no"})
        })
public class DraftPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_pack_id")
    private Long draftPackId;

    @Column(name = "draft_session_id", nullable = false)
    private Long draftSessionId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo; // 1..packsPerPlayer

    @Column(name = "initial_owner_seat_no", nullable = false)
    private Integer initialOwnerSeatNo;

    @Column(name = "current_owner_seat_no", nullable = false)
    private Integer currentOwnerSeatNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private PassDirection direction; // LEFT, RIGHT

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // getters/setters

    public Long getDraftPackId() {
        return draftPackId;
    }

    public void setDraftPackId(Long draftPackId) {
        this.draftPackId = draftPackId;
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

    public Integer getInitialOwnerSeatNo() {
        return initialOwnerSeatNo;
    }

    public void setInitialOwnerSeatNo(Integer initialOwnerSeatNo) {
        this.initialOwnerSeatNo = initialOwnerSeatNo;
    }

    public Integer getCurrentOwnerSeatNo() {
        return currentOwnerSeatNo;
    }

    public void setCurrentOwnerSeatNo(Integer currentOwnerSeatNo) {
        this.currentOwnerSeatNo = currentOwnerSeatNo;
    }

    public PassDirection getDirection() {
        return direction;
    }

    public void setDirection(PassDirection direction) {
        this.direction = direction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
