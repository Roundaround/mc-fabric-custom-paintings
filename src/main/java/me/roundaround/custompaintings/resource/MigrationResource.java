package me.roundaround.custompaintings.resource;

import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;

public record MigrationResource(String id, String description, List<List<String>> pairs) {
  public MigrationData toData(String packId) {
    HashMap<Identifier, Identifier> pairs = new HashMap<>();
    this.pairs()
        .forEach((pair) -> pairs.put(Identifier.tryParse(pair.getFirst()), Identifier.tryParse(pair.getLast())));
    pairs.entrySet().removeIf((entry) -> entry.getKey() == null || entry.getValue() == null);
    return new MigrationData(new Identifier(packId, this.id()), this.description(), pairs);
  }
}
