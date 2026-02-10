package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.CubeCardDetailsResponse;
import ygo.draftr.data.CardRepository;
import ygo.draftr.data.CubeCardRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.models.Card;
import ygo.draftr.models.CubeCard;

import java.util.*;

@RestController
@RequestMapping("/api/cubes/{cubeId}/cards")
public class CubeCardDetailsController {

    private final CubeMemberRepository memberRepo;
    private final CubeCardRepository cubeCardRepo;
    private final CardRepository cardRepo;

    public CubeCardDetailsController(CubeMemberRepository memberRepo,
                                     CubeCardRepository cubeCardRepo,
                                     CardRepository cardRepo) {
        this.memberRepo = memberRepo;
        this.cubeCardRepo = cubeCardRepo;
        this.cardRepo = cardRepo;
    }

    // GET /api/cubes/1/cards/details?actorUserId=2&includeBanned=true
    @GetMapping("/details")
    public ResponseEntity<?> listCubeCardsWithDetails(@PathVariable Long cubeId,
                                                      @RequestParam(defaultValue = "true") boolean includeBanned) {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (!memberRepo.existsByCubeIdAndUserId(cubeId, actorUserId)) {
            return ResponseEntity.badRequest().body("actor is not a member of this cube");
        }

        List<CubeCard> cubeCards = includeBanned
                ? cubeCardRepo.findByCubeId(cubeId)
                : cubeCardRepo.findByCubeIdAndBannedFalse(cubeId);

        Set<Long> cardIds = new HashSet<>();
        for (CubeCard cc : cubeCards) {
            cardIds.add(cc.getCardId());
        }

        List<Card> cards = cardIds.isEmpty()
                ? List.of()
                : cardRepo.findAllById(cardIds);

        Map<Long, Card> cardById = new HashMap<>();
        for (Card c : cards) {
            cardById.put(c.getId(), c);
        }

        List<CubeCardDetailsResponse> resp = new ArrayList<>();
        for (CubeCard cc : cubeCards) {
            Card c = cardById.get(cc.getCardId());

            CubeCardDetailsResponse r = new CubeCardDetailsResponse();
            r.setCubeId(cc.getCubeId());
            r.setCardId(cc.getCardId());
            r.setMaxQty(cc.getMaxQty());
            r.setBanned(cc.isBanned());

            if (c != null) {
                r.setName(c.getName());
                r.setCardType(c.getCardType());
                r.setHumanReadableCardType(c.getHumanReadableCardType());
                r.setFrameType(c.getFrameType());
                r.setRace(c.getRace());
                r.setArchetype(c.getArchetype());
                r.setTypeline(c.getTypeline());
                r.setAttribute(c.getAttribute());
                r.setLevel(c.getLevel());
                r.setAtk(c.getAtk());
                r.setDef(c.getDef());
                r.setImageUrl(c.getImageUrl());
                r.setDescription(c.getDescription());
            }

            resp.add(r);
        }

        return ResponseEntity.ok(resp);
    }
}
