package ygo.draftr.controllers.dto;

public class WinnerOfferResponse {
    private Long roundClaimId;
    private java.util.List<Long> offeredCardIds; // up to 12

    public WinnerOfferResponse(Long roundClaimId, java.util.List<Long> offeredCardIds) {
        this.roundClaimId = roundClaimId;
        this.offeredCardIds = offeredCardIds;
    }

    public Long getRoundClaimId() { return roundClaimId; }
    public java.util.List<Long> getOfferedCardIds() { return offeredCardIds; }
}
