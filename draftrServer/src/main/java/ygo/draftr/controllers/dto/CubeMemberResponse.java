package ygo.draftr.controllers.dto;

import java.time.Instant;

public class CubeMemberResponse {

    private Long cubeId;
    private Long userId;
    private String role;
    private Instant joinedAt;
    private String username;
    private int wins;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
}
