package ygo.draftr.controllers.dto;

import java.time.Instant;

public class CubeSummaryResponse {
    private Long cubeId;
    private String name;
    private Long ownerUserId;
    private int maxPlayers;
    private Instant createdAt;
    private String role; // OWNER/MEMBER

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
