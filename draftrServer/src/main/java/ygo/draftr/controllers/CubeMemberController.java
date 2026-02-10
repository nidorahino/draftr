package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.JoinCubeRequest;
import ygo.draftr.data.UserRepository;
import ygo.draftr.domain.CubeMemberService;
import ygo.draftr.models.CubeMember;
import ygo.draftr.models.User;

@RestController
@RequestMapping("/api/cubes/{cubeId}/members")
public class CubeMemberController {

    private final CubeMemberService service;
    private final UserRepository userRepo;

    public CubeMemberController(CubeMemberService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    // POST /api/cubes/{cubeId}/members
    // body: { "userId": 5 } OR { "username": "someUser" }
    @PostMapping
    public ResponseEntity<?> joinCube(@PathVariable Long cubeId,
                                      @RequestBody JoinCubeRequest request) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            Long targetUserId = request.getUserId();

            if (targetUserId == null) {
                String username = request.getUsername();

                if (username == null || username.isBlank()) {
                    return ResponseEntity.badRequest().body("userId or username is required");
                }

                User u = userRepo.findByUsernameIgnoreCase(username.trim()).orElse(null);
                if (u == null) {
                    return ResponseEntity.badRequest().body("user not found");
                }

                targetUserId = u.getUserId();
            }

            CubeMember saved = service.joinCube(cubeId, targetUserId, actorUserId);
            return ResponseEntity.ok(saved);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // DELETE /api/cubes/{cubeId}/members/me
    @DeleteMapping("/me")
    public ResponseEntity<?> leaveCube(@PathVariable Long cubeId) {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        try {
            service.leaveCube(cubeId, actorUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
