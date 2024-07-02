package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.resource.PaintingImageCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class CustomPaintingsClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientNetworking.registerS2CHandlers();

    ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new PaintingImageCache());
  }
}
