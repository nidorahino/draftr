package ygo.draftr.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ygo.draftr.models.Card;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Page<Card> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
        select distinct c.archetype
        from Card c
        where c.archetype is not null and c.archetype <> ''
        order by c.archetype asc
    """)
    List<String> findDistinctArchetypes();

    @Query("""
        select distinct c.race
        from Card c
        where c.race is not null and c.race <> ''
        order by c.race asc
    """)
    List<String> findDistinctRaces();

    @Query("""
        select distinct c.attribute
        from Card c
        where c.attribute is not null and c.attribute <> ''
        order by c.attribute asc
    """)
    List<String> findDistinctAttributes();

    @Query("""
        select distinct c.cardType
        from Card c
        where c.cardType is not null and c.cardType <> ''
        order by c.cardType asc
    """)
    List<String> findDistinctCardTypes();

    @Query("""
    select c
    from Card c
    where c.id in (
        select cc.cardId
        from CubeCard cc
        where cc.cubeId = :cubeId
    )
    and lower(c.name) like lower(concat('%', :name, '%'))
    """)
    Page<Card> searchInCubePool(@Param("cubeId") Long cubeId,
                                @Param("name") String name,
                                Pageable pageable);

    interface CardDetails {
        Long getId();
        String getName();
        String getImageUrl();
        String getDescription();
        String getHumanReadableCardType();
        Integer getAtk();
        Integer getDef();
        Integer getLevel();
    }

    @Query("""
        select c.id as id,
               c.name as name,
               c.imageUrl as imageUrl,
               c.description as description,
               c.humanReadableCardType as humanReadableCardType,
               c.atk as atk,
               c.def as def,
               c.level as level
        from Card c
        where c.id in :ids
    """)
    List<CardDetails> findDetailsByIds(@Param("ids") List<Long> ids);

}
