package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.models.DraftPackCard;

import java.util.List;

public interface DraftPackCardRepository extends JpaRepository<DraftPackCard, Long> {

    int countByDraftPackIdAndPickedAtIsNull(Long draftPackId);

    @Query("""
        select count(c)
        from DraftPackCard c
        join DraftPack p on p.draftPackId = c.draftPackId
        where p.draftSessionId = :draftId
          and c.pickedAt is not null
    """)
    long countPickedByDraftId(@Param("draftId") Long draftId);

    @Query("""
        select c
        from DraftPackCard c
        where c.draftPackId = :packId
          and c.pickedAt is null
        order by c.slotNo asc
    """)
    List<DraftPackCard> findUnpickedByPackId(@Param("packId") Long packId);

    @Query("""
        select c
        from DraftPackCard c
        join DraftPack p on p.draftPackId = c.draftPackId
        where p.draftSessionId = :draftId
          and p.currentOwnerSeatNo = :seatNo
          and c.pickedAt is null
        order by p.roundNo asc, p.draftPackId asc, c.slotNo asc
    """)
    List<DraftPackCard> findUnpickedForSeat(@Param("draftId") Long draftId,
                                            @Param("seatNo") int seatNo);

    @Query("""
    select c
    from DraftPackCard c
    where c.draftPackId = :draftPackId
      and c.pickedAt is null
    order by c.slotNo asc
    """)
    List<DraftPackCard> findUnpickedByDraftPackId(@Param("draftPackId") Long draftPackId);

    @Query("""
    select case when count(c) > 0 then true else false end
    from DraftPackCard c
    join DraftPack p on p.draftPackId = c.draftPackId
    where p.draftSessionId = :draftSessionId
      and p.roundNo = :wave
      and c.pickedAt is null
    """)
    boolean existsUnpickedInWave(@Param("draftSessionId") Long draftSessionId,
                                 @Param("wave") int wave);

    @Query("""
    select c
    from DraftPackCard c
    join DraftPack p on p.draftPackId = c.draftPackId
    where p.draftSessionId = :draftId
      and c.pickedByUserId = :userId
    order by c.pickedAt asc
    """)
    List<DraftPackCard> findPickedByUser(@Param("draftId") Long draftId,
                                         @Param("userId") Long userId);

    @Query("""
        select ppc.pickedByUserId as userId, ppc.cardId as cardId, count(ppc) as qty
        from DraftPackCard ppc
        join DraftPack dp on dp.draftPackId = ppc.draftPackId
        where dp.draftSessionId = :draftId
          and ppc.pickedByUserId is not null
        group by ppc.pickedByUserId, ppc.cardId
        """)
    List<PickedCount> countPickedByUserAndCard(@Param("draftId") Long draftId);

    interface PickedCount {
        Long getUserId();
        Long getCardId();
        Long getQty();
    }
}
