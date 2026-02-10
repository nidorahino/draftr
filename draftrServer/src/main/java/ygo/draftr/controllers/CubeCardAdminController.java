package ygo.draftr.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.CardSearchResult;
import ygo.draftr.controllers.dto.UpdateCubeCardQtyRequest;
import ygo.draftr.data.CardRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.models.Card;
import ygo.draftr.models.CubeCard;
import ygo.draftr.domain.CubeCardAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/cubes/{cubeId}/cards")
public class CubeCardAdminController {

    private final CubeCardAdminService service;
    private final CubeMemberRepository memberRepo;
    private final CardRepository cardRepo;

    public CubeCardAdminController(CubeCardAdminService service,
                                   CubeMemberRepository memberRepo,
                                   CardRepository cardRepo) {
        this.service = service;
        this.memberRepo = memberRepo;
        this.cardRepo = cardRepo;
    }

    // GET /api/cubes/1/cards/pool-search?name=blue&page=0&size=10
    @GetMapping("/pool-search")
    public ResponseEntity<?> poolSearch(@PathVariable Long cubeId,
                                        @RequestParam String name,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        // ✅ OWNER/ADMIN only
        boolean isAdmin = memberRepo.existsByCubeIdAndUserIdAndRoleIn(
                cubeId, actorUserId, List.of("OWNER", "ADMIN")
        );
        if (!isAdmin) {
            return ResponseEntity.status(403).build();
        }

        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 2) {
            return ResponseEntity.badRequest().body("name must be at least 2 characters");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<CardSearchResult> resp = cardRepo
                .searchInCubePool(cubeId, trimmed, pageable)
                .map(this::toSearchResult);

        return ResponseEntity.ok(resp);
    }

    private CardSearchResult toSearchResult(Card c) {
        CardSearchResult r = new CardSearchResult();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setCardType(c.getCardType());
        r.setHumanReadableCardType(c.getHumanReadableCardType());
        r.setArchetype(c.getArchetype());
        r.setImageUrl(c.getImageUrl());
        return r;
    }


    // PUT /api/cubes/1/cards/6983839   body: { "maxQty": 2 }
    @PutMapping("/{cardId}")
    public ResponseEntity<?> updateQty(@PathVariable Long cubeId,
                                       @PathVariable Long cardId,
                                       @RequestBody UpdateCubeCardQtyRequest request) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            CubeCard saved = service.updateMaxQty(cubeId, cardId, request.getMaxQty(), actorUserId);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // DELETE /api/cubes/1/cards/6983839
    @DeleteMapping("/{cardId}")
    public ResponseEntity<?> remove(@PathVariable Long cubeId,
                                    @PathVariable Long cardId) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            service.removeCard(cubeId, cardId, actorUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}