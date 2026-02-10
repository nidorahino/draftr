package ygo.draftr.models;

import jakarta.persistence.*;

@Entity
@Table(name = "card")
public class Card {

    // YGOPRODeck ID (not generated)
    @Id
    @Column(name = "card_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "card_type", nullable = false, length = 50)
    private String cardType;

    @Column(name = "human_readable_card_type", length = 80)
    private String humanReadableCardType;

    @Column(name = "frame_type", length = 30)
    private String frameType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 50)
    private String race;

    @Column(length = 80)
    private String archetype;

    @Column(length = 120)
    private String typeline;

    @Column(length = 20)
    private String attribute;

    private Integer level;
    private Integer atk;
    private Integer def;

    @Column(name = "ygoprodeck_url", columnDefinition = "text")
    private String ygoprodeckUrl;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    /* Getters / Setters */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
