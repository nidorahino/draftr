package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.BanRequest;
import ygo.draftr.domain.CubeCardService;
import ygo.draftr.models.CubeCard;

@RestController
@RequestMapping("/api/cubes/{cubeId}/cards")
public class CubeCardController {

    private final CubeCardService service;

    public CubeCardController(CubeCardService service) {
        this.service = service;
    }

    // PUT /api/cubes/1/cards/6983839/ban
    @PutMapping("/{cardId}/ban")
    public ResponseEntity<?> setBanned(@PathVariable Long cubeId,
                                       @PathVariable Long cardId,
                                       @RequestBody BanRequest request) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            CubeCard saved = service.setBanned(
                    cubeId,
                    cardId,
                    request.isBanned(),
                    actorUserId,
                    request.getReason()
            );

            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}