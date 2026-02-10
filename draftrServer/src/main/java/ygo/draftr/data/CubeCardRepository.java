package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.domain.DraftService;
import ygo.draftr.models.CubeCard;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CubeCardRepository extends JpaRepository<CubeCard, Long> {
    Optional<CubeCard> findByCubeIdAndCardId(Long cubeId, Long cardId);

    boolean existsByCubeIdAndCardId(Long cubeId, Long cardId);

    List<CubeCard> findByCubeId(Long cubeId);

    List<CubeCard> findByCubeIdAndBannedFalse(Long cubeId);

    List<CubeCard> findByCubeIdAndCardIdIn(Long cubeId, Collection<Long> cardIds);

    void deleteByCubeIdAndCardId(Long cubeId, Long cardId);

    List<DraftService.CubeCardQty> findCardQtyByCubeId(Long cubeId);

    @Query("select coalesce(sum(cc.maxQty), 0) from CubeCard cc where cc.cubeId = :cubeId")
    int sumMaxQtyByCubeId(@Param("cubeId") Long cubeId);

    @Query("select count(distinct cc.cardId) from CubeCard cc where cc.cubeId = :cubeId")
    int countDistinctCardIdByCubeId(@Param("cubeId") Long cubeId);

    long countByCubeIdAndBannedTrue(Long cubeId);
}
