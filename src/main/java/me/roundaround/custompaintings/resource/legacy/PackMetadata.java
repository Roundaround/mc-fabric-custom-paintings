package me.roundaround.custompaintings.resource.legacy;

import me.roundaround.custompaintings.resource.Image;

import java.util.UUID;

public record PackMetadata(UUID uuid, LegacyPackId id, LegacyPackResource pack, Image icon) {
  public PackMetadata(LegacyPackId id, LegacyPackResource pack, Image icon) {
    this(UUID.randomUUID(), id, pack, icon);
  }
}
