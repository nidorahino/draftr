package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import ygo.draftr.models.Cube;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CubeRepository extends JpaRepository<Cube, Long> {
    List<Cube> findAllByCubeIdInAndArchivedFalse(Collection<Long> cubeIds);

    Optional<Cube> findByCubeIdAndArchivedFalse(Long cubeId);
}
