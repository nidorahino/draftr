package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import ygo.draftr.models.CubeMember;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CubeMemberRepository extends JpaRepository<CubeMember, Long> {
    boolean existsByCubeIdAndUserId(Long cubeId, Long userId);

    long countByCubeId(Long cubeId);

    List<CubeMember> findByCubeId(Long cubeId);

    List<CubeMember> findByUserId(Long userId);

    Optional<CubeMember> findByCubeIdAndUserId(Long cubeId, Long userId);

    void deleteByCubeIdAndUserId(Long cubeId, Long userId);

    boolean existsByCubeIdAndUserIdAndRoleIn(Long cubeId, Long userId, Collection<String> roles);
}
