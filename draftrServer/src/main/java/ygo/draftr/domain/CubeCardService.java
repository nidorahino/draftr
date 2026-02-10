package ygo.draftr.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.CubeCardRepository;
import ygo.draftr.data.CubeEventRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.models.CubeCard;
import ygo.draftr.models.CubeEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class CubeCardService {

    private final CubeMemberRepository cubeMemberRepo;
    private final CubeCardRepository cubeCardRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    public CubeCardService(CubeMemberRepository cubeMemberRepo,
                           CubeCardRepository cubeCardRepo,
                           CubeEventRepository eventRepo,
                           ObjectMapper objectMapper) {
        this.cubeMemberRepo = cubeMemberRepo;
        this.cubeCardRepo = cubeCardRepo;
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CubeCard setBanned(Long cubeId, Long cardId, boolean banned, Long actorUserId, String reason) {

        // Must be a cube member to ban/unban
        if (!cubeMemberRepo.existsByCubeIdAndUserId(cubeId, actorUserId)) {
            throw new IllegalArgumentException("actor is not a member of this cube");
        }

        CubeCard cubeCard = cubeCardRepo.findByCubeIdAndCardId(cubeId, cardId)
                .orElseThrow(() -> new IllegalArgumentException("card is not in this cube"));

        boolean old = cubeCard.isBanned();
        cubeCard.setBanned(banned);
        CubeCard saved = cubeCardRepo.save(cubeCard);

        // Only log if something actually changed
        if (old != banned) {
            CubeEvent event = new CubeEvent();
            event.setCubeId(cubeId);
            event.setActorUserId(actorUserId);
            event.setCardId(cardId);
            event.setCreatedAt(Instant.now());
            event.setEventType(banned ? "CARD_BANNED" : "CARD_UNBANNED");
            event.setSummary(banned ? "Card banned" : "Card unbanned");

            Map<String, Object> payload = new HashMap<>();
            payload.put("cubeId", cubeId);
            payload.put("cardId", cardId);
            payload.put("banned", banned);
            if (reason != null && !reason.isBlank()) {
                payload.put("reason", reason.trim());
            }

            event.setPayload(toJson(payload));
            eventRepo.save(event);
        }

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
