package ygo.draftr.controllers.dto;

public class ApplyLoserPickResponse {
    private Long bannedCardId;

    public ApplyLoserPickResponse(Long bannedCardId) {
        this.bannedCardId = bannedCardId;
    }

    public Long getBannedCardId() { return bannedCardId; }
}
