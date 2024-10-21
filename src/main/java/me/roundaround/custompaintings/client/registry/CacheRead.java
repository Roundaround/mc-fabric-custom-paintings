package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.resource.Image;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public record CacheRead(HashMap<Identifier, Image> images, HashMap<Identifier, String> hashes, String combinedHash) {
  public static CacheRead empty() {
    return new CacheRead(new HashMap<>(0), new HashMap<>(0), "");
  }
}
