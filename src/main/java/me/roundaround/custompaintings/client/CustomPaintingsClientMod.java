package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class CustomPaintingsClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientNetworking.registerReceivers();

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      ClientPaintingRegistry.getInstance().close();
      ClientPaintingManager.getInstance().close();
    });
  }
}
