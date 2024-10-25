package me.roundaround.custompaintings.resource;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;

import java.util.List;

public record PackResource(Integer format, String id, String name, String description, String legacyPackId,
                           List<PaintingResource> paintings) {
  public PaintingPack toData() {
    return new PaintingPack(this.id(), this.name(), this.description(),
        this.paintings().stream().map((painting) -> painting.toData(this.id())).toList()
    );
  }
}
