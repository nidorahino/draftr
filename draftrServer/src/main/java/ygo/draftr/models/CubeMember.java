package ygo.draftr.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "cube_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cube_id", "user_id"})
)
public class CubeMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cube_member_id")
    private Long cubeMemberId;

    @Column(name = "cube_id", nullable = false)
    private Long cubeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String role = "MEMBER"; // OWNER / MEMBER

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(nullable = false)
    private int wins = 0;

    public Long getCubeMemberId() { return cubeMemberId; }
    public void setCubeMemberId(Long cubeMemberId) { this.cubeMemberId = cubeMemberId; }

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
    public void incrementWins() { this.wins++; }
}
