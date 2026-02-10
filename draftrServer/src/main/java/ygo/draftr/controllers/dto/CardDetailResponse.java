package ygo.draftr.controllers.dto;

public class CardDetailResponse {

    private long id;
    private String name;
    private String cardType;
    private String humanReadableCardType;
    private String frameType;
    private String description;
    private String race;
    private String archetype;
    private String typeline;
    private String attribute;
    private Integer level;
    private Integer atk;
    private Integer def;
    private String ygoprodeckUrl;
    private String imageUrl;

    // No-args constructor (needed for Jackson sometimes)
    public CardDetailResponse() {
    }

    // All-args constructor
    public CardDetailResponse(
            long id,
            String name,
            String cardType,
            String humanReadableCardType,
            String frameType,
            String description,
            String race,
            String archetype,
            String typeline,
            String attribute,
            Integer level,
            Integer atk,
            Integer def,
            String ygoprodeckUrl,
            String imageUrl
    ) {
        this.id = id;
        this.name = name;
        this.cardType = cardType;
        this.humanReadableCardType = humanReadableCardType;
        this.frameType = frameType;
        this.description = description;
        this.race = race;
        this.archetype = archetype;
        this.typeline = typeline;
        this.attribute = attribute;
        this.level = level;
        this.atk = atk;
        this.def = def;
        this.ygoprodeckUrl = ygoprodeckUrl;
        this.imageUrl = imageUrl;
    }

    // Getters & Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getHumanReadableCardType() {
        return humanReadableCardType;
    }

    public void setHumanReadableCardType(String humanReadableCardType) {
        this.humanReadableCardType = humanReadableCardType;
    }

    public String getFrameType() {
        return frameType;
    }

    public void setFrameType(String frameType) {
        this.frameType = frameType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getArchetype() {
        return archetype;
    }

    public void setArchetype(String archetype) {
        this.archetype = archetype;
    }

    public String getTypeline() {
        return typeline;
    }

    public void setTypeline(String typeline) {
        this.typeline = typeline;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getAtk() {
        return atk;
    }

    public void setAtk(Integer atk) {
        this.atk = atk;
    }

    public Integer getDef() {
        return def;
    }

    public void setDef(Integer def) {
        this.def = def;
    }

    public String getYgoprodeckUrl() {
        return ygoprodeckUrl;
    }

    public void setYgoprodeckUrl(String ygoprodeckUrl) {
        this.ygoprodeckUrl = ygoprodeckUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
