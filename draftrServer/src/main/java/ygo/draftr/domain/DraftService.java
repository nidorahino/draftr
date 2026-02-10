package ygo.draftr.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import ygo.draftr.controllers.dto.CreateDraftRequest;
import ygo.draftr.controllers.dto.DraftPlayerResponse;
import ygo.draftr.controllers.dto.MyPackResponse;
import ygo.draftr.data.*;
import ygo.draftr.models.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DraftService {

    private final DraftSessionRepository sessionRepo;
    private final DraftPlayerRepository playerRepo;
    private final DraftPoolCardRepository poolRepo;
    private final DraftPackRepository packRepo;
    private final DraftPackCardRepository packCardRepo;
    private final CubeMemberRepository cubeMemberRepo;
    private final CubeCardRepository cubeCardRepo;
    private final CubeEventRepository eventRepo;
    private final ObjectMapper objectMapper;
    private final CubeRepository cubeRepo;
    private final UserRepository userRepo;
    private final CardRepository cardRepo;
    private final CubeCollectionCardRepository collectionRepo;

    public DraftService(
            DraftSessionRepository sessionRepo,
            DraftPlayerRepository playerRepo,
            DraftPoolCardRepository poolRepo,
            DraftPackRepository packRepo,
            DraftPackCardRepository packCardRepo,
            CubeMemberRepository cubeMemberRepo,
            CubeCardRepository cubeCardRepo,
            CubeEventRepository eventRepo,
            CubeRepository cubeRepo,
            ObjectMapper objectMapper,
            UserRepository userRepo,
            CardRepository cardRepo,
            CubeCollectionCardRepository collectionRepo
    ) {
        this.sessionRepo = sessionRepo;
        this.playerRepo = playerRepo;
        this.poolRepo = poolRepo;
        this.packRepo = packRepo;
        this.packCardRepo = packCardRepo;
        this.cubeMemberRepo = cubeMemberRepo;
        this.cubeCardRepo = cubeCardRepo;
        this.eventRepo = eventRepo;
        this.objectMapper = objectMapper;
        this.cubeRepo = cubeRepo;
        this.userRepo = userRepo;
        this.cardRepo = cardRepo;
        this.collectionRepo = collectionRepo;
    }

    // 1) Create lobby
    @Transactional
    public DraftSession createLobby(Long cubeId, Long actorUserId, CreateDraftRequest req) {
        requireMember(cubeId, actorUserId);

        // Optional: only one lobby per cube
        sessionRepo.findByCubeIdAndStatus(cubeId, DraftStatus.LOBBY)
                .ifPresent(s -> { throw new IllegalStateException("A draft lobby already exists."); });

        validateConfig(cubeId, req);

        DraftSession s = new DraftSession();
        s.setCubeId(cubeId);
        s.setCreatedByUserId(actorUserId);
        s.setStatus(DraftStatus.LOBBY);

        s.setDraftSize(req.getDraftSize());   // per-player
        s.setPackSize(req.getPackSize());

        // legacy columns (if still exist)
        s.setPacksPerPlayer(0);
        s.setNoDuplicates(false);


        s.setCreatedAt(Instant.now());

        s = sessionRepo.save(s);

        // auto-join creator as seat 0
        joinLobby(s.getDraftSessionId(), actorUserId);

        return s;
    }

    // 2) Join lobby
    @Transactional
    public DraftPlayer joinLobby(Long draftSessionId, Long actorUserId) {
        DraftSession s = getSession(draftSessionId);
        requireMember(s.getCubeId(), actorUserId);

        if (s.getStatus() != DraftStatus.LOBBY) {
            throw new IllegalStateException("Draft is not in lobby state.");
        }
        if (playerRepo.existsByDraftSessionIdAndUserId(draftSessionId, actorUserId)) {
            // already joined - return current row
            return playerRepo.findByDraftSessionIdOrderBySeatNo(draftSessionId)
                    .stream()
                    .filter(p -> p.getUserId().equals(actorUserId))
                    .findFirst()
                    .orElseThrow();
        }

        int nextSeat = (int) playerRepo.countByDraftSessionId(draftSessionId);

        DraftPlayer p = new DraftPlayer();
        p.setDraftSessionId(draftSessionId);
        p.setUserId(actorUserId);
        p.setSeatNo(nextSeat);
        p.setReady(false);
        p.setJoinedAt(Instant.now());
        p.setLastWave(0);     // not picked yet
        p.setLastPickNo(-1);  // not picked yet

        p = playerRepo.save(p);

        return p;
    }

    // 3) Toggle ready
    @Transactional
    public DraftPlayer setReady(Long draftSessionId, Long actorUserId, boolean ready) {
        DraftSession s = getSession(draftSessionId);
        if (s.getStatus() != DraftStatus.LOBBY) {
            throw new IllegalStateException("Draft is not in lobby state.");
        }

        DraftPlayer p = playerRepo.findByDraftSessionIdOrderBySeatNo(draftSessionId)
                .stream()
                .filter(x -> x.getUserId().equals(actorUserId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not in draft lobby."));

        p.setReady(ready);
        p = playerRepo.save(p);

        return p;
    }

    // 4) Start draft: generate pool -> create packs -> set RUNNING
    @Transactional
    public void startDraft(Long draftSessionId, Long actorUserId) {
        DraftSession s = getSession(draftSessionId);

        if (!s.getCreatedByUserId().equals(actorUserId)) {
            throw new SecurityException("Only the lobby creator can start the draft.");
        }
        if (s.getStatus() != DraftStatus.LOBBY) {
            throw new IllegalStateException("Draft is not in lobby state.");
        }
        if (poolRepo.existsByDraftSessionId(draftSessionId)) {
            throw new IllegalStateException("Draft already started.");
        }

        List<DraftPlayer> players = playerRepo.findByDraftSessionIdOrderBySeatNo(draftSessionId);
        if (players.size() < 2) {
            throw new IllegalStateException("Need at least 2 players.");
        }
        long readyCount = playerRepo.countByDraftSessionIdAndReadyTrue(draftSessionId);
        if (readyCount != players.size()) {
            throw new IllegalStateException("All players must be ready.");
        }

        // Validate math (recommended strict version)
        int n = players.size();

        // draftSize is PER PLAYER
        int totalCardsNeeded = s.getDraftSize() * n;

        // make sure the cube has enough
        int totalAvailable = cubeCardRepo.sumMaxQtyByCubeId(s.getCubeId());
        if (totalCardsNeeded > totalAvailable) {
            throw new IllegalStateException("Not enough cards in cube pool for this draft.");
        }

        // compute packs per player (number of cycles)
        int packsPerPlayer = (int) Math.ceil((double) s.getDraftSize() / s.getPackSize());

        // store it if you keep the column
        s.setPacksPerPlayer(packsPerPlayer);

       // 4a) Generate pool of exactly totalCardsNeeded
        List<Long> poolCardIds = generateDraftPoolCardIds(s.getCubeId(), totalCardsNeeded);


        List<DraftPoolCard> poolRows = new ArrayList<>(poolCardIds.size());
        Instant now = Instant.now();
        for (int i = 0; i < poolCardIds.size(); i++) {
            DraftPoolCard pc = new DraftPoolCard();
            pc.setDraftSessionId(draftSessionId);
            pc.setSeq(i);
            pc.setCardId(poolCardIds.get(i));
            pc.setCreatedAt(now);
            poolRows.add(pc);
        }
        poolRepo.saveAll(poolRows);

        // 4b) Create packs and assign cards
        createPacksAndCards(s, players, poolRows);

        s.setStatus(DraftStatus.RUNNING);
        s.setStartedAt(Instant.now());

        // round-based state
        s.setCurrentWave(1);     // waves start at 1 (matches your pack.roundNo)
        s.setCurrentPickNo(0);   // pick index within wave

        sessionRepo.save(s);
    }

    // ---------------- helpers ----------------

    private DraftSession getSession(Long draftSessionId) {
        return sessionRepo.findById(draftSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Draft session not found."));
    }

    private void requireMember(Long cubeId, Long userId) {
        if (!cubeMemberRepo.existsByCubeIdAndUserId(cubeId, userId)) {
            throw new SecurityException("Not a member of this cube.");
        }
    }

    private void validateConfig(Long cubeId, CreateDraftRequest req) {

        if (req == null) throw new IllegalArgumentException("Request body is required.");

        if (req.getDraftSize() <= 0) {
            throw new IllegalArgumentException("draftSize must be > 0");
        }

        if (req.getPackSize() <= 0) {
            throw new IllegalArgumentException("packSize must be > 0");
        }

        // This is a light check. Real check happens on startDraft once player count is known.
        int totalAvailable = cubeCardRepo.sumMaxQtyByCubeId(cubeId);

        // At minimum, a 2-player draft needs 2 * draftSize cards total.
        // But since we don't know player count here, keep it simple:
        if (req.getDraftSize() >= totalAvailable) {
            throw new IllegalArgumentException("draftSize is too large for this cube pool.");
        }
    }

    private int countTotalCardsAvailable(Long cubeId, boolean noDuplicates) {
        // You implement based on your cubeCard schema:
        // - duplicates allowed: sum(maxQty)
        // - noDuplicates: count(distinct cardId)
        if (noDuplicates) {
            return cubeCardRepo.countDistinctCardIdByCubeId(cubeId);
        }
        return cubeCardRepo.sumMaxQtyByCubeId(cubeId);
    }

    private List<Long> generateDraftPoolCardIds(Long cubeId, int totalCardsNeeded) {

        List<CubeCardQty> pool = cubeCardRepo.findCardQtyByCubeId(cubeId);

        List<Long> bag = new ArrayList<>();
        for (CubeCardQty c : pool) {
            for (int i = 0; i < c.maxQty(); i++) bag.add(c.cardId());
        }

        if (totalCardsNeeded > bag.size()) {
            throw new IllegalArgumentException("Not enough cards in cube pool for this draft.");
        }

        Collections.shuffle(bag);
        return bag.subList(0, totalCardsNeeded);
    }


    private void createPacksAndCards(
            DraftSession s,
            List<DraftPlayer> players,
            List<DraftPoolCard> poolRows
    ) {
        int n = players.size();

        // draftSize is PER PLAYER (as you described)
        int draftSizePerPlayer = s.getDraftSize();
        int packSize = s.getPackSize();

        // total cards that must be drafted across table
        int totalNeeded = n * draftSizePerPlayer;

        if (poolRows.size() < totalNeeded) {
            throw new IllegalStateException("Not enough pool cards to satisfy draft. Need " + totalNeeded);
        }

        // Shuffle poolRows for distribution
        List<DraftPoolCard> shuffled = new ArrayList<>(poolRows);
        Collections.shuffle(shuffled);

        // Each "wave" creates N packs (one per player).
        // Each wave contributes up to N * packSize cards.
        int cardsPerWave = n * packSize;
        int waves = (int) Math.ceil(totalNeeded / (double) cardsPerWave);

        Instant now = Instant.now();

        List<DraftPack> packsToSave = new ArrayList<>(waves * n);

        // Create packs
        for (int wave = 1; wave <= waves; wave++) {
            PassDirection dir = (wave % 2 == 1) ? PassDirection.LEFT : PassDirection.RIGHT;

            for (int seat = 0; seat < n; seat++) {
                DraftPack pack = new DraftPack();
                pack.setDraftSessionId(s.getDraftSessionId());

                // reuse roundNo as "wave number"
                pack.setRoundNo(wave);

                pack.setInitialOwnerSeatNo(seat);
                pack.setCurrentOwnerSeatNo(seat);

                pack.setDirection(dir);
                pack.setCreatedAt(now);

                packsToSave.add(pack);
            }
        }

        packsToSave = packRepo.saveAll(packsToSave);

        // Assign cards into pack slots (packs are in wave order, seat order)
        List<DraftPackCard> packCards = new ArrayList<>();

        int idx = 0; // index into shuffled poolRows

        for (DraftPack pack : packsToSave) {
            // remaining cards we still need to allocate into packs
            int remainingNeeded = totalNeeded - idx;
            if (remainingNeeded <= 0) break;

            // this pack's capacity is up to packSize, but last packs can be smaller
            int thisPackCount = Math.min(packSize, remainingNeeded);

            for (int slot = 0; slot < thisPackCount; slot++) {
                DraftPoolCard pc = shuffled.get(idx++);

                DraftPackCard ppc = new DraftPackCard();
                ppc.setDraftPackId(pack.getDraftPackId());
                ppc.setSlotNo(slot);
                ppc.setDraftPoolCardId(pc.getDraftPoolCardId());
                ppc.setCardId(pc.getCardId());

                packCards.add(ppc);
            }
        }

        packCardRepo.saveAll(packCards);
    }

    private void audit(Long cubeId, Long actorUserId, Long draftSessionId, String type, Object payloadObj) {
        if (eventRepo == null) return;

        try {
            CubeEvent e = new CubeEvent();
            e.setCubeId(cubeId);
            e.setActorUserId(actorUserId);
            e.setEventType(type);

            // Store draftSessionId inside payload since CubeEvent doesn't have a column for it
            Map<String, Object> payload = new HashMap<>();
            payload.put("draftSessionId", draftSessionId);
            payload.put("data", payloadObj);

            e.setPayload(objectMapper.writeValueAsString(payload));
            e.setCreatedAt(Instant.now());

            eventRepo.save(e);
        } catch (JsonProcessingException ex) {
            // don't kill draft over audit serialization
        }
    }

    @Transactional(readOnly = true)
    public DraftSession getSessionForMember(Long draftSessionId, Long actorUserId) {
        DraftSession s = sessionRepo.findById(draftSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Draft session not found."));
        requireMember(s.getCubeId(), actorUserId);
        return s;
    }

    @Transactional(readOnly = true)
    public List<DraftPlayer> getPlayers(Long draftSessionId, Long actorUserId) {
        DraftSession s = getSessionForMember(draftSessionId, actorUserId);
        return playerRepo.findByDraftSessionIdOrderBySeatNo(s.getDraftSessionId());
    }

    @Transactional
    public void leaveLobby(Long draftSessionId, Long actorUserId) {
        DraftSession s = getSessionForMember(draftSessionId, actorUserId);
        if (s.getStatus() != DraftStatus.LOBBY) {
            throw new IllegalStateException("Cannot leave after draft starts.");
        }
        playerRepo.deleteByDraftSessionIdAndUserId(draftSessionId, actorUserId);
        audit(s.getCubeId(), actorUserId, draftSessionId, "DRAFT_PLAYER_LEFT", null);
    }

    @Transactional(readOnly = true)
    public List<DraftSession> getOpenSessions(Long cubeId, Long actorUserId) {
        requireMember(cubeId, actorUserId); // members can see/join
        return sessionRepo.findByCubeIdAndStatusIn(
                cubeId, List.of(DraftStatus.LOBBY, DraftStatus.RUNNING)
        );
    }

    @Transactional
    public void cancelDraft(Long draftSessionId, Long actorUserId) {
        DraftSession s = sessionRepo.findById(draftSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Draft session not found."));

        boolean isCreator = s.getCreatedByUserId().equals(actorUserId);

        if (!isCreator) {
            requireOwnerOrAdmin(s.getCubeId(), actorUserId);
        }

        if (s.getStatus() == DraftStatus.CANCELLED || s.getStatus() == DraftStatus.COMPLETED) {
            return;
        }

        s.setStatus(DraftStatus.CANCELLED);
        s.setEndedAt(Instant.now());
        sessionRepo.save(s);
    }

    private void requireOwnerOrAdmin(Long cubeId, Long actorUserId) {
        Cube cube = cubeRepo.findById(cubeId)
                .orElseThrow(() -> new IllegalArgumentException("Cube not found."));

        // owner stored on cube table
        if (cube.getOwnerUserId().equals(actorUserId)) {
            return;
        }

        // otherwise must be ADMIN member row
        CubeMember m = cubeMemberRepo.findByCubeIdAndUserId(cubeId, actorUserId)
                .orElseThrow(() -> new SecurityException("Not a member of this cube."));

        if (!"ADMIN".equalsIgnoreCase(m.getRole())) {
            throw new SecurityException("Owner/admin only.");
        }
    }

    @Transactional(readOnly = true)
    public List<DraftPlayerResponse> getPlayerResponses(Long draftId, Long actorId) {

        DraftSession s = getSessionForMember(draftId, actorId);

        List<DraftPlayer> players =
                playerRepo.findByDraftSessionIdOrderBySeatNo(s.getDraftSessionId());

        List<Long> ids = players.stream()
                .map(DraftPlayer::getUserId)
                .toList();

        Map<Long, String> names =
                userRepo.findUsernamesByIds(ids);

        return players.stream().map(p -> {
            DraftPlayerResponse r = new DraftPlayerResponse();
            r.setUserId(p.getUserId());
            r.setSeatNo(p.getSeatNo());
            r.setReady(p.isReady());
            r.setUsername(names.get(p.getUserId()));
            return r;
        }).toList();
    }

    @Transactional(readOnly = true)
    public MyPackResponse getMyCurrentPack(Long draftId, Long actorUserId) {

        DraftSession s = getSessionForMember(draftId, actorUserId);
        if (s.getStatus() != DraftStatus.RUNNING) {
            throw new IllegalStateException("Draft is not running.");
        }

        DraftPlayer me = playerRepo.findByDraftSessionIdAndUserId(draftId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Not a player in this draft."));

        int seatNo = me.getSeatNo();

        int wave = s.getCurrentWave();

        // ✅ if I already picked this round, I should see "waiting" (empty pack)
        if (me.getLastWave() == wave && me.getLastPickNo() == s.getCurrentPickNo()) {
            MyPackResponse res = new MyPackResponse();
            res.setDraftPackId(null);
            res.setCards(List.of());
            res.setRoundNo(wave);
            res.setDirection(null);
            return res;
        }

        // ✅ find my currently owned pack IN THIS WAVE with unpicked cards
        DraftPack pack = packRepo.findCurrentPackForSeatInWave(draftId, seatNo, wave)
                .orElse(null);

        if (pack == null) {
            MyPackResponse res = new MyPackResponse();
            res.setDraftPackId(null);
            res.setCards(List.of());
            res.setRoundNo(wave);
            res.setDirection(null);
            return res;
        }

        List<DraftPackCard> cardsForPack =
                packCardRepo.findUnpickedByDraftPackId(pack.getDraftPackId());

        // safety
        if (cardsForPack.isEmpty()) {
            MyPackResponse res = new MyPackResponse();
            res.setDraftPackId(null);
            res.setCards(List.of());
            res.setRoundNo(wave);
            res.setDirection(pack.getDirection().name());
            return res;
        }

        List<Long> cardIds = cardsForPack.stream()
                .map(DraftPackCard::getCardId)
                .distinct()
                .toList();

        Map<Long, CardRepository.CardDetails> detailsById =
                cardRepo.findDetailsByIds(cardIds)
                        .stream()
                        .collect(Collectors.toMap(
                                CardRepository.CardDetails::getId,
                                d -> d
                        ));

        MyPackResponse res = new MyPackResponse();
        res.setDraftPackId(pack.getDraftPackId());
        res.setRoundNo(pack.getRoundNo());
        res.setDirection(pack.getDirection().name());

        List<MyPackResponse.MyPackCard> cards =
                cardsForPack.stream().map(c -> {
                    MyPackResponse.MyPackCard dto = new MyPackResponse.MyPackCard();
                    dto.setDraftPackCardId(c.getDraftPackCardId());
                    dto.setCardId(c.getCardId());
                    dto.setSlotNo(c.getSlotNo());

                    var d = detailsById.get(c.getCardId());
                    if (d != null) {
                        dto.setName(d.getName());
                        dto.setImageUrl(d.getImageUrl());
                        dto.setDescription(d.getDescription());
                        dto.setHumanReadableCardType(d.getHumanReadableCardType());
                        dto.setAtk(d.getAtk());
                        dto.setDef(d.getDef());
                        dto.setLevel(d.getLevel());
                    }

                    return dto;
                }).toList();

        res.setCards(cards);
        return res;
    }

    @Transactional
    public void pickCard(Long draftId, Long actorUserId, Long draftPackCardId) {

        DraftSession s = getSessionForMember(draftId, actorUserId);
        if (s.getStatus() != DraftStatus.RUNNING) {
            throw new IllegalStateException("Draft is not running.");
        }

        DraftPlayer me = playerRepo.findByDraftSessionIdAndUserId(draftId, actorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Not a player in this draft."));

        int seatNo = me.getSeatNo();

        int wave = s.getCurrentWave();
        int pickNo = s.getCurrentPickNo();

        // ✅ barrier: only one pick per round
        if (me.getLastWave() == wave && me.getLastPickNo() == pickNo) {
            throw new IllegalStateException("You already picked this round.");
        }

        DraftPackCard ppc = packCardRepo.findById(draftPackCardId)
                .orElseThrow(() -> new IllegalArgumentException("Pack card not found."));

        if (ppc.getPickedAt() != null) {
            throw new IllegalStateException("Card already picked.");
        }

        DraftPack pack = packRepo.lockById(ppc.getDraftPackId())
                .orElseThrow(() -> new IllegalArgumentException("Pack not found."));

        if (!pack.getDraftSessionId().equals(draftId)) {
            throw new SecurityException("Wrong draft.");
        }

        // ✅ only pick from current wave packs
        if (pack.getRoundNo() != wave) {
            throw new IllegalStateException("Not the current wave.");
        }

        // ✅ must be your currently owned pack
        if (pack.getCurrentOwnerSeatNo() != seatNo) {
            throw new SecurityException("Not your pack.");
        }

        // ✅ enforce "current pack" for this seat+wave (prevents picking from a different owned pack)
        DraftPack currentPack = packRepo.findCurrentPackForSeatInWave(draftId, seatNo, wave)
                .orElseThrow(() -> new IllegalStateException("No available pack to pick from."));

        if (!pack.getDraftPackId().equals(currentPack.getDraftPackId())) {
            throw new IllegalStateException("You must pick from your current pack.");
        }

        // ✅ mark picked (consider making this an atomic update; see section B)
        Instant now = Instant.now();
        ppc.setPickedAt(now);
        ppc.setPickedByUserId(actorUserId);
        packCardRepo.save(ppc);

        // ✅ record that player has picked for (wave, pickNo)
        me.setLastWave(wave);
        me.setLastPickNo(pickNo);
        playerRepo.save(me);

        // ✅ rotate only when everyone picked
        // Important: pass IDs, not a possibly-stale session object
        maybeAdvanceRoundOrWave(draftId);

        // ✅ completion
        long pickedTotal = packCardRepo.countPickedByDraftId(draftId);
        int playerCount = (int) playerRepo.countByDraftSessionId(draftId);
        long totalNeeded = (long) s.getDraftSize() * playerCount;

        if (pickedTotal >= totalNeeded) {

            // Refresh + lock latest state
            DraftSession s2 = getSession(draftId);

            // Only finalize once
            if (s2.getStatus() != DraftStatus.COMPLETED) {
                s2.setStatus(DraftStatus.COMPLETED);
                s2.setEndedAt(Instant.now());
                sessionRepo.save(s2);
            }
            // Apply picks to collections (safe to call multiple times)
            applyDraftToCollectionsIfNeeded(s2);
            return;
        }
    }

    private void maybeAdvanceRoundOrWave(Long draftId) {

        DraftSession s = sessionRepo.lockById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Draft session not found."));

        int wave = s.getCurrentWave();
        int pickNo = s.getCurrentPickNo();

        int playerCount = (int) playerRepo.countByDraftSessionId(draftId);

        int pickedThisRound = playerRepo.countPickedForWavePick(draftId, wave, pickNo);
        if (pickedThisRound < playerCount) {
            return;
        }

        List<DraftPack> packs =
                packRepo.findByDraftSessionIdAndRoundNoOrderByDraftPackId(draftId, wave);

        int n = playerCount;

        for (DraftPack pack : packs) {
            int remaining = packCardRepo.countByDraftPackIdAndPickedAtIsNull(pack.getDraftPackId());
            if (remaining <= 0) continue;

            int seat = pack.getCurrentOwnerSeatNo();
            int nextSeat = (pack.getDirection() == PassDirection.LEFT)
                    ? (seat + 1) % n
                    : (seat - 1 + n) % n;

            pack.setCurrentOwnerSeatNo(nextSeat);
        }

        packRepo.saveAll(packs);

        s.setCurrentPickNo(pickNo + 1);

        boolean waveHasCards = packCardRepo.existsUnpickedInWave(draftId, wave);
        if (!waveHasCards) {
            s.setCurrentWave(wave + 1);
            s.setCurrentPickNo(0);
        }

        sessionRepo.save(s);
    }

    @Transactional(readOnly = true)
    public List<MyPackResponse.MyPackCard> getMyPicks(Long draftId, Long actorUserId) {

        DraftSession s = getSessionForMember(draftId, actorUserId);

        // get picked pack cards
        List<DraftPackCard> picked = packCardRepo.findPickedByUser(draftId, actorUserId);

        if (picked.isEmpty()) return List.of();

        List<Long> cardIds = picked.stream()
                .map(DraftPackCard::getCardId)
                .distinct()
                .toList();

        Map<Long, CardRepository.CardDetails> detailsById =
                cardRepo.findDetailsByIds(cardIds)
                        .stream()
                        .collect(Collectors.toMap(CardRepository.CardDetails::getId, d -> d));

        return picked.stream().map(c -> {
            MyPackResponse.MyPackCard dto = new MyPackResponse.MyPackCard();
            dto.setDraftPackCardId(c.getDraftPackCardId());
            dto.setCardId(c.getCardId());
            dto.setSlotNo(c.getSlotNo());

            var d = detailsById.get(c.getCardId());
            if (d != null) {
                dto.setName(d.getName());
                dto.setImageUrl(d.getImageUrl());
                dto.setDescription(d.getDescription());
                dto.setHumanReadableCardType(d.getHumanReadableCardType());
                dto.setAtk(d.getAtk());
                dto.setDef(d.getDef());
                dto.setLevel(d.getLevel());
            }
            return dto;
        }).toList();
    }

    // helper for saving draft
    @Transactional
    protected void applyDraftToCollectionsIfNeeded(DraftSession s) {
        if (s.getCollectionsAppliedAt() != null) return;

        Long draftId = s.getDraftSessionId();
        Long cubeId = s.getCubeId();

        // rows: (userId, cardId, qty)
        var pickedCounts = packCardRepo.countPickedByUserAndCard(draftId);

        // Apply to collections
        for (var row : pickedCounts) {
            Long userId = row.getUserId();
            Long cardId = row.getCardId();
            int qty = row.getQty().intValue();
            collectionRepo.addToCollection(cubeId, userId, cardId, qty);
        }

        // mark applied
        Instant appliedAt = Instant.now();
        s.setCollectionsAppliedAt(appliedAt);
        sessionRepo.save(s);

        // Build ONE audit payload with "draft completed + applied"
        Map<Long, List<Map<String, Object>>> byUser = new LinkedHashMap<>();
        for (var row : pickedCounts) {
            byUser.computeIfAbsent(row.getUserId(), k -> new ArrayList<>())
                    .add(Map.of(
                            "cardId", row.getCardId(),
                            "qtyAdded", row.getQty()
                    ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("draftId", draftId);
        payload.put("cubeId", cubeId);
        payload.put("status", "COMPLETED");
        payload.put("appliedAt", appliedAt.toString());
        payload.put("applied", byUser);

        // actor: lobby creator (or you can use 0/system if you want)
        audit(cubeId, s.getCreatedByUserId(), draftId, "DRAFT_COMPLETED_APPLIED", payload);
    }

    // simple projection record
    public record CubeCardQty(Long cardId, int maxQty) {}
}
