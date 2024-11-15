package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.resource.Image;

import java.util.HashMap;

public record CacheRead(HashMap<CustomId, Image> images, HashMap<CustomId, String> hashes, String combinedHash) {
  public static CacheRead empty() {
    return new CacheRead(new HashMap<>(0), new HashMap<>(0), "");
  }
}
