package me.roundaround.custompaintings.resource.legacy;

import me.roundaround.custompaintings.resource.Image;

import java.util.UUID;

public record PackMetadata(UUID uuid, LegacyPackResource pack, Image icon) {
  public PackMetadata(LegacyPackResource pack, Image icon) {
    this(UUID.randomUUID(), pack, icon);
  }
}
