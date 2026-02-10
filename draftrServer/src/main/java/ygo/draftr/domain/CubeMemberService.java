package ygo.draftr.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.CubeEventRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.data.CubeRepository;
import ygo.draftr.models.Cube;
import ygo.draftr.models.CubeEvent;
import ygo.draftr.models.CubeMember;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class CubeMemberService {

    private final CubeRepository cubeRepo;
    private final CubeMemberRepository memberRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;

    public CubeMemberService(CubeRepository cubeRepo,
                             CubeMemberRepository memberRepo,
                             CubeEventRepository eventRepo,
                             ObjectMapper objectMapper) {
        this.cubeRepo = cubeRepo;
        this.memberRepo = memberRepo;
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CubeMember joinCube(Long cubeId, Long userId, Long actorUserId) {
        // For now: allow actor to add anyone. Later: restrict to owner or self.
        // Still enforce that actor is a member if cube already has members:
        // (optional rule; if you want "invite-only", keep it)
        if (memberRepo.countByCubeId(cubeId) > 0 && !memberRepo.existsByCubeIdAndUserId(cubeId, actorUserId)) {
            throw new IllegalArgumentException("actor is not a member of this cube");
        }

        Cube cube = cubeRepo.findById(cubeId)
                .orElseThrow(() -> new IllegalArgumentException("cube not found"));

        if (memberRepo.existsByCubeIdAndUserId(cubeId, userId)) {
            throw new IllegalArgumentException("user is already a member of this cube");
        }

        long currentMembers = memberRepo.countByCubeId(cubeId);
        if (currentMembers >= cube.getMaxPlayers()) {
            throw new IllegalArgumentException("cube is full");
        }

        CubeMember member = new CubeMember();
        member.setCubeId(cubeId);
        member.setUserId(userId);

        // If first member, make them owner automatically
        if (currentMembers == 0) {
            member.setRole("OWNER");
        } else {
            member.setRole("MEMBER");
        }

        member.setJoinedAt(Instant.now());
        CubeMember saved = memberRepo.save(member);

        CubeEvent event = new CubeEvent();
        event.setCubeId(cubeId);
        event.setActorUserId(actorUserId);
        event.setTargetUserId(userId);
        event.setEventType("MEMBER_JOINED");
        event.setCreatedAt(Instant.now());
        event.setSummary("Member joined cube");

        Map<String, Object> payload = new HashMap<>();
        payload.put("cubeId", cubeId);
        payload.put("userId", userId);
        payload.put("role", saved.getRole());

        event.setPayload(toJson(payload));
        eventRepo.save(event);

        return saved;
    }

    @Transactional
    public void leaveCube(Long cubeId, Long actorUserId) {

        CubeMember membership = memberRepo.findByCubeIdAndUserId(cubeId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("actor is not a member of this cube"));

        if ("OWNER".equalsIgnoreCase(membership.getRole())) {
            throw new IllegalArgumentException("owner cannot leave the cube (transfer ownership or archive)");
        }

        memberRepo.deleteByCubeIdAndUserId(cubeId, actorUserId);

        CubeEvent event = new CubeEvent();
        event.setCubeId(cubeId);
        event.setActorUserId(actorUserId);
        event.setTargetUserId(actorUserId);
        event.setEventType("MEMBER_LEFT");
        event.setCreatedAt(Instant.now());
        event.setSummary("Member left cube");

        Map<String, Object> payload = new HashMap<>();
        payload.put("cubeId", cubeId);
        payload.put("userId", actorUserId);

        event.setPayload(toJson(payload));
        eventRepo.save(event);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
