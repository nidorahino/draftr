package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.CubeMemberResponse;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.data.UserRepository;
import ygo.draftr.models.CubeMember;
import ygo.draftr.models.User;

import java.util.*;

@RestController
@RequestMapping("/api/cubes/{cubeId}/members")
public class CubeMemberListController {

    private final CubeMemberRepository memberRepo;
    private final UserRepository userRepo;

    public CubeMemberListController(CubeMemberRepository memberRepo, UserRepository userRepo) {
        this.memberRepo = memberRepo;
        this.userRepo = userRepo;
    }

    // GET /api/cubes/1/members?actorUserId=2
    @GetMapping
    public ResponseEntity<?> listMembers(@PathVariable Long cubeId) {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (!memberRepo.existsByCubeIdAndUserId(cubeId, actorUserId)) {
            return ResponseEntity.badRequest().body("actor is not a member of this cube");
        }

        List<CubeMember> rows = memberRepo.findByCubeId(cubeId);

        // collect userIds
        Set<Long> userIds = new HashSet<>();
        for (CubeMember m : rows) userIds.add(m.getUserId());

        // batch load users
        List<User> users = userIds.isEmpty() ? List.of() : userRepo.findAllById(userIds);

        Map<Long, String> usernameById = new HashMap<>();
        for (User u : users) {
            usernameById.put(u.getUserId(), u.getUsername()); // change to u.getId() if needed
        }

        List<CubeMemberResponse> resp = new ArrayList<>();
        for (CubeMember m : rows) {
            CubeMemberResponse r = new CubeMemberResponse();
            r.setCubeId(m.getCubeId());
            r.setUserId(m.getUserId());
            r.setUsername(usernameById.get(m.getUserId())); // NEW
            r.setRole(m.getRole());
            r.setJoinedAt(m.getJoinedAt());
            r.setWins(m.getWins());
            resp.add(r);
        }

        return ResponseEntity.ok(resp);
    }
}
