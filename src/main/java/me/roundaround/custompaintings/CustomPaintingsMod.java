package me.roundaround.custompaintings;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.registry.VanillaPaintingRegistry;
import me.roundaround.custompaintings.resource.PaintingPackLoader;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.resource.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  public static final TrackedDataHandler<PaintingData> CUSTOM_PAINTING_DATA_HANDLER =
      (TrackedDataHandler.ImmutableHandler<PaintingData>) () -> PaintingData.PACKET_CODEC;

  @Override
  public void onInitialize() {
    Networking.registerS2CPayloads();
    Networking.registerC2SPayloads();

    TrackedDataHandlerRegistry.register(CUSTOM_PAINTING_DATA_HANDLER);
    ServerNetworking.registerReceivers();

    ServerLifecycleEvents.SERVER_STARTED.register(((server) -> {
      VanillaPaintingRegistry.init();
      ServerPaintingRegistry.init(server);
    }));

    ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> {
      ServerPaintingRegistry.getInstance().sendSummaryToPlayer(handler.getPlayer());
    }));

    ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }
      ServerPaintingManager manager = ServerPaintingManager.getInstance(world);
      manager.registerPainting(painting);
    });

    ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new PaintingPackLoader());
  }
}
