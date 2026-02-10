package ygo.draftr.controllers.dto;

import java.util.List;

public class CardFiltersResponse {
    private List<String> archetypes;
    private List<String> races;
    private List<String> attributes;
    private List<String> types;

    public List<String> getArchetypes() { return archetypes; }
    public void setArchetypes(List<String> archetypes) { this.archetypes = archetypes; }

    public List<String> getRaces() { return races; }
    public void setRaces(List<String> races) { this.races = races; }

    public List<String> getAttributes() { return attributes; }
    public void setAttributes(List<String> attributes) { this.attributes = attributes; }

    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
}
