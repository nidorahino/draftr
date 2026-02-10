package ygo.draftr.controllers;

import ygo.draftr.controllers.dto.*;
import ygo.draftr.domain.RoundClaimService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
public class RoundClaimController {

    private final RoundClaimService roundClaims;

    public RoundClaimController(RoundClaimService roundClaims) {
        this.roundClaims = roundClaims;
    }

    @PostMapping("/api/cubes/{cubeId}/rounds/claim-loss")
    public ResponseEntity<?> claimLoss(@PathVariable Long cubeId, @RequestBody ClaimLossRequest req) {
        Long actorUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            var rc = roundClaims.claimLossCreatePending(cubeId, actorUserId, req.getWinnerUserId());
            return ResponseEntity.ok(rc.getRoundClaimId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/api/cubes/{cubeId}/wheels/pending")
    public ResponseEntity<?> pending(@PathVariable Long cubeId) {
        Long actorUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            return ResponseEntity.ok(roundClaims.pendingForUser(cubeId, actorUserId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/api/cubes/{cubeId}/wheels/claim-winner")
    public ResponseEntity<?> startWinnerSpin(@PathVariable Long cubeId) {
        Long actorUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            return ResponseEntity.ok(roundClaims.startWinnerSpin(cubeId, actorUserId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/api/cubes/{cubeId}/wheels/claim-winner/apply")
    public ResponseEntity<?> applyWinnerSpin(@PathVariable Long cubeId, @RequestBody ApplyWinnerPickRequest req) {
        Long actorUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            roundClaims.applyWinnerSpin(cubeId, actorUserId, req.getSelectedCardIds());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/api/cubes/{cubeId}/wheels/claim-loser")
    public ResponseEntity<?> startLoserSpin(@PathVariable Long cubeId) {
        Long actorUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            return ResponseEntity.ok(roundClaims.startLoserSpin(cubeId, actorUserId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/api/cubes/{cubeId}/wheels/claim-loser/apply")
    public ResponseEntity<?> applyLoserSpin(@PathVariable Long cubeId, @RequestBody ApplyLoserPickRequest req) {
        Long actorUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            var resp = roundClaims.applyLoserSpin(cubeId, actorUserId, req.getSelectedCardIds());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
