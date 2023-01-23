package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.util.Identifier;

public interface KnownPaintingsTracker {
  public void onKnownPaintingsChanged(HashMap<Identifier, PaintingData> knownPaintings);

  public default void onResourcesReloaded() {
    onKnownPaintingsChanged(getKnownPaintings());
  }

  public default HashMap<Identifier, PaintingData> getKnownPaintings() {
    return CustomPaintingsClientMod.customPaintingManager.getEntries().stream()
        .collect(Collectors.toMap(PaintingData::id, Function.identity(), (a, b) -> a, HashMap::new));
  }
}
