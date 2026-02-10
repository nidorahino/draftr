package ygo.draftr.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cube_event")
public class CubeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cube_event_id")
    private Long cubeEventId;

    @Column(name = "cube_id", nullable = false)
    private Long cubeId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 255)
    private String summary;

    @Column(columnDefinition = "text")
    private String payload;

    // optional references
    private Long cardId;
    private Long targetUserId;

    public Long getCubeEventId() { return cubeEventId; }
    public void setCubeEventId(Long cubeEventId) { this.cubeEventId = cubeEventId; }

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
}
