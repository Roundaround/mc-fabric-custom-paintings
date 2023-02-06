package me.roundaround.custompaintings.util;

import java.util.UUID;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.decoration.painting.PaintingEntity;

public record MismatchedPainting(
    UUID uuid,
    PaintingEntity paintingRef,
    PaintingData currentData,
    PaintingData knownData) {
  public MismatchedPainting(UUID uuid, PaintingData currentData, PaintingData knownData) {
    this(uuid, null, currentData, knownData);
  }

  public MismatchedPainting(PaintingEntity paintingRef, PaintingData currentData, PaintingData knownData) {
    this(paintingRef.getUuid(), paintingRef, currentData, knownData);
  }
}
