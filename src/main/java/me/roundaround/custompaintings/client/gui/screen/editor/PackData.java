package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.List;
import java.util.UUID;

import me.roundaround.custompaintings.resource.file.Image;

public record PackData(
    UUID uuid,
    String id,
    String name,
    String description,
    String iconHash,
    Image icon,
    List<Painting> paintings) {
  public PackData() {
    this(UUID.randomUUID(), "", "", "", "", null, List.of());
  }

  public PackData(PackData other) {
    this(other.uuid, other.id, other.name, other.description, other.iconHash, other.icon, other.paintings);
  }

  public PackData(String id, String name, String description, String iconHash, Image icon, List<Painting> paintings) {
    this(UUID.randomUUID(), id, name, description, iconHash, icon, paintings);
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public String getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public String getIconHash() {
    return this.iconHash;
  }

  public Image getIcon() {
    return this.icon;
  }

  public List<Painting> getPaintings() {
    return this.paintings;
  }

  public boolean equals(PackData other) {
    return this.id.equals(other.id)
        && this.name.equals(other.name)
        && this.description.equals(other.description)
        && this.iconHash.equals(other.iconHash)
        && imagesEqual(this.icon, other.icon)
        && this.paintings.equals(other.paintings);
  }

  public static boolean imagesEqual(Image a, Image b) {
    if (a == null && b == null) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    return a.width() == b.width()
        && a.height() == b.height();
  }

  public record Painting(String id, String name, String artist, int blockWidth, int blockHeight, String hash,
      Image image) {
    public Painting() {
      this("", "", "", 16, 16, "", null);
    }

    public boolean equals(Painting other) {
      return this.id.equals(other.id)
          && this.name.equals(other.name)
          && this.artist.equals(other.artist)
          && this.blockWidth == other.blockWidth
          && this.blockHeight == other.blockHeight
          && this.hash.equals(other.hash)
          && imagesEqual(this.image, other.image);
    }
  }
}
