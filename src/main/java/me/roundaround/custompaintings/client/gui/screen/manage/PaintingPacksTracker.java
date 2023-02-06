package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.CustomPaintingManager.Pack;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.Migration;
import net.minecraft.util.Identifier;

public interface PaintingPacksTracker {
  public default void onKnownPaintingsChanged(HashMap<Identifier, PaintingData> knownPaintings) {
  }

  public default void onMigrationsChanged(HashMap<String, MigrationGroup> migrations) {
  }

  public default void onResourcesReloaded() {
    onKnownPaintingsChanged(getKnownPaintings());
    onMigrationsChanged(getMigrations());
  }

  public default HashMap<Identifier, PaintingData> getKnownPaintings() {
    return CustomPaintingsClientMod.customPaintingManager.getEntries().stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity(), (a, b) -> a, HashMap::new));
  }

  public default HashMap<String, MigrationGroup> getMigrations() {
    return CustomPaintingsClientMod.customPaintingManager.getMigrations().stream()
        .collect(Collectors.groupingBy(Migration::packId, HashMap::new, Collectors.collectingAndThen(
            Collectors.toCollection(ArrayList::new), list -> {
              Pack pack = CustomPaintingsClientMod.customPaintingManager.getPack(list.get(0).packId()).get();
              return new MigrationGroup(pack.id(), pack.name(), list);
            })));
  }

  public record MigrationGroup(String packId, String packName, ArrayList<Migration> migrations) {
  }
}
