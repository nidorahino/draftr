package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.CollectionQtyRequest;
import ygo.draftr.controllers.dto.CubeCollectionCardResponse;
import ygo.draftr.domain.CollectionService;
import ygo.draftr.models.CubeCollectionCard;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/cubes/{cubeId}/users/{userId}/collection")
public class CollectionController {

    private final CollectionService service;

    public CollectionController(CollectionService service) {
        this.service = service;
    }

    // For now actorUserId is passed as a query param.
    // Later, replace this with JWT auth and read actorUserId from the token.
    @PutMapping
    public ResponseEntity<?> setQty(@PathVariable Long cubeId,
                                    @PathVariable Long userId,
                                    @RequestBody CollectionQtyRequest request) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            CubeCollectionCard saved = service.setQty(
                    cubeId,
                    userId,
                    request.getCardId(),
                    request.getQty(),
                    actorUserId
            );

            // ✅ qty=0 => service returns null => 204 No Content
            if (saved == null) {
                return ResponseEntity.noContent().build();
            }

            CubeCollectionCardResponse resp =
                    toResponse(saved, service.isCardBanned(cubeId, saved.getCardId()));

            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getCollection(@PathVariable Long cubeId,
                                           @PathVariable Long userId) {
        try {
            List<CubeCollectionCard> rows = service.getUserCollection(cubeId, userId);
            List<CubeCollectionCardResponse> resp = new ArrayList<>();

            for (CubeCollectionCard row : rows) {
                boolean banned = service.isCardBanned(cubeId, row.getCardId());
                resp.add(toResponse(row, banned));
            }

            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private CubeCollectionCardResponse toResponse(CubeCollectionCard row, boolean banned) {
        CubeCollectionCardResponse r = new CubeCollectionCardResponse();
        r.setCubeId(row.getCubeId());
        r.setUserId(row.getUserId());
        r.setCardId(row.getCardId());
        r.setQty(row.getQty());
        r.setUpdatedAt(row.getUpdatedAt());
        r.setBanned(banned);
        return r;
    }
}
