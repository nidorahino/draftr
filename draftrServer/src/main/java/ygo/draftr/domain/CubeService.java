package ygo.draftr.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.*;
import ygo.draftr.models.*;

import java.time.Instant;
import java.util.*;

@Service
public class CubeService {

    private final CubeRepository cubeRepo;
    private final CubeMemberRepository memberRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;
    private final CubeCardRepository cubeCardRepo;
    private final CubeCollectionCardRepository collectionRepo;

    public CubeService(CubeRepository cubeRepo,
                       CubeMemberRepository memberRepo,
                       CubeEventRepository eventRepo,
                       CubeCardRepository cubeCardRepo,
                       CubeCollectionCardRepository collectionRepo,
                       ObjectMapper objectMapper) {
        this.cubeRepo = cubeRepo;
        this.memberRepo = memberRepo;
        this.eventRepo = eventRepo;
        this.cubeCardRepo = cubeCardRepo;
        this.collectionRepo = collectionRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Cube createCube(String name, Integer maxPlayers, Long actorUserId) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("cube name must be at least 3 characters");
        }

        int mp;
        if (maxPlayers == null) {
            mp = 8; // default
        } else if (maxPlayers < 2 || maxPlayers > 16) {
            throw new IllegalArgumentException("maxPlayers must be between 2 and 16");
        } else {
            mp = maxPlayers;
        }

        Cube cube = new Cube();
        cube.setName(trimmed);
        cube.setOwnerUserId(actorUserId);
        cube.setMaxPlayers(mp);

        Cube saved = cubeRepo.save(cube);

        CubeMember owner = new CubeMember();
        owner.setCubeId(saved.getCubeId());
        owner.setUserId(actorUserId);
        owner.setRole("OWNER");
        memberRepo.save(owner);

