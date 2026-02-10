package ygo.draftr.data;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.models.DraftPack;

import java.util.List;
import java.util.Optional;

public interface DraftPackRepository extends JpaRepository<DraftPack, Long> {
    List<DraftPack> findByDraftSessionIdAndRoundNoOrderByInitialOwnerSeatNo(Long draftSessionId, Integer roundNo);

    @Query("""
    select p
    from DraftPack p
    where p.draftSessionId = :draftId
      and p.currentOwnerSeatNo = :seatNo
    order by p.roundNo asc, p.draftPackId asc
    """)
    List<DraftPack> findPacksForSeat(@Param("draftId") Long draftId,
                                     @Param("seatNo") int seatNo);


    @Query("""
    select p
    from DraftPack p
    where p.draftSessionId = :draftSessionId
      and p.roundNo = :wave
      and p.currentOwnerSeatNo = :seatNo
      and exists (
          select 1 from DraftPackCard c
          where c.draftPackId = p.draftPackId
            and c.pickedAt is null
      )
    order by p.draftPackId asc
    """)
    Optional<DraftPack> findCurrentPackForSeatInWave(@Param("draftSessionId") Long draftSessionId,
                                                     @Param("seatNo") int seatNo,
                                                     @Param("wave") int wave);

    List<DraftPack> findByDraftSessionIdAndRoundNoOrderByDraftPackId(Long draftSessionId, int roundNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select p
    from DraftPack p
    where p.draftPackId = :packId
    """)
    Optional<DraftPack> lockById(@Param("packId") Long packId);

}