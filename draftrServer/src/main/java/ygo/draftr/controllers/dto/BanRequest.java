package ygo.draftr.controllers.dto;

public class BanRequest {
    private boolean banned;
    private String reason;

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
