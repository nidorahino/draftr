package ygo.draftr.controllers.dto;

public class UpdateWinsRequest {

    private int delta; // +1 or -1

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }
}