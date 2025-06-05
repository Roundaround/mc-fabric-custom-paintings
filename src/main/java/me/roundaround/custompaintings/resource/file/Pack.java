package me.roundaround.custompaintings.resource.file;

import java.util.List;
import java.util.Optional;

import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.InvalidIdException;

public record Pack(Integer format,
    String id,
    String name,
    String description,
    String sourceLegacyPack,
    List<Painting> paintings,
    List<Migration> migrations) {
  public Pack(String id, String name, String description, List<Painting> paintings, List<Migration> migrations) {
    this(-1, id, name, description, null, paintings, migrations);
  }

  public PackData toData(FileUid fileUid, boolean disabled) {
    return new PackData(
        fileUid.stringValue(),
        disabled,
        fileUid.fileSize(),
        this.id,
        this.name,
        Optional.ofNullable(this.description),
        Optional.ofNullable(this.sourceLegacyPack),
        this.paintings.stream().map((painting) -> painting.toData(this.id)).toList(),
        this.migrations.stream().map((migration) -> migration.toData(this.id)).toList());
  }

  public void validateIds() throws InvalidIdException {
    CustomId.validatePart(this.id, "pack");

    int i = 0;
    for (Painting painting : this.paintings) {
      painting.validateId(i);
      i++;
    }

    i = 0;
    for (Migration migration : this.migrations) {
      migration.validateIds(i);
      i++;
    }
  }
}
