package ygo.draftr.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_player",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_draft_player_session_user",
                        columnNames = {"draft_session_id", "user_id"}),
                @UniqueConstraint(name = "uk_draft_player_session_seat",
                        columnNames = {"draft_session_id", "seat_no"})
        })
public class DraftPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_player_id")
    private Long draftPlayerId;

    @Column(name = "draft_session_id", nullable = false)
    private Long draftSessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "seat_no", nullable = false)
    private Integer seatNo; // 0..N-1

    @Column(name = "is_ready", nullable = false)
    private boolean ready;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "last_wave")
    private int lastWave;

    @Column(name = "last_pick_no")
    private int lastPickNo;

    // getters/setters

    public Long getDraftPlayerId() {
        return draftPlayerId;
    }

    public void setDraftPlayerId(Long draftPlayerId) {
        this.draftPlayerId = draftPlayerId;
    }

    public Long getDraftSessionId() {
        return draftSessionId;
    }

    public void setDraftSessionId(Long draftSessionId) {
        this.draftSessionId = draftSessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(Integer seatNo) {
        this.seatNo = seatNo;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public int getLastWave() {
        return lastWave;
    }

    public void setLastWave(int lastWave) {
        this.lastWave = lastWave;
    }

    public int getLastPickNo() {
        return lastPickNo;
    }

    public void setLastPickNo(int lastPickNo) {
        this.lastPickNo = lastPickNo;
    }
}
