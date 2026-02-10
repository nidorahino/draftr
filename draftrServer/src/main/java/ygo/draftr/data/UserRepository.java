package ygo.draftr.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.models.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByResetToken(String resetToken);

    @Query("""
        select u.userId, u.username
        from User u
        where u.userId in :ids
    """)
    List<Object[]> findIdAndUsername(@Param("ids") List<Long> ids);

    default Map<Long, String> findUsernamesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();

        return findIdAndUsername(ids).stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (String) r[1]
                ));
    }
}
