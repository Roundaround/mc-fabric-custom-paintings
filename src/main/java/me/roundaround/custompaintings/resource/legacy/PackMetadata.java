package me.roundaround.custompaintings.resource.legacy;

import me.roundaround.custompaintings.resource.Image;

import java.util.UUID;

public record PackMetadata(UUID uuid, String packFileUid, LegacyPackResource pack, Image icon) {
  public PackMetadata(String packFileUid, LegacyPackResource pack, Image icon) {
    this(UUID.randomUUID(), packFileUid, pack, icon);
  }
}
