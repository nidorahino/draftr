package ygo.draftr.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.CubeCardPoolItemResponse;
import ygo.draftr.data.CubeCardRepository;
import ygo.draftr.data.CubeMemberRepository;
import ygo.draftr.models.CubeCard;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/cubes/{cubeId}/cards")
public class CubeCardListController {

    private final CubeMemberRepository memberRepo;
    private final CubeCardRepository cubeCardRepo;

    public CubeCardListController(CubeMemberRepository memberRepo,
                                  CubeCardRepository cubeCardRepo) {
        this.memberRepo = memberRepo;
        this.cubeCardRepo = cubeCardRepo;
    }

    // GET /api/cubes/1/cards?actorUserId=2&includeBanned=true
    // If includeBanned=false, returns only not-banned
    @GetMapping
    public ResponseEntity<?> listCubeCards(@PathVariable Long cubeId,
                                           @RequestParam(defaultValue = "true") boolean includeBanned) {

        Long actorUserId = (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (!memberRepo.existsByCubeIdAndUserId(cubeId, actorUserId)) {
            return ResponseEntity.badRequest().body("actor is not a member of this cube");
        }

        List<CubeCard> rows = includeBanned
                ? cubeCardRepo.findByCubeId(cubeId)
                : cubeCardRepo.findByCubeIdAndBannedFalse(cubeId);

        List<CubeCardPoolItemResponse> resp = new ArrayList<>();
        for (CubeCard row : rows) {
            CubeCardPoolItemResponse r = new CubeCardPoolItemResponse();
            r.setCubeId(row.getCubeId());
            r.setCardId(row.getCardId());
            r.setMaxQty(row.getMaxQty());
            r.setBanned(row.isBanned());
            resp.add(r);
        }

        return ResponseEntity.ok(resp);
    }
}