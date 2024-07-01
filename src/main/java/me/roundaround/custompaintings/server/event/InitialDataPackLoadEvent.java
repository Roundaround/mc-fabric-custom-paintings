package me.roundaround.custompaintings.server.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.level.storage.LevelStorage;

import java.util.Arrays;

@FunctionalInterface
public interface InitialDataPackLoadEvent {
  Event<InitialDataPackLoadEvent> EVENT = EventFactory.createArrayBacked(
      InitialDataPackLoadEvent.class, (listeners) -> (session) -> {
        Arrays.stream(listeners).forEach((listener) -> listener.handle(session));
      });

  void handle(LevelStorage.Session session);
}
