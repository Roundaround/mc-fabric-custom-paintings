package me.roundaround.custompaintings;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.custompaintings.registry.VanillaPaintingRegistry;
import me.roundaround.custompaintings.resource.PaintingPackLoader;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.server.command.sub.MoveSub.MoveDirectionArgumentType;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  public static final TrackedDataHandler<PaintingData> CUSTOM_PAINTING_DATA_HANDLER =
      (TrackedDataHandler.ImmutableHandler<PaintingData>) () -> PaintingData.PACKET_CODEC;

  public static HashMap<UUID, HashSet<PaintingData>> knownPaintings = new HashMap<>();

  private static HashMap<MinecraftServer, PaintingPackLoader> paintingPackLoaders = new HashMap<>(1);

  @Override
  public void onInitialize() {
    Networking.registerS2CPayloads();
    Networking.registerC2SPayloads();

    TrackedDataHandlerRegistry.register(CUSTOM_PAINTING_DATA_HANDLER);
    ServerNetworking.registerReceivers();

    ArgumentTypeRegistry.registerArgumentType(new Identifier(CustomPaintingsMod.MOD_ID, "move_direction"),
        MoveDirectionArgumentType.class, ConstantArgumentSerializer.of(MoveDirectionArgumentType::direction)
    );

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      CustomPaintingsCommand.register(dispatcher);
    });

    ServerLifecycleEvents.SERVER_STARTED.register(((server) -> {
      VanillaPaintingRegistry.init();
    }));

    ServerLifecycleEvents.SERVER_STOPPED.register(((server) -> {
      ServerPaintingRegistry.close();
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
