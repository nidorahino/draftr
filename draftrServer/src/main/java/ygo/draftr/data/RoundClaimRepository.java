package ygo.draftr.data;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.models.RoundClaim;

import java.util.List;
import java.util.Optional;

public interface RoundClaimRepository extends JpaRepository<RoundClaim, Long> {

    Optional<RoundClaim> findFirstByCubeIdAndAppliedAtIsNullOrderByCreatedAtDesc(Long cubeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rc from RoundClaim rc " +
            "where rc.cubeId = :cubeId and rc.appliedAt is null " +
            "order by rc.createdAt desc")
    List<RoundClaim> findPendingForUpdateList(@Param("cubeId") Long cubeId);
}
