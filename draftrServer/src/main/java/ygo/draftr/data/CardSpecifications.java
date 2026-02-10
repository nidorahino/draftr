package ygo.draftr.data;

import org.springframework.data.jpa.domain.Specification;
import ygo.draftr.models.Card;

public class CardSpecifications {

    public static Specification<Card> queryLike(String query) {
        if (query == null || query.isBlank()) return null;

        String q = "%" + query.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), q),
                cb.like(cb.lower(root.get("description")), q),
                cb.like(cb.lower(root.get("archetype")), q)
        );
    }

    public static Specification<Card> cardTypeEquals(String cardType) {
        if (cardType == null || cardType.isBlank()) return null;
        return (root, cq, cb) -> cb.equal(root.get("cardType"), cardType);
    }

    public static Specification<Card> archetypeEquals(String archetype) {
        if (archetype == null || archetype.isBlank()) return null;
        return (root, cq, cb) -> cb.equal(root.get("archetype"), archetype);
    }

    public static Specification<Card> raceEquals(String race) {
        if (race == null || race.isBlank()) return null;
        return (root, cq, cb) -> cb.equal(root.get("race"), race);
    }

    public static Specification<Card> attributeEquals(String attribute) {
        if (attribute == null || attribute.isBlank()) return null;
        return (root, cq, cb) -> cb.equal(root.get("attribute"), attribute);
    }
}
