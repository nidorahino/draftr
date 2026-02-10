package ygo.draftr.controllers.dto;

public class CardSearchResult {

    private Long id;
    private String name;
    private String cardType;
    private String humanReadableCardType;
    private String archetype;
    private String imageUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getHumanReadableCardType() { return humanReadableCardType; }
    public void setHumanReadableCardType(String humanReadableCardType) { this.humanReadableCardType = humanReadableCardType; }

    public String getArchetype() { return archetype; }
    public void setArchetype(String archetype) { this.archetype = archetype; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
