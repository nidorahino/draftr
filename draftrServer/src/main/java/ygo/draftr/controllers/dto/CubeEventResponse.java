package ygo.draftr.controllers.dto;

import java.time.Instant;

public class CubeEventResponse {

    private Long cubeEventId;
    private Long cubeId;
    private String eventType;
    private Long actorUserId;
    private Instant createdAt;
    private String summary;

    private String payload;     // JSON string (optional)
    private Long cardId;        // optional
    private Long targetUserId;  // optional

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
