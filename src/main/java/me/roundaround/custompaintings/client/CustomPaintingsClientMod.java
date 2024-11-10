package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.option.KeyBindings;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class CustomPaintingsClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientNetworking.registerReceivers();

    KeyBindings.register();

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      ClientPaintingRegistry.getInstance().clear();
      ClientPaintingManager.getInstance().clear();
    });
  }
}
