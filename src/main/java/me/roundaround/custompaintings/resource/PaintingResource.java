package me.roundaround.custompaintings.resource;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.InvalidIdException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PaintingResource(String id, String name, String artist, Integer height, Integer width) {
  public PaintingData toData(String packId) {
    return new PaintingData(
        new CustomId(packId, this.id()),
        this.width(),
        this.height(),
        nullToEmpty(this.name()),
        nullToEmpty(this.artist())
    );
  }

  public void validateId(int index) throws InvalidIdException {
    CustomId.validatePart(this.id, String.format("painting at index %s", index));
  }

  private static @NotNull String nullToEmpty(@Nullable String input) {
    return input == null ? "" : input;
  }
}
