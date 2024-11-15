package me.roundaround.custompaintings.resource;

import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.util.CustomId;

import java.util.HashMap;
import java.util.List;

public record MigrationResource(String id, String description, List<List<String>> pairs) {
  public MigrationData toData(String packId) {
    HashMap<CustomId, CustomId> pairs = new HashMap<>();
    this.pairs().forEach((pair) -> pairs.put(CustomId.parse(pair.getFirst()), CustomId.parse(pair.getLast())));
    pairs.entrySet().removeIf((entry) -> entry.getKey() == null || entry.getValue() == null);
    return new MigrationData(new CustomId(packId, this.id()), this.description(), pairs);
  }
}
