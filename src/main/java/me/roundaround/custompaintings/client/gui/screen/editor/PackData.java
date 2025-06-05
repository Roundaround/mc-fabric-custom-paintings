package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.UUID;

public record PackData(UUID uuid, String id, String name, String description) {
  public PackData() {
    this(UUID.randomUUID(), "", "", "");
  }

  public PackData(PackData other) {
    this(other.uuid, other.id, other.name, other.description);
  }

  public PackData(String id, String name, String description) {
    this(UUID.randomUUID(), id, name, description);
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

  public boolean equals(PackData other) {
    return this.id.equals(other.id)
        && this.name.equals(other.name)
        && this.description.equals(other.description);
  }
}
