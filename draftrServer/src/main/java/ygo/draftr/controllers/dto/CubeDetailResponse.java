package ygo.draftr.controllers.dto;

import java.time.Instant;

public class CubeDetailResponse {
    private Long cubeId;
    private String name;
    private Long ownerUserId;
    private Integer maxPlayers;
    private Instant createdAt;
    private String myRole;
    private Long myUserId;

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getMyRole() { return myRole; }
    public void setMyRole(String myRole) { this.myRole = myRole; }

    public Long getMyUserId() { return myUserId; }
    public void setMyUserId(Long myUserId) { this.myUserId = myUserId; }
}
