package ygo.draftr.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cube")
public class Cube {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cube_id")
    private Long cubeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = false)
    private int maxPlayers = 4;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    // Getters / Setters

    public Long getCubeId() {
        return cubeId;
    }
    public void setCubeId(Long cubeId) {
        this.cubeId = cubeId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }
    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
}
