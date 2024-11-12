package me.roundaround.custompaintings.command.argument;

import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public enum PackType {
  ALL("all", CustomPaintingRegistry::getAllPacks),
  ENABLED("enabled", CustomPaintingRegistry::getActivePacks),
  DISABLED("disabled", CustomPaintingRegistry::getInactivePacks);

  private final String id;
  private final Function<ServerPaintingRegistry, Collection<PackData>> getPacks;

  PackType(String id, Function<ServerPaintingRegistry, Collection<PackData>> getPacks) {
    this.id = id;
    this.getPacks = getPacks;
  }

  @Override
  public String toString() {
    return this.id;
  }

  public Collection<PackData> getPacks(ServerPaintingRegistry registry) {
    return this.getPacks.apply(registry);
  }

  public static PackType parse(String id) throws IllegalArgumentException {
    return Arrays.stream(values()).filter((packType) -> packType.id.equals(id)).findFirst().orElseThrow(() -> {
      return new IllegalArgumentException("Unknown PackType " + id);
    });
  }
}
