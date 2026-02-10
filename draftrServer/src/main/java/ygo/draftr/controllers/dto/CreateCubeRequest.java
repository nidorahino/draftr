package ygo.draftr.controllers.dto;

public class CreateCubeRequest {
    private String name;
    private Integer maxPlayers;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
}
