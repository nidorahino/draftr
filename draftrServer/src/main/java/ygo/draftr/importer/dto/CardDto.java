package ygo.draftr.importer.dto;

import java.util.List;

public record CardDto(
        long id,
        String name,
        String type,
        String humanReadableCardType,
        String frameType,
        String desc,
        String race,
        String archetype,
        List<String> typeline,
        String attribute,
        Integer level,
        Integer atk,
        Integer def,
        String ygoprodeck_url
) {}
