package ygo.draftr.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.CubeEventResponse;
import ygo.draftr.data.CubeEventRepository;
import ygo.draftr.models.CubeEvent;

@RestController
@RequestMapping("/api/cubes/{cubeId}/events")
public class CubeEventController {

    private final CubeEventRepository eventRepo;

    public CubeEventController(CubeEventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    // Example: GET /api/cubes/1/events?page=0&size=25
    @GetMapping
    public ResponseEntity<Page<CubeEventResponse>> getEvents(@PathVariable Long cubeId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "25") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // clamp to 1..100

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<CubeEvent> events = eventRepo.findByCubeIdOrderByCreatedAtDesc(cubeId, pageable);

        Page<CubeEventResponse> mapped = events.map(this::toResponse);

        return ResponseEntity.ok(mapped);
    }

    private CubeEventResponse toResponse(CubeEvent e) {
        CubeEventResponse r = new CubeEventResponse();
        r.setCubeEventId(e.getCubeEventId());
        r.setCubeId(e.getCubeId());
        r.setEventType(e.getEventType());
        r.setActorUserId(e.getActorUserId());
        r.setCreatedAt(e.getCreatedAt());
        r.setSummary(e.getSummary());
        r.setPayload(e.getPayload());
        r.setCardId(e.getCardId());
        r.setTargetUserId(e.getTargetUserId());
        return r;
    }
}
