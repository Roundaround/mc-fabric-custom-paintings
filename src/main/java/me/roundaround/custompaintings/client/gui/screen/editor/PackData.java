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
    HashedImage icon,
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
        new HashedImage(icon),
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

  public static class HashedImage {
    public final Image image;
    public final String hash;

    public HashedImage(Image image) {
      this.image = image;
      this.hash = image == null ? "" : image.getHash();
    }

    public boolean equals(HashedImage other) {
      if (!this.hash.equals(other.hash)) {
        return false;
      }

      if (this.image == null && other.image == null) {
        return true;
      }

      if (this.image == null || other.image == null) {
        return false;
      }

      return this.image.width() == other.image.width()
          && this.image.height() == other.image.height();
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      return other instanceof HashedImage && this.equals((HashedImage) other);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.image, this.hash);
    }
  }

  public record Painting(
      String id,
      String name,
      String artist,
      int blockWidth,
      int blockHeight,
      HashedImage image) {
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
