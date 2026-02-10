package ygo.draftr.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ygo.draftr.models.CubeEvent;

public interface CubeEventRepository extends JpaRepository<CubeEvent, Long> {
    Page<CubeEvent> findByCubeIdOrderByCreatedAtDesc(Long cubeId, Pageable pageable);
}
