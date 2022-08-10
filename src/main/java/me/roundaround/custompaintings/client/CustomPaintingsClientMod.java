package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.event.MinecraftClientEvents;
import me.roundaround.custompaintings.network.EditPaintingPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;

public class CustomPaintingsClientMod implements ClientModInitializer {
  public static CustomPaintingManager customPaintingManager;

  @Override
  public void onInitializeClient() {
    MinecraftClientEvents.AFTER_INIT.register(() -> {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      customPaintingManager = new CustomPaintingManager(minecraft.getTextureManager());
      ResourceManagerHelper
          .get(ResourceType.CLIENT_RESOURCES)
          .registerReloadListener(customPaintingManager);
      MinecraftClientEvents.ON_CLOSE.register(customPaintingManager::close);
    });

    EditPaintingPacket.registerReceive();
  }
}
