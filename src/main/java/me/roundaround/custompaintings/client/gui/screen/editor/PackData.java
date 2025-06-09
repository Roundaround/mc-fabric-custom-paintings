package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.roundaround.custompaintings.resource.file.Image;

public record PackData(
    UUID uuid,
    String id,
    String name,
    String description,
    Image icon,
    @NotNull List<Painting> paintings) {
  public PackData() {
    this(UUID.randomUUID(), "", "", "", null, List.of());
  }

  public PackData(PackData other) {
    this(other.uuid, other.id, other.name, other.description, other.icon, other.paintings);
  }

  public PackData(
      String id,
      String name,
      String description,
      Image icon,
      @Nullable List<Painting> paintings) {
    this(
        UUID.randomUUID(),
        id,
        name,
        description,
        icon,
        paintings == null ? List.of() : paintings);
  }

  public boolean equals(PackData other) {
    return Objects.equals(this.id, other.id)
        && Objects.equals(this.name, other.name)
        && Objects.equals(this.description, other.description)
        && Objects.equals(this.icon, other.icon)
        && this.paintings.equals(other.paintings);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof PackData && this.equals((PackData) other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.name, this.description, this.icon, this.paintings);
  }

  public record Painting(
      String id,
      String name,
      String artist,
      int blockWidth,
      int blockHeight,
      Image image) {
    public Painting() {
      this("", "", "", 16, 16, null);
    }

    public boolean equals(Painting other) {
      return Objects.equals(this.id, other.id)
          && Objects.equals(this.name, other.name)
          && Objects.equals(this.artist, other.artist)
          && this.blockWidth == other.blockWidth
          && this.blockHeight == other.blockHeight
          && Objects.equals(this.image, other.image);
    }

    public Painting withImage(Image image) {
      return new Painting(
          this.id,
          this.name,
          this.artist,
          this.blockWidth,
          this.blockHeight,
          image);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      return other instanceof Painting && this.equals((Painting) other);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.id, this.name, this.artist, this.blockWidth, this.blockHeight, this.image);
    }
  }
}
