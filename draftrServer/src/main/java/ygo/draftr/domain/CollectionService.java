package ygo.draftr.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.CubeCardRepository;
import ygo.draftr.data.CubeCollectionCardRepository;
import ygo.draftr.data.CubeEventRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.models.CubeCard;
import ygo.draftr.models.CubeCollectionCard;
import ygo.draftr.models.CubeEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CollectionService {

    private final CubeMemberRepository cubeMemberRepo;
    private final CubeCardRepository cubeCardRepo;
    private final CubeCollectionCardRepository collectionRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    public CollectionService(CubeMemberRepository cubeMemberRepo,
                             CubeCardRepository cubeCardRepo,
                             CubeCollectionCardRepository collectionRepo,
                             CubeEventRepository eventRepo,
                             ObjectMapper objectMapper) {
        this.cubeMemberRepo = cubeMemberRepo;
        this.cubeCardRepo = cubeCardRepo;
        this.collectionRepo = collectionRepo;
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CubeCollectionCard setQty(Long cubeId, Long userId, Long cardId, int qty, Long actorUserId) {
        if (qty < 0) throw new IllegalArgumentException("qty must be >= 0");

        if (!cubeMemberRepo.existsByCubeIdAndUserId(cubeId, userId)) {
            throw new IllegalArgumentException("user is not a member of this cube");
        }
        if (!cubeCardRepo.existsByCubeIdAndCardId(cubeId, cardId)) {
            throw new IllegalArgumentException("card is not in this cube");
        }

        CubeCollectionCard existing = collectionRepo
                .findByCubeIdAndUserIdAndCardId(cubeId, userId, cardId)
                .orElse(null);

        int prevQty = existing == null ? 0 : existing.getQty();

        // ✅ if qty becomes 0, delete instead of saving 0
        if (qty == 0) {
            if (existing != null) {
                collectionRepo.delete(existing);
            }

            saveEvent(cubeId, actorUserId, userId, cardId,
                    "COLLECTION_CARD_REMOVED",
                    "Removed from collection",
                    prevQty, 0);

            return null; // controller can return 204
        }

        CubeCollectionCard row = existing != null ? existing : new CubeCollectionCard();
        row.setCubeId(cubeId);
        row.setUserId(userId);
        row.setCardId(cardId);
        row.setQty(qty);
        row.setUpdatedAt(Instant.now());

        CubeCollectionCard saved = collectionRepo.save(row);

        saveEvent(cubeId, actorUserId, userId, cardId,
                "COLLECTION_QTY_SET",
                "Qty changed",
                prevQty, qty);

        return saved;
    }

    private void saveEvent(Long cubeId, Long actorUserId, Long targetUserId, Long cardId,
                           String type, String summary, int prevQty, int newQty) {

        CubeEvent event = new CubeEvent();
        event.setCubeId(cubeId);
        event.setActorUserId(actorUserId);
        event.setTargetUserId(targetUserId);
        event.setCardId(cardId);
        event.setEventType(type);
        event.setCreatedAt(Instant.now());

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetUserId", targetUserId);
        payload.put("cardId", cardId);
        payload.put("prevQty", prevQty);
        payload.put("newQty", newQty);

        event.setPayload(toJson(payload));

        // ✅ make summary human-readable for list view
        event.setSummary(summary + " (" + prevQty + " → " + newQty + ")");

        eventRepo.save(event);
    }

    @Transactional(readOnly = true)
    public List<CubeCollectionCard> getUserCollection(Long cubeId, Long userId) {
        if (!cubeMemberRepo.existsByCubeIdAndUserId(cubeId, userId)) {
            throw new IllegalArgumentException("user is not a member of this cube");
        }
        return collectionRepo.findByCubeIdAndUserId(cubeId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isCardBanned(Long cubeId, Long cardId) {
        return cubeCardRepo.findByCubeIdAndCardId(cubeId, cardId)
                .map(CubeCard::isBanned)
                .orElse(false);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            // Keep things moving; payload is optional
            return null;
        }
    }
}
