package ygo.draftr.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ygo.draftr.controllers.dto.CreateDraftRequest;
import ygo.draftr.controllers.dto.DraftPlayerResponse;
import ygo.draftr.controllers.dto.MyPackResponse;
import ygo.draftr.domain.DraftService;
import ygo.draftr.models.DraftPlayer;
import ygo.draftr.models.DraftSession;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DraftController {

    private final DraftService draftService;

    public DraftController(DraftService draftService) {
        this.draftService = draftService;
    }

    // ----------- DTOs (controller-local) -----------

    public static class PickRequest {
        private Long draftPackCardId;
        public Long getDraftPackCardId() { return draftPackCardId; }
        public void setDraftPackCardId(Long draftPackCardId) { this.draftPackCardId = draftPackCardId; }
    }

    public static class ReadyRequest {
        private boolean ready;

        public boolean isReady() { return ready; }
        public void setReady(boolean ready) { this.ready = ready; }
    }

    public static class DraftStateResponse {
        private DraftSession session;
        private List<DraftPlayerResponse> players;

        public DraftSession getSession() { return session; }
        public void setSession(DraftSession session) { this.session = session; }

        public List<DraftPlayerResponse> getPlayers() { return players; }
        public void setPlayers(List<DraftPlayerResponse> players) { this.players = players; }
    }

    // ----------- helpers -----------

    private Long actorId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (!(auth.getPrincipal() instanceof Long uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid auth principal");
        }
        return uid;
    }

    // ----------- endpoints -----------

    @GetMapping("/cubes/{cubeId}/drafts/open")
    public List<DraftSession> openSessions(@PathVariable Long cubeId,
                                           Authentication authentication) {
        try {
            return draftService.getOpenSessions(cubeId, actorId(authentication));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/drafts/{draftId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long draftId,
                       Authentication authentication) {
        try {
            draftService.cancelDraft(draftId, actorId(authentication));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Create a draft lobby for a cube (also auto-joins creator).
     */
    @PostMapping("/cubes/{cubeId}/drafts")
    public DraftSession createLobby(@PathVariable Long cubeId,
                                    @RequestBody CreateDraftRequest req,
                                    Authentication authentication) {
        try {
            return draftService.createLobby(cubeId, actorId(authentication), req);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Join an existing lobby.
     */
    @PostMapping("/drafts/{draftId}/join")
    public DraftPlayer join(@PathVariable Long draftId,
                            Authentication authentication) {
        try {
            return draftService.joinLobby(draftId, actorId(authentication));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Set ready/unready in lobby.
     */
    @PostMapping("/drafts/{draftId}/ready")
    public DraftPlayer setReady(@PathVariable Long draftId,
                                @RequestBody ReadyRequest req,
                                Authentication authentication) {
        try {
            return draftService.setReady(draftId, actorId(authentication), req.isReady());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Start the draft (host only). Generates pool + packs.
     */
    @PostMapping("/drafts/{draftId}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start(@PathVariable Long draftId,
                      Authentication authentication) {
        try {
            draftService.startDraft(draftId, actorId(authentication));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * Leave lobby (optional). If you don't want this yet, delete it.
     */
    @PostMapping("/drafts/{draftId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long draftId,
                      Authentication authentication) {
        try {
            draftService.leaveLobby(draftId, actorId(authentication));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping("/drafts/{draftId}")
    public DraftStateResponse getState(
            @PathVariable Long draftId,
            Authentication authentication) {

        Long uid = actorId(authentication);

        DraftSession session = draftService.getSessionForMember(draftId, uid);

        List<DraftPlayerResponse> players =
                draftService.getPlayerResponses(draftId, uid);

        DraftStateResponse res = new DraftStateResponse();
        res.setSession(session);
        res.setPlayers(players);
        return res;
    }

    @GetMapping("/drafts/{draftId}/me/pack")
    public MyPackResponse myPack(@PathVariable Long draftId, Authentication authentication) {
        try {
            return draftService.getMyCurrentPack(draftId, actorId(authentication));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/drafts/{draftId}/pick")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pick(@PathVariable Long draftId,
                     @RequestBody PickRequest req,
                     Authentication authentication) {
        try {
            if (req == null || req.getDraftPackCardId() == null) {
                throw new IllegalArgumentException("draftPackCardId is required.");
            }
            draftService.pickCard(draftId, actorId(authentication), req.getDraftPackCardId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping("/drafts/{draftId}/me/picks")
    public List<MyPackResponse.MyPackCard> myPicks(@PathVariable Long draftId,
                                                   Authentication authentication) {
        try {
            return draftService.getMyPicks(draftId, actorId(authentication));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

}