package ygo.draftr.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.*;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.data.CubeRepository;
import ygo.draftr.models.Cube;
import ygo.draftr.models.CubeMember;
import ygo.draftr.domain.CubeService;

import java.nio.file.attribute.UserPrincipal;
import java.util.*;

@RestController
@RequestMapping("/api/cubes")
public class CubeController {

    private final CubeRepository cubeRepo;
    private final CubeMemberRepository memberRepo;
    private final CubeService cubeService;

    public CubeController(CubeRepository cubeRepo,
                          CubeMemberRepository memberRepo,
                          CubeService cubeService) {
        this.cubeRepo = cubeRepo;
        this.memberRepo = memberRepo;
        this.cubeService = cubeService;
    }

    // GET /api/cubes
    @GetMapping
    public ResponseEntity<?> listMyCubes() {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        List<CubeMember> memberships = memberRepo.findByUserId(actorUserId);

        Set<Long> cubeIds = new HashSet<>();
        for (CubeMember m : memberships) cubeIds.add(m.getCubeId());

        Map<Long, Cube> cubeById = new HashMap<>();
        if (!cubeIds.isEmpty()) {
            for (Cube c : cubeRepo.findAllByCubeIdInAndArchivedFalse(cubeIds)) {
                cubeById.put(c.getCubeId(), c);
            }
        }

        List<CubeSummaryResponse> resp = new ArrayList<>();
        for (CubeMember m : memberships) {
            Cube c = cubeById.get(m.getCubeId());

            // If cube is archived (or missing), it won't be in the map
            if (c == null) continue;

            CubeSummaryResponse r = new CubeSummaryResponse();
            r.setCubeId(c.getCubeId());
            r.setName(c.getName());
            r.setOwnerUserId(c.getOwnerUserId());
            r.setMaxPlayers(c.getMaxPlayers());
            r.setCreatedAt(c.getCreatedAt());
            r.setRole(m.getRole());
            resp.add(r);
        }

        return ResponseEntity.ok(resp);
    }

    // GET /api/cubes/{cubeId}
    @GetMapping("/{cubeId}")
    public ResponseEntity<?> getCube(@PathVariable Long cubeId) {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        Optional<CubeMember> membershipOpt =
                memberRepo.findByCubeIdAndUserId(cubeId, actorUserId);

        if (membershipOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("actor is not a member of this cube");
        }

        Optional<Cube> cubeOpt = cubeRepo.findByCubeIdAndArchivedFalse(cubeId);
        if (cubeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CubeMember membership = membershipOpt.get();
        Cube cube = cubeOpt.get();

        CubeDetailResponse resp = new CubeDetailResponse();
        resp.setCubeId(cube.getCubeId());
        resp.setName(cube.getName());
        resp.setOwnerUserId(cube.getOwnerUserId());
        resp.setMaxPlayers(cube.getMaxPlayers());
        resp.setCreatedAt(cube.getCreatedAt());
        resp.setMyRole(membership.getRole());
        resp.setMyUserId(actorUserId);

        return ResponseEntity.ok(resp);
    }

    // POST /api/cubes
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateCubeRequest request) {
        try {
            Long actorUserId = (Long) SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal();

            Cube saved = cubeService.createCube(
                    request.getName(),
                    request.getMaxPlayers(),
                    actorUserId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{cubeId}")
    public ResponseEntity<?> updateCube(@PathVariable Long cubeId,
                                        @RequestBody UpdateCubeRequest request) {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        try {
            Cube updated = cubeService.updateCube(
                    cubeId,
                    request.getName(),
                    request.getMaxPlayers(),
                    actorUserId
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{cubeId}")
    public ResponseEntity<?> deleteCube(@PathVariable Long cubeId) {
        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        try {
            cubeService.archiveCube(cubeId, actorUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}