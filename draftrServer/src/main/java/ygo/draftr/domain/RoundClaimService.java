package ygo.draftr.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import ygo.draftr.controllers.dto.*;
import ygo.draftr.data.*;
import ygo.draftr.models.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class RoundClaimService {

    private final RoundClaimRepository roundClaimRepo;
    private final CubeMemberRepository memberRepo;
    private final CubeCardRepository cubeCardRepo;
    private final CubeCollectionCardRepository collectionRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    public RoundClaimService(
            RoundClaimRepository roundClaimRepo,
            CubeMemberRepository memberRepo,
            CubeCardRepository cubeCardRepo,
            CubeCollectionCardRepository collectionRepo,
            CubeEventRepository eventRepo,
            ObjectMapper objectMapper
    ) {
        this.roundClaimRepo = roundClaimRepo;
        this.memberRepo = memberRepo;
        this.cubeCardRepo = cubeCardRepo;
        this.collectionRepo = collectionRepo;
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
    }

    private CubeMember requireMembership(Long cubeId, Long userId) {
        return memberRepo.findByCubeIdAndUserId(cubeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("actor is not a member of this cube"));
    }

    @Transactional
    public RoundClaim claimLossCreatePending(Long cubeId, Long loserUserId, Long winnerUserId) {
        if (winnerUserId == null) throw new IllegalArgumentException("winnerUserId is required");
        if (winnerUserId.equals(loserUserId)) throw new IllegalArgumentException("winner cannot be the loser");

        requireMembership(cubeId, loserUserId);
        requireMembership(cubeId, winnerUserId);

        RoundClaim rc = new RoundClaim();
        rc.setCubeId(cubeId);
        rc.setLoserUserId(loserUserId);
        rc.setWinnerUserId(winnerUserId);

        try {
            // if a pending already exists, the partial unique index will reject this
            return roundClaimRepo.save(rc);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("There is already a pending round. Claim both spins first.");
        }
    }

    @Transactional(readOnly = true)
    public PendingSpinResponse pendingForUser(Long cubeId, Long userId) {
        requireMembership(cubeId, userId);

        var opt = roundClaimRepo.findFirstByCubeIdAndAppliedAtIsNullOrderByCreatedAtDesc(cubeId);
        if (opt.isEmpty()) {
            return new PendingSpinResponse(false, false, null);
        }

        RoundClaim rc = opt.get();
        boolean loserAvail = !rc.isLoserSpinClaimed() && userId.equals(rc.getLoserUserId());
        boolean winnerAvail = !rc.isWinnerSpinClaimed() && userId.equals(rc.getWinnerUserId());

        return new PendingSpinResponse(loserAvail, winnerAvail, rc.getRoundClaimId());
    }

    @Transactional
    public void claimLoserSpin(Long cubeId, Long actorUserId) {
        requireMembership(cubeId, actorUserId);

        RoundClaim rc = roundClaimRepo.findPendingForUpdateList(cubeId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending round."));

        if (!actorUserId.equals(rc.getLoserUserId())) {
            throw new IllegalArgumentException("You are not the loser for the pending round.");
        }
        if (rc.isLoserSpinClaimed()) {
            return; // idempotent
        }

        // MVP: just mark claimed (later apply loser wheel reward here)
        rc.setLoserSpinClaimed(true);
        roundClaimRepo.save(rc);

        maybeApply(rc);
    }

    @Transactional
    public void claimWinnerSpin(Long cubeId, Long actorUserId) {
        requireMembership(cubeId, actorUserId);

        RoundClaim rc = roundClaimRepo.findPendingForUpdateList(cubeId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending round."));

        if (!actorUserId.equals(rc.getWinnerUserId())) {
            throw new IllegalArgumentException("You are not the winner for the pending round.");
        }
        if (rc.isWinnerSpinClaimed()) {
            return; // idempotent
        }

        // MVP: just mark claimed (later apply winner wheel reward here)
        rc.setWinnerSpinClaimed(true);
        roundClaimRepo.save(rc);

        maybeApply(rc);
    }

    @Transactional
    public WinnerOfferResponse startWinnerSpin(Long cubeId, Long actorUserId) {
        requireMembership(cubeId, actorUserId);

        RoundClaim rc = roundClaimRepo.findPendingForUpdateList(cubeId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending round."));

        if (!actorUserId.equals(rc.getWinnerUserId())) {
            throw new IllegalArgumentException("You are not the winner for the pending round.");
        }
        if (rc.isWinnerSpinClaimed()) {
            throw new IllegalArgumentException("Winner spin already claimed.");
        }

        // if offer already exists for this round, reuse it (idempotent)
        List<Long> existing = findWinnerOfferCardIds(cubeId, rc.getRoundClaimId());
        if (existing != null) {
            return new WinnerOfferResponse(rc.getRoundClaimId(), existing);
        }

        // generate up to 12 unique cardIds (no duplicates) from non-banned pool
        List<Long> offer = generateWinnerOffer(cubeId, 12);

        // persist offer in cube_event so refresh doesn't change it
        writeEvent(cubeId, "WINNER_SPIN_OFFER_CREATED", actorUserId,
                Map.of("roundClaimId", rc.getRoundClaimId(), "offeredCardIds", offer));

        return new WinnerOfferResponse(rc.getRoundClaimId(), offer);
    }

    @Transactional
    public ApplyWinnerPickResponse applyWinnerSpin(Long cubeId, Long actorUserId, List<Long> selectedCardIds) {
        requireMembership(cubeId, actorUserId);

        if (selectedCardIds == null) throw new IllegalArgumentException("selectedCardIds is required");
        List<Long> picks = selectedCardIds.stream().filter(Objects::nonNull).distinct().toList();
        if (picks.size() != 2) throw new IllegalArgumentException("Pick exactly 2 cards.");

        RoundClaim rc = roundClaimRepo.findPendingForUpdateList(cubeId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending round."));

        if (!actorUserId.equals(rc.getWinnerUserId())) {
            throw new IllegalArgumentException("You are not the winner for the pending round.");
        }
        if (rc.isWinnerSpinClaimed()) {
            List<Long> prev = findWinnerAppliedCardIds(cubeId, rc.getRoundClaimId());

            if (prev == null) {
                throw new IllegalStateException("Winner spin already applied but no record found.");
            }

            return new ApplyWinnerPickResponse(prev);
        }

        List<Long> offer = findWinnerOfferCardIds(cubeId, rc.getRoundClaimId());
        if (offer == null) throw new IllegalArgumentException("No winner offer exists yet. Click Claim Winner Spin first.");

        Set<Long> offerSet = new HashSet<>(offer);
        for (Long pick : picks) {
            if (!offerSet.contains(pick)) {
                throw new IllegalArgumentException("Selected card is not in the current offer.");
            }
        }

        // apply: +1 quantity per pick
        for (Long cardId : picks) {
            addOneToCollection(cubeId, actorUserId, cardId);
        }

        rc.setWinnerSpinClaimed(true);
        roundClaimRepo.save(rc);

        writeEvent(
                cubeId,
                "WINNER_SPIN_APPLIED",
                actorUserId,
                Map.of(
                        "roundClaimId", rc.getRoundClaimId(),
                        "offeredCardIds", offer,
                        "selectedCardIds", picks
                )
        );

        // this will increment wins only when BOTH spins claimed
        maybeApply(rc);

        return new ApplyWinnerPickResponse(picks);
    }

    private List<Long> generateWinnerOffer(Long cubeId, int max) {
        List<CubeCard> pool = cubeCardRepo.findByCubeIdAndBannedFalse(cubeId);

        // cube_card is unique per (cubeId, cardId), so cardIds are already unique
        List<Long> ids = pool.stream().map(CubeCard::getCardId).collect(java.util.stream.Collectors.toList());
        java.util.Collections.shuffle(ids);

        if (ids.size() <= max) return ids;
        return ids.subList(0, max);
    }

    private void addOneToCollection(Long cubeId, Long userId, Long cardId) {
        CubeCollectionCard row = collectionRepo
                .findByCubeIdAndUserIdAndCardId(cubeId, userId, cardId)
                .orElseGet(() -> {
                    CubeCollectionCard n = new CubeCollectionCard();
                    n.setCubeId(cubeId);
                    n.setUserId(userId);
                    n.setCardId(cardId);
                    n.setQty(0);
                    return n;
                });

        row.setQty(row.getQty() + 1);
        collectionRepo.save(row);
    }

    private void writeEvent(Long cubeId, String type, Long actorUserId, Object payloadObj) {
        try {
            CubeEvent e = new CubeEvent();
            e.setCubeId(cubeId);
            e.setEventType(type);
            e.setActorUserId(actorUserId);
            e.setSummary(type);
            e.setPayload(objectMapper.writeValueAsString(payloadObj));
            eventRepo.save(e);
        } catch (Exception ex) {
            throw new RuntimeException("failed to write cube event", ex);
        }
    }

    private List<Long> findWinnerOfferCardIds(Long cubeId, Long roundClaimId) {
        // scan recent events; your app is tiny so this is fine
        List<CubeEvent> events = eventRepo.findByCubeIdOrderByCreatedAtDesc(cubeId, org.springframework.data.domain.PageRequest.of(0, 200))
                .getContent();

        for (CubeEvent e : events) {
            if (!"WINNER_SPIN_OFFER_CREATED".equals(e.getEventType())) continue;
            Long rcId = extractLong(e.getPayload(), "roundClaimId");
            if (rcId == null || !rcId.equals(roundClaimId)) continue;

            return extractLongList(e.getPayload(), "offeredCardIds");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Long extractLong(String payloadJson, String key) {
        if (payloadJson == null || payloadJson.isBlank()) return null;

        try {
            Map<String, Object> m = objectMapper.readValue(payloadJson, Map.class);
            Object v = m.get(key);

            if (v == null) return null;

            return Long.valueOf(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractLongList(String payloadJson, String key) {
        if (payloadJson == null || payloadJson.isBlank()) return null;

        try {
            Map<String, Object> m = objectMapper.readValue(payloadJson, Map.class);
            Object v = m.get(key);

            if (!(v instanceof List<?> list)) return null;

            List<Long> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) continue;
                out.add(Long.valueOf(String.valueOf(o)));
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public LoserOfferResponse startLoserSpin(Long cubeId, Long actorUserId) {
        requireMembership(cubeId, actorUserId);

        RoundClaim rc = roundClaimRepo.findPendingForUpdateList(cubeId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending round."));

        if (!actorUserId.equals(rc.getLoserUserId())) {
            throw new IllegalArgumentException("You are not the loser for the pending round.");
        }
        if (rc.isLoserSpinClaimed()) {
            throw new IllegalArgumentException("Loser spin already claimed.");
        }

        // idempotent: reuse existing offer for this round
        List<Long> existing = findLoserOfferCardIds(cubeId, rc.getRoundClaimId());
        if (existing != null) {
            return new LoserOfferResponse(rc.getRoundClaimId(), rc.getWinnerUserId(), existing);
        }

        // Build offer from opponent (winner) collection: RANDOM sample up to 8 unique cardIds
        List<CubeCollectionCard> oppRows =
                collectionRepo.findByCubeIdAndUserId(cubeId, rc.getWinnerUserId());

        List<Long> uniqueCardIds = oppRows.stream()
                .map(CubeCollectionCard::getCardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueCardIds.isEmpty()) {
            throw new IllegalArgumentException("Opponent has no cards in their collection to offer.");
        }

        List<Long> offer = new ArrayList<>(uniqueCardIds);
        Collections.shuffle(offer);
        if (offer.size() > 8) {
            offer = offer.subList(0, 8);
        }

        writeEvent(
                cubeId,
                "LOSER_SPIN_OFFER_CREATED",
                actorUserId,
                java.util.Map.of(
                        "roundClaimId", rc.getRoundClaimId(),
                        "opponentUserId", rc.getWinnerUserId(),
                        "offeredCardIds", offer
                )
        );

        return new LoserOfferResponse(
                rc.getRoundClaimId(),
                rc.getWinnerUserId(),
                offer
        );
    }

    @Transactional
    public ApplyLoserPickResponse applyLoserSpin(Long cubeId, Long actorUserId, java.util.List<Long> selectedCardIds) {
        requireMembership(cubeId, actorUserId);

        if (selectedCardIds == null) throw new IllegalArgumentException("selectedCardIds is required");

        List<Long> picks = selectedCardIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (picks.isEmpty()) throw new IllegalArgumentException("Select at least 1 card.");
        if (picks.size() > 8) throw new IllegalArgumentException("You can select at most 8 cards.");

        RoundClaim rc = roundClaimRepo.findPendingForUpdateList(cubeId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No pending round."));

        if (!actorUserId.equals(rc.getLoserUserId())) {
            throw new IllegalArgumentException("You are not the loser for the pending round.");
        }
        if (rc.isLoserSpinClaimed()) {
            throw new IllegalArgumentException("Loser spin already claimed.");
        }

        List<Long> offer = findLoserOfferCardIds(cubeId, rc.getRoundClaimId());
        if (offer == null) {
            throw new IllegalArgumentException("No loser offer exists yet. Start the loser spin first.");
        }

        Set<Long> offerSet = new HashSet<>(offer);
        for (Long pick : picks) {
            if (!offerSet.contains(pick)) {
                throw new IllegalArgumentException("Selected card is not in the current offer.");
            }
        }

        // Randomly ban 1 from the chosen list (1..8)
        Long bannedCardId = picks.get(new Random().nextInt(picks.size()));

        // Apply ban in cube pool
        CubeCard cubeCard = cubeCardRepo.findByCubeIdAndCardId(cubeId, bannedCardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found in cube pool."));
        if (!cubeCard.isBanned()) {
            cubeCard.setBanned(true);
            cubeCardRepo.save(cubeCard);
        }

        // mark loser spin claimed
        rc.setLoserSpinClaimed(true);
        roundClaimRepo.save(rc);

        writeEvent(
                cubeId,
                "LOSER_SPIN_BAN_APPLIED",
                actorUserId,
                java.util.Map.of(
                        "roundClaimId", rc.getRoundClaimId(),
                        "opponentUserId", rc.getWinnerUserId(),
                        "selectedCardIds", picks,
                        "bannedCardId", bannedCardId
                )
        );

        maybeApply(rc);

        return new ApplyLoserPickResponse(bannedCardId);
    }

    private List<Long> findLoserOfferCardIds(Long cubeId, Long roundClaimId) {
        var page = eventRepo.findByCubeIdOrderByCreatedAtDesc(
                cubeId,
                org.springframework.data.domain.PageRequest.of(0, 200)
        );

        for (CubeEvent e : page.getContent()) {
            if (!"LOSER_SPIN_OFFER_CREATED".equals(e.getEventType())) continue;

            Long rcId = extractLong(e.getPayload(), "roundClaimId");
            if (rcId == null || !rcId.equals(roundClaimId)) continue;

            return extractLongList(e.getPayload(), "offeredCardIds");
        }
        return null;
    }

    private void maybeApply(RoundClaim rc) {
        if (rc.getAppliedAt() != null) return;
        if (!rc.isLoserSpinClaimed() || !rc.isWinnerSpinClaimed()) return;

        // Apply: increment winner wins only after both spins claimed
        CubeMember winner = requireMembership(rc.getCubeId(), rc.getWinnerUserId());
        winner.setWins(winner.getWins() + 1);
        memberRepo.save(winner);

        rc.setAppliedAt(Instant.now());
        roundClaimRepo.save(rc);
    }

    private List<Long> findWinnerAppliedCardIds(Long cubeId, Long roundClaimId) {
        var page = eventRepo.findByCubeIdOrderByCreatedAtDesc(
                cubeId,
                org.springframework.data.domain.PageRequest.of(0, 200)
        );

        for (CubeEvent e : page.getContent()) {
            if (!"WINNER_SPIN_APPLIED".equals(e.getEventType())) continue;

            Long rcId = extractLong(e.getPayload(), "roundClaimId");
            if (rcId == null || !rcId.equals(roundClaimId)) continue;

            return extractLongList(e.getPayload(), "selectedCardIds");
        }

        return null;
    }

}