package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.UpdateMemberRoleRequest;
import ygo.draftr.controllers.dto.UpdateWinsRequest;
import ygo.draftr.domain.CubeMemberAdminService;

@RestController
@RequestMapping("/api/cubes/{cubeId}/members")
public class CubeMemberAdminController {

    private final CubeMemberAdminService service;

    public CubeMemberAdminController(CubeMemberAdminService service) {
        this.service = service;
    }

    // DELETE /api/cubes/1/members/5?actorUserId=2
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<?> remove(@PathVariable Long cubeId,
                                    @PathVariable Long targetUserId) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            service.removeMember(cubeId, actorUserId, targetUserId);

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // PATCH /api/cubes/{cubeId}/members/{targetUserId}/role
    @PatchMapping("/{targetUserId}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long cubeId,
                                        @PathVariable Long targetUserId,
                                        @RequestBody UpdateMemberRoleRequest request) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            service.updateRole(cubeId, actorUserId, targetUserId, request.getRole());
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PatchMapping("/{targetUserId}/wins")
    public ResponseEntity<?> updateWins(@PathVariable Long cubeId,
                                        @PathVariable Long targetUserId,
                                        @RequestBody UpdateWinsRequest request) {

        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            service.updateWins(
                    cubeId,
                    actorUserId,
                    targetUserId,
                    request.getDelta()
            );

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
