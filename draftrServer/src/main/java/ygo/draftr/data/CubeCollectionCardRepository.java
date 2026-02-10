package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.models.CubeCollectionCard;

import java.util.List;
import java.util.Optional;

public interface CubeCollectionCardRepository extends JpaRepository<CubeCollectionCard, Long> {

    Optional<CubeCollectionCard> findByCubeIdAndUserIdAndCardId(Long cubeId, Long userId, Long cardId);

    List<CubeCollectionCard> findByCubeIdAndUserId(Long cubeId, Long userId);

    void deleteByCubeIdAndUserIdAndCardId(Long cubeId, Long userId, Long cardId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO cube_collection_card (cube_id, user_id, card_id, qty, updated_at)
        VALUES (:cubeId, :userId, :cardId, :qty, now())
        ON CONFLICT (cube_id, user_id, card_id)
        DO UPDATE SET
            qty = cube_collection_card.qty + EXCLUDED.qty,
            updated_at = now()
        """, nativeQuery = true)
    void addToCollection(@Param("cubeId") Long cubeId,
                         @Param("userId") Long userId,
                         @Param("cardId") Long cardId,
                         @Param("qty") int qty);

}
