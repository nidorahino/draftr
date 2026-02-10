package ygo.draftr.controllers.dto;

public class CubeCardDetailsResponse {

    // cube-specific
    private Long cubeId;
    private Long cardId;
    private int maxQty;
    private boolean banned;

    // card details
    private String name;
    private String cardType;
    private String humanReadableCardType;
    private String frameType;
    private String race;
    private String archetype;
    private String typeline;
    private String attribute;
    private Integer level;
    private Integer atk;
    private Integer def;
    private String imageUrl;
    private String description;

    public Long getCubeId() { return cubeId; }
    public void setCubeId(Long cubeId) { this.cubeId = cubeId; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public int getMaxQty() { return maxQty; }
    public void setMaxQty(int maxQty) { this.maxQty = maxQty; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getHumanReadableCardType() { return humanReadableCardType; }
    public void setHumanReadableCardType(String humanReadableCardType) { this.humanReadableCardType = humanReadableCardType; }

    public String getFrameType() { return frameType; }
    public void setFrameType(String frameType) { this.frameType = frameType; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getArchetype() { return archetype; }
    public void setArchetype(String archetype) { this.archetype = archetype; }

    public String getTypeline() { return typeline; }
    public void setTypeline(String typeline) { this.typeline = typeline; }

    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getAtk() { return atk; }
    public void setAtk(Integer atk) { this.atk = atk; }

    public Integer getDef() { return def; }
    public void setDef(Integer def) { this.def = def; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
