package me.roundaround.custompaintings.server;

import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class CustomPaintingsServerMod implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    ServerTickEvents.START_SERVER_TICK.register((server) -> {
      ImagePacketQueue.getInstance().tick();
    });
  }
}
