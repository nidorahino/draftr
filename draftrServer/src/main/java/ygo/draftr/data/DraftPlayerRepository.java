package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.models.DraftPlayer;

import java.util.List;
import java.util.Optional;

public interface DraftPlayerRepository extends JpaRepository<DraftPlayer, Long> {
    boolean existsByDraftSessionIdAndUserId(Long draftSessionId, Long userId);
    List<DraftPlayer> findByDraftSessionIdOrderBySeatNo(Long draftSessionId);
    long countByDraftSessionId(Long draftSessionId);
    long countByDraftSessionIdAndReadyTrue(Long draftSessionId);
    void deleteByDraftSessionIdAndUserId(Long draftSessionId, Long userId);
    Optional<DraftPlayer> findByDraftSessionIdAndUserId(Long draftSessionId, Long userId);

    @Query("""
        select count(p)
        from DraftPlayer p
        where p.draftSessionId = :draftSessionId
          and p.lastWave = :wave
          and p.lastPickNo = :pickNo
    """)
    int countPickedForWavePick(@Param("draftSessionId") Long draftSessionId,
                               @Param("wave") int wave,
                               @Param("pickNo") int pickNo);
}
