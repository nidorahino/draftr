package ygo.draftr.controllers.dto;

public class CreateDraftRequest {
    private int draftSize; // per player
    private int packSize;

    public int getDraftSize() { return draftSize; }
    public void setDraftSize(int draftSize) { this.draftSize = draftSize; }

    public int getPackSize() { return packSize; }
    public void setPackSize(int packSize) { this.packSize = packSize; }
}