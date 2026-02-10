package ygo.draftr.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ygo.draftr.controllers.dto.CardDetailResponse;
import ygo.draftr.controllers.dto.CardFiltersResponse;
import ygo.draftr.controllers.dto.CardSearchResult;
import ygo.draftr.data.CardRepository;
import ygo.draftr.data.CardSpecifications;
import ygo.draftr.models.Card;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardRepository cardRepo;

    @Value("${app.images.base-url}")
    private String imagesBaseUrl;

    public CardController(CardRepository cardRepo) {
        this.cardRepo = cardRepo;
    }

    // GET /api/cards/archetypes
    @GetMapping("/archetypes")
    public ResponseEntity<?> archetypes() {
        return ResponseEntity.ok(cardRepo.findDistinctArchetypes());
    }

    // GET /api/cards/races
    @GetMapping("/races")
    public ResponseEntity<?> races() {
        return ResponseEntity.ok(cardRepo.findDistinctRaces());
    }

    // GET /api/cards/attributes
    @GetMapping("/attributes")
    public ResponseEntity<?> attributes() {
        return ResponseEntity.ok(cardRepo.findDistinctAttributes());
    }

    // GET /api/cards/types
    @GetMapping("/types")
    public ResponseEntity<?> types() {
        return ResponseEntity.ok(cardRepo.findDistinctCardTypes());
    }

    // GET /api/cards/86988864
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return cardRepo.findById(id)
                .<ResponseEntity<?>>map(c -> ResponseEntity.ok(
                        new CardDetailResponse(
                                c.getId(),
                                c.getName(),
                                c.getCardType(),
                                c.getHumanReadableCardType(),
                                c.getFrameType(),
                                c.getDescription(),
                                c.getRace(),
                                c.getArchetype(),
                                c.getTypeline(),
                                c.getAttribute(),
                                c.getLevel(),
                                c.getAtk(),
                                c.getDef(),
                                c.getYgoprodeckUrl(),
                                toPublicImageUrl(c.getImageUrl())
                        )
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // GET /api/cards/filters
    @GetMapping("/filters")
    public ResponseEntity<?> filters() {
        CardFiltersResponse r = new CardFiltersResponse();
        r.setArchetypes(cardRepo.findDistinctArchetypes());
        r.setRaces(cardRepo.findDistinctRaces());
        r.setAttributes(cardRepo.findDistinctAttributes());
        r.setTypes(cardRepo.findDistinctCardTypes());
        return ResponseEntity.ok(r);
    }

    // GET /api/cards/search?name=blue&page=0&size=25
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String name,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "25") int size) {

        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 2) {
            return ResponseEntity.badRequest().body("name must be at least 2 characters");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Card> cards = cardRepo.findByNameContainingIgnoreCase(trimmed, pageable);

        Page<CardSearchResult> resp = cards.map(this::toSearchResult);

        return ResponseEntity.ok(resp);
    }

    // GET /api/cards?page=0&size=24&query=blue&cardType=Effect%20Monster&attribute=DARK
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String cardType,
            @RequestParam(required = false) String archetype,
            @RequestParam(required = false) String race,
            @RequestParam(required = false) String attribute,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {

        // ✅ Enforce min length = 3
        if (query != null) {
            query = query.trim();

            if (!query.isEmpty() && query.length() < 3) {
                return ResponseEntity.badRequest()
                        .body("query must be at least 3 characters");
            }

            if (query.isEmpty()) {
                query = null;
            }
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Specification<Card> spec = Specification.where(
                        CardSpecifications.queryLike(query)
                )
                .and(CardSpecifications.cardTypeEquals(cardType))
                .and(CardSpecifications.archetypeEquals(archetype))
                .and(CardSpecifications.raceEquals(race))
                .and(CardSpecifications.attributeEquals(attribute));

        Page<CardSearchResult> resp =
                cardRepo.findAll(spec, pageable).map(this::toSearchResult);

        return ResponseEntity.ok(resp);
    }

    private String toPublicImageUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) return null;

        // already a full URL
        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) return keyOrUrl;

        String base = imagesBaseUrl.endsWith("/")
                ? imagesBaseUrl.substring(0, imagesBaseUrl.length() - 1)
                : imagesBaseUrl;

        String key = keyOrUrl.startsWith("/") ? keyOrUrl.substring(1) : keyOrUrl;

        return base + "/" + key;
    }


    private CardSearchResult toSearchResult(Card c) {
        CardSearchResult r = new CardSearchResult();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setCardType(c.getCardType());
        r.setHumanReadableCardType(c.getHumanReadableCardType());
        r.setArchetype(c.getArchetype());
        r.setImageUrl(c.getImageUrl());
        return r;
    }

}
