package ygo.draftr.importer.dto;

import java.util.List;

public class CardInfoResponse {

    private List<CardDto> data;

    public CardInfoResponse() {
    }

    public List<CardDto> getData() {
        return data;
    }

    public void setData(List<CardDto> data) {
        this.data = data;
    }
}