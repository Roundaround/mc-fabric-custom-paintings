package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.world.ServerWorldExtensions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements ServerWorldExtensions {
  @Shadow
  public abstract PersistentStateManager getPersistentStateManager();

  @Override
  public ServerPaintingManager custompaintings$getPaintingManager() {
    return this.getPersistentStateManager().getOrCreate(ServerPaintingManager.STATE_TYPE);
  }
}
