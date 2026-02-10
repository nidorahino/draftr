package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import ygo.draftr.models.DraftPoolCard;

public interface DraftPoolCardRepository extends JpaRepository<DraftPoolCard, Long> {
    boolean existsByDraftSessionId(Long draftSessionId);
}