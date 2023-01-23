package me.roundaround.custompaintings.util;

import java.util.UUID;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;

public record OutdatedPainting(
    UUID paintingUuid,
    PaintingData currentData,
    PaintingData knownData) {
}
