package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.AddCubeCardRequest;
import ygo.draftr.domain.CubeCardPoolService;
import ygo.draftr.models.CubeCard;

@RestController
@RequestMapping("/api/cubes/{cubeId}/cards")
public class CubeCardPoolController {

    private final CubeCardPoolService service;

    public CubeCardPoolController(CubeCardPoolService service) {
        this.service = service;
    }

    // POST /api/cubes/1/cards?actorUserId=2
    // body: { "cardId": 86988864, "maxQty": 2 }
    @PostMapping
    public ResponseEntity<?> addCard(@PathVariable Long cubeId,
                                     @RequestBody AddCubeCardRequest request) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            CubeCard saved = service.addCard(
                    cubeId,
                    request.getCardId(),
                    request.getMaxQty(),
                    actorUserId
            );

            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
