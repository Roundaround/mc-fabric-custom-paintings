package me.roundaround.custompaintings.resource.file;

import java.util.HashMap;
import java.util.List;

import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.InvalidIdException;

public record Migration(String id, String description, List<List<String>> pairs) {
  public MigrationData toData(String packId) {
    HashMap<CustomId, CustomId> pairs = new HashMap<>();
    this.pairs.forEach((pair) -> pairs.put(CustomId.parse(pair.getFirst()), CustomId.parse(pair.getLast())));
    pairs.entrySet().removeIf((entry) -> entry.getKey() == null || entry.getValue() == null);
    return new MigrationData(new CustomId(packId, this.id), this.description, pairs);
  }

  public void validateIds(int index) throws InvalidIdException {
    CustomId.validatePart(this.id, this.migrationErrorStr(index));

    int i = 0;
    for (List<String> pair : this.pairs) {
      CustomId.validate(pair.getFirst(), this.pairErrorStr("from", index, i));
      CustomId.validate(pair.getLast(), this.pairErrorStr("to", index, i));
      i++;
    }
  }

  private String migrationErrorStr(int index) {
    return String.format("migration at index %s", index);
  }

  private String pairErrorStr(String which, int migrationIndex, int pairIndex) {
    return String.format(
        "'%s' painting in pair at index %s in %s", which, pairIndex, this.migrationErrorStr(migrationIndex));
  }
}
