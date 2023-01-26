package me.roundaround.custompaintings.util;

import java.util.UUID;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.decoration.painting.PaintingEntity;

public record UnknownPainting(
    UUID uuid,
    PaintingEntity paintingRef,
    PaintingData currentData,
    PaintingData suggestedData) {
  public UnknownPainting(UUID uuid, PaintingData currentData, PaintingData suggestedData) {
    this(uuid, null, currentData, suggestedData);
  }

  public UnknownPainting(PaintingEntity paintingRef, PaintingData currentData, PaintingData suggestedData) {
    this(paintingRef.getUuid(), paintingRef, currentData, suggestedData);
  }
}
