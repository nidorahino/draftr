package ygo.draftr.controllers.dto;

import java.util.List;

public class LoserOfferResponse {
    private Long roundClaimId;
    private Long opponentUserId; // the winner (opponent) userId
    private List<Long> offeredCardIds;

    public LoserOfferResponse(Long roundClaimId, Long opponentUserId, List<Long> offeredCardIds) {
        this.roundClaimId = roundClaimId;
        this.opponentUserId = opponentUserId;
        this.offeredCardIds = offeredCardIds;
    }

    public Long getRoundClaimId() { return roundClaimId; }
    public Long getOpponentUserId() { return opponentUserId; }
    public List<Long> getOfferedCardIds() { return offeredCardIds; }
}
