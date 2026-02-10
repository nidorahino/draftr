package ygo.draftr.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.CardRepository;
import ygo.draftr.data.CubeCardRepository;
import ygo.draftr.data.CubeEventRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.models.Card;
import ygo.draftr.models.CubeCard;
import ygo.draftr.models.CubeEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class CubeCardPoolService {

    private final CubeMemberRepository memberRepo;
    private final CubeCardRepository cubeCardRepo;
    private final CardRepository cardRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    public CubeCardPoolService(CubeMemberRepository memberRepo,
                               CubeCardRepository cubeCardRepo,
                               CardRepository cardRepo,
                               CubeEventRepository eventRepo,
                               ObjectMapper objectMapper) {
        this.memberRepo = memberRepo;
        this.cubeCardRepo = cubeCardRepo;
        this.cardRepo = cardRepo;
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CubeCard addCard(Long cubeId, Long cardId, int maxQty, Long actorUserId) {
        if (!memberRepo.existsByCubeIdAndUserId(cubeId, actorUserId)) {
            throw new IllegalArgumentException("actor is not a member of this cube");
        }

        if (maxQty < 1 || maxQty > 4) {
            throw new IllegalArgumentException("maxQty must be between 1 and 4");
        }

        // ✅ Look up name for audit summary
        String cardName = cardRepo.findById(cardId)
                .map(Card::getName)
                .filter(n -> n != null && !n.isBlank())
                .orElse("Card #" + cardId);

        CubeCard cubeCard = cubeCardRepo.findByCubeIdAndCardId(cubeId, cardId)
                .orElseGet(() -> {
                    CubeCard cc = new CubeCard();
                    cc.setCubeId(cubeId);
                    cc.setCardId(cardId);
                    cc.setBanned(false);
                    return cc;
                });

        boolean wasNew = (cubeCard.getCubeCardId() == null);
        cubeCard.setMaxQty(maxQty);

        CubeCard saved = cubeCardRepo.save(cubeCard);

        CubeEvent event = new CubeEvent();
        event.setCubeId(cubeId);
        event.setActorUserId(actorUserId);
        event.setCardId(cardId);
        event.setCreatedAt(Instant.now());
        event.setEventType(wasNew ? "CUBE_CARD_ADDED" : "CUBE_CARD_UPDATED");

        // ✅ Better summary (always has name)
        if (wasNew) {
            event.setSummary("Added " + cardName + " (" + cardId + ") to cube");
        } else {
            event.setSummary("Updated cube card: " + cardName + " (" + cardId + "), maxQty=" + maxQty);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("cubeId", cubeId);
        payload.put("cardId", cardId);
        payload.put("cardName", cardName);
        payload.put("maxQty", maxQty);
        payload.put("banned", saved.isBanned());

        event.setPayload(toJson(payload));
        eventRepo.save(event);

        return saved;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
