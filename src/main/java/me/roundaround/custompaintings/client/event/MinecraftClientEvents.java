package me.roundaround.custompaintings.client.event;

import java.util.Arrays;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@Environment(value = EnvType.CLIENT)
public interface MinecraftClientEvents {
  Event<MinecraftClientEvents> AFTER_INIT = EventFactory.createArrayBacked(MinecraftClientEvents.class,
      (listeners) -> () -> Arrays.stream(listeners).forEach(MinecraftClientEvents::interact));
  Event<MinecraftClientEvents> ON_CLOSE = EventFactory.createArrayBacked(MinecraftClientEvents.class,
      (listeners) -> () -> Arrays.stream(listeners).forEach(MinecraftClientEvents::interact));

  void interact();
}
