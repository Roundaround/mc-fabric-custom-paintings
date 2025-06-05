package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.util.CustomId;

import java.util.HashMap;
import java.util.Map;

public record CacheRead(Map<CustomId, Image> images, Map<CustomId, String> hashes, String combinedHash) {
  public static CacheRead empty() {
    return new CacheRead(new HashMap<>(0), new HashMap<>(0), "");
  }
}
