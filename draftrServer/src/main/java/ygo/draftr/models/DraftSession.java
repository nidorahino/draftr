package ygo.draftr.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "draft_session")
public class DraftSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draft_session_id")
    private Long draftSessionId;

    @Column(name = "cube_id", nullable = false)
    private Long cubeId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DraftStatus status; // LOBBY, RUNNING, COMPLETED, CANCELLED

    @Column(name = "draft_size", nullable = false)
    private Integer draftSize;

    @Column(name = "pack_size", nullable = false)
    private Integer packSize;

    @Column(name = "packs_per_player", nullable = false)
    private Integer packsPerPlayer;

    @Column(name = "no_duplicates", nullable = false)
    private boolean noDuplicates;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "current_wave")
    private int currentWave;

    @Column(name = "current_pick_no")
    private int currentPickNo;

    @Column(name = "collections_applied_at")
    private Instant collectionsAppliedAt;

    // getters/setters

    public Long getDraftSessionId() {
        return draftSessionId;
    }

    public void setDraftSessionId(Long draftSessionId) {
        this.draftSessionId = draftSessionId;
    }

    public Long getCubeId() {
        return cubeId;
    }

    public void setCubeId(Long cubeId) {
        this.cubeId = cubeId;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = status;
    }

    public Integer getDraftSize() {
        return draftSize;
    }

    public void setDraftSize(Integer draftSize) {
        this.draftSize = draftSize;
    }

    public Integer getPackSize() {
        return packSize;
    }

    public void setPackSize(Integer packSize) {
        this.packSize = packSize;
    }

    public Integer getPacksPerPlayer() {
        return packsPerPlayer;
    }

    public void setPacksPerPlayer(Integer packsPerPlayer) {
        this.packsPerPlayer = packsPerPlayer;
    }

    public boolean isNoDuplicates() {
        return noDuplicates;
    }

    public void setNoDuplicates(boolean noDuplicates) {
        this.noDuplicates = noDuplicates;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public int getCurrentPickNo() {
        return currentPickNo;
    }

    public void setCurrentPickNo(int currentPickNo) {
        this.currentPickNo = currentPickNo;
    }

    public Instant getCollectionsAppliedAt() { return collectionsAppliedAt; }
    public void setCollectionsAppliedAt(Instant collectionsAppliedAt) { this.collectionsAppliedAt = collectionsAppliedAt; }
}
