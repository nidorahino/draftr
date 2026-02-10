package ygo.draftr.controllers.dto;

import java.util.List;

public class MyPackResponse {
    private Long draftPackId;
    private int roundNo;
    private String direction;
    private List<MyPackCard> cards;
    private boolean waiting;
    private int pickNo;

    public Long getDraftPackId() { return draftPackId; }
    public void setDraftPackId(Long draftPackId) { this.draftPackId = draftPackId; }

    public int getRoundNo() { return roundNo; }
    public void setRoundNo(int roundNo) { this.roundNo = roundNo; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public List<MyPackCard> getCards() { return cards; }
    public void setCards(List<MyPackCard> cards) { this.cards = cards; }

    public boolean isWaiting() { return waiting; }
    public void setWaiting(boolean waiting) { this.waiting = waiting; }

    public int getPickNo() { return pickNo; }
    public void setPickNo(int pickNo) { this.pickNo = pickNo; }

    public static class MyPackCard {
        private Long draftPackCardId;
        private Long cardId;
        private int slotNo;

        private String name;
        private String imageUrl;
        private String description;
        private String humanReadableCardType;

        private Integer atk;
        private Integer def;
        private Integer level;

        public Long getDraftPackCardId() { return draftPackCardId; }
        public void setDraftPackCardId(Long draftPackCardId) { this.draftPackCardId = draftPackCardId; }

        public Long getCardId() { return cardId; }
        public void setCardId(Long cardId) { this.cardId = cardId; }

        public int getSlotNo() { return slotNo; }
        public void setSlotNo(int slotNo) { this.slotNo = slotNo; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getHumanReadableCardType() { return humanReadableCardType; }
        public void setHumanReadableCardType(String humanReadableCardType) { this.humanReadableCardType = humanReadableCardType; }

        public Integer getAtk() { return atk; }
        public void setAtk(Integer atk) { this.atk = atk; }

        public Integer getDef() { return def; }
        public void setDef(Integer def) { this.def = def; }

        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
    }
}
