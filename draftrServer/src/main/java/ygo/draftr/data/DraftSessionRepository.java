package ygo.draftr.data;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.models.DraftSession;
import ygo.draftr.models.DraftStatus;

import java.util.List;
import java.util.Optional;

public interface DraftSessionRepository extends JpaRepository<DraftSession, Long> {
    Optional<DraftSession> findByCubeIdAndStatus(Long cubeId, DraftStatus status);
    List<DraftSession> findByCubeIdAndStatusIn(Long cubeId, List<DraftStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select s
    from DraftSession s
    where s.draftSessionId = :id
    """)
    Optional<DraftSession> lockById(@Param("id") Long id);
}