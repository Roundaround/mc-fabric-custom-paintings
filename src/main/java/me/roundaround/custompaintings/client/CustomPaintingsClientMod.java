package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.resource.legacy.LegacyPaintingPackChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class CustomPaintingsClientMod implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    ClientNetworking.registerReceivers();

    ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
      ClientPaintingRegistry.getInstance().close();
      ClientPaintingManager.getInstance().close();
    }));

    ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new LegacyPaintingPackChecker());
  }
}
