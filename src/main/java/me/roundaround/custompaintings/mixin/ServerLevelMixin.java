package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.world.ServerLevelExtensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements ServerLevelExtensions {
  @Shadow
  public abstract SavedDataStorage getDataStorage();

  @Override
  public ServerPaintingManager custompaintings$getPaintingManager() {
    return this.getDataStorage().computeIfAbsent(ServerPaintingManager.STATE_TYPE);
  }
}
