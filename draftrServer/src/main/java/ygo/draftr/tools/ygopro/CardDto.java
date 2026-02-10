package ygo.draftr.tools.ygopro;

import java.util.List;

public record CardDto(
        long id,
        List<CardImageDto> card_images
) {}
