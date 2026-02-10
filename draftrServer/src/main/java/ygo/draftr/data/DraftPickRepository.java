package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import ygo.draftr.models.DraftPick;

public interface DraftPickRepository extends JpaRepository<DraftPick, Long> {
}