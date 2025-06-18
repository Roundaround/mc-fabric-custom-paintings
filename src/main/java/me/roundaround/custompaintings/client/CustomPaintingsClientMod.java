package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.option.KeyBindings;
import me.roundaround.custompaintings.client.registry.CacheManager;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.client.registry.ItemManager;
import me.roundaround.gradle.api.annotation.Entrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

@Entrypoint(Entrypoint.CLIENT)
public class CustomPaintingsClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientNetworking.registerReceivers();

    KeyBindings.register();

    CacheManager.runBackgroundClean();
    ItemManager.runBackgroundClean();

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      ClientPaintingManager.init();
    });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      ClientPaintingRegistry.getInstance().clear();
      ClientPaintingManager.getInstance().clear();
    });
  }
}