        return saved;
    }

    @Transactional
    public Cube updateCube(Long cubeId, String name, Integer maxPlayers, Long actorUserId) {

        // must be a member
        CubeMember membership = memberRepo.findByCubeIdAndUserId(cubeId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("actor is not a member of this cube"));

        // only OWNER can edit settings (change later if you add ADMIN)
        if (!isOwnerOrAdmin(membership)) {
            throw new IllegalArgumentException("only OWNER or ADMIN can edit cube settings");
        }

        Cube cube = cubeRepo.findById(cubeId)
                .orElseThrow(() -> new IllegalArgumentException("cube not found"));

        // validate + apply name (if provided)
        if (name != null) {
            String trimmed = name.trim();
            if (trimmed.length() < 3) {
                throw new IllegalArgumentException("cube name must be at least 3 characters");
            }
            cube.setName(trimmed);
        }

        // validate + apply maxPlayers (if provided)
        if (maxPlayers != null) {
            if (maxPlayers < 2 || maxPlayers > 16) {
                throw new IllegalArgumentException("maxPlayers must be between 2 and 16");
            }
            cube.setMaxPlayers(maxPlayers);
        }

        return cubeRepo.save(cube);
    }

    @Transactional
    public void archiveCube(Long cubeId, Long actorUserId) {

        CubeMember membership = memberRepo.findByCubeIdAndUserId(cubeId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("actor is not a member of this cube"));

        if (!isOwnerOrAdmin(membership)) {
            throw new IllegalArgumentException("only OWNER or ADMIN can archive the cube");
        }

        Cube cube = cubeRepo.findById(cubeId)
                .orElseThrow(() -> new IllegalArgumentException("cube not found"));

        if (cube.isArchived()) {
            return; // idempotent
        }

        cube.setArchived(true);
        cube.setArchivedAt(Instant.now());
        cubeRepo.save(cube);
    }

    @Transactional
    public void claimLoss(Long cubeId, Long loserUserId, Long winnerUserId) {
        if (winnerUserId == null) {
            throw new IllegalArgumentException("winnerUserId is required");
        }
        if (winnerUserId.equals(loserUserId)) {
            throw new IllegalArgumentException("winner cannot be the loser");
        }

        // membership validation
        requireMembership(cubeId, loserUserId);
        CubeMember winnerMembership = requireMembership(cubeId, winnerUserId);

        // increment winner wins (MVP behavior)
        winnerMembership.setWins(winnerMembership.getWins() + 1);
        memberRepo.save(winnerMembership);

        // audit
        logEvent(
                cubeId,
                "ROUND_LOSS_CLAIMED",
                loserUserId,
                "Loss claimed; winner selected",
                new java.util.LinkedHashMap<String, Object>() {{
                    put("loserUserId", loserUserId);
                    put("winnerUserId", winnerUserId);
                    put("winnerWinsAfter", winnerMembership.getWins());
                }}
        );
    }

    @Transactional
    public void claimWin(Long cubeId, Long winnerUserId) {
        requireMembership(cubeId, winnerUserId);

        // MVP: just record that winner came to claim (later: wheel spin)
        logEvent(
                cubeId,
                "ROUND_WIN_CLAIMED",
                winnerUserId,
                "Winner claimed reward",
                new java.util.LinkedHashMap<String, Object>() {{
                    put("winnerUserId", winnerUserId);
                }}
        );
    }

    public Cube requireCube(Long cubeId) {
        return cubeRepo.findById(cubeId)
                .orElseThrow(() -> new IllegalArgumentException("cube not found"));
    }

    public CubeMember requireMembership(Long cubeId, Long userId) {
        return memberRepo.findByCubeIdAndUserId(cubeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("actor is not a member of this cube"));
    }

    public boolean isOwner(Long cubeId, Long userId) {
        Optional<CubeMember> m = memberRepo.findByCubeIdAndUserId(cubeId, userId);
        return m.isPresent() && "OWNER".equalsIgnoreCase(m.get().getRole());
    }

    private static boolean isOwnerOrAdmin(CubeMember m) {
        String role = m.getRole();
        return "OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    private List<Long> addRandomNonBannedCardsToCollection(Long cubeId, Long userId, int count) {
        List<CubeCard> pool = cubeCardRepo.findByCubeIdAndBannedFalse(cubeId);
        if (pool.isEmpty()) return List.of();

        Collections.shuffle(pool);

        List<Long> added = new ArrayList<>();
        for (CubeCard cc : pool) {
            if (added.size() >= count) break;
            Long cardId = cc.getCardId();

            // Option A bans: banned cards are excluded already.
            // Add 1 copy to collection (you should still enforce max 3 later if needed)
            CubeCollectionCard row = collectionRepo
                    .findByCubeIdAndUserIdAndCardId(cubeId, userId, cardId)
                    .orElseGet(() -> {
                        CubeCollectionCard n = new CubeCollectionCard();
                        n.setCubeId(cubeId);
                        n.setUserId(userId);
                        n.setCardId(cardId);
                        n.setQty(0);
                        // ensure your entity sets updatedAt/createdAt properly
                        return n;
                    });

            row.setQty(row.getQty() + 1);
            collectionRepo.save(row);

            added.add(cardId);
        }

        return added;
    }

    private void logEvent(Long cubeId, String type, Long actorUserId, String summary, Object payloadObj) {
        try {
            CubeEvent e = new CubeEvent();
            e.setCubeId(cubeId);
            e.setEventType(type);
            e.setActorUserId(actorUserId);
            e.setSummary(summary);
            e.setPayload(objectMapper.writeValueAsString(payloadObj));
            eventRepo.save(e);
        } catch (Exception ex) {
            throw new RuntimeException("failed to write cube event", ex);
        }
    }

    private String extractSpinKey(String payloadJson) {
        return extractString(payloadJson, "spinKey");
    }

    private String extractString(String payloadJson, String key) {
        if (payloadJson == null || payloadJson.isBlank()) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = objectMapper.readValue(payloadJson, Map.class);
            Object v = m.get(key);
            return v == null ? null : String.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractLong(String payloadJson, String key) {
        String s = extractString(payloadJson, key);
        if (s == null) return null;
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
