package ygo.draftr.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.CubeEventRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.domain.CubeService;
import ygo.draftr.models.CubeEvent;
import ygo.draftr.models.CubeMember;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CubeMemberAdminService {

    private final CubeService cubeService;
    private final CubeMemberRepository memberRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    public CubeMemberAdminService(CubeService cubeService,
                                  CubeMemberRepository memberRepo,
                                  CubeEventRepository eventRepo,
                                  ObjectMapper objectMapper) {
        this.cubeService = cubeService;
        this.memberRepo = memberRepo;
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
    }

    // actor removes target, or target leaves
    @Transactional
    public void removeMember(Long cubeId, Long actorUserId, Long targetUserId) {
        cubeService.requireCube(cubeId);
        cubeService.requireMembership(cubeId, actorUserId);

        boolean actorIsOwner = cubeService.isOwner(cubeId, actorUserId);
        boolean self = actorUserId.equals(targetUserId);

        if (!self && !actorIsOwner) {
            throw new IllegalArgumentException("only owner can remove other members");
        }

        // prevent removing the cube owner unless self-removing and you handle transfer later
        // simplest: do not allow deleting OWNER membership at all right now
        var target = memberRepo.findByCubeIdAndUserId(cubeId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("target user is not a member"));

        if ("OWNER".equalsIgnoreCase(target.getRole())) {
            throw new IllegalArgumentException("cannot remove owner (transfer ownership first)");
        }

        memberRepo.deleteByCubeIdAndUserId(cubeId, targetUserId);
    }

    public void updateRole(Long cubeId, Long actorUserId, Long targetUserId, String role) {
        // actor must be OWNER
        CubeMember actor = memberRepo.findByCubeIdAndUserId(cubeId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("actor is not a member of this cube"));

        if (!"OWNER".equals(actor.getRole())) {
            throw new IllegalArgumentException("only the owner can change roles");
        }

        CubeMember target = memberRepo.findByCubeIdAndUserId(cubeId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("target is not a member of this cube"));

        if ("OWNER".equals(target.getRole())) {
            throw new IllegalArgumentException("cannot change the owner's role");
        }

        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }

        String normalized = role.trim().toUpperCase();
        if (!normalized.equals("ADMIN") && !normalized.equals("MEMBER")) {
            throw new IllegalArgumentException("role must be ADMIN or MEMBER");
        }

        target.setRole(normalized);
        memberRepo.save(target);
    }

    @Transactional
    public void updateWins(Long cubeId, Long actorId, Long targetId, int delta) {

        CubeMember actor = memberRepo.findByCubeIdAndUserId(cubeId, actorId)
                .orElseThrow(() -> new IllegalArgumentException("actor is not a member"));

        if (!"OWNER".equals(actor.getRole()) && !"ADMIN".equals(actor.getRole())) {
            throw new IllegalArgumentException("not authorized");
        }

        CubeMember target = memberRepo.findByCubeIdAndUserId(cubeId, targetId)
                .orElseThrow(() -> new IllegalArgumentException("target not found"));

        int before = target.getWins();
        int after = before + delta;

        if (delta == 0) return;
        if (after < 0) throw new IllegalArgumentException("wins cannot be negative");

        target.setWins(after);
        memberRepo.save(target);

        // AUDIT LOG
        CubeEvent ev = new CubeEvent();
        ev.setCubeId(cubeId);
        ev.setEventType("MEMBER_WINS_CHANGED");
        ev.setActorUserId(actorId);

        ev.setSummary(String.format("User %d wins %s (%d → %d)",
                targetId,
                delta > 0 ? "+" + delta : String.valueOf(delta),
                before,
                after));

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("targetUserId", targetId);
            payload.put("delta", delta);
            payload.put("winsBefore", before);
            payload.put("winsAfter", after);

            ev.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            // don't fail the main operation if payload serialization fails
            ev.setPayload(null);
        }

        eventRepo.save(ev);
    }
}
