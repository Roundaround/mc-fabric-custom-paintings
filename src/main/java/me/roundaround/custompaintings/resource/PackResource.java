package me.roundaround.custompaintings.resource;

import me.roundaround.custompaintings.entity.decoration.painting.PackData;

import java.util.List;

public record PackResource(Integer format, String id, String name, String description, String legacyPackId,
                           List<PaintingResource> paintings, List<MigrationResource> migrations) {
  public PackData toData() {
    return new PackData(this.id(), this.name(), this.description(), this.legacyPackId(),
        this.paintings().stream().map((painting) -> painting.toData(this.id())).toList(),
        this.migrations().stream().map((migration) -> migration.toData(this.id())).toList()
    );
  }
}
