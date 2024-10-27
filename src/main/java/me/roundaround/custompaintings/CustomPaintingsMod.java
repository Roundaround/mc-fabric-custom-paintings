package me.roundaround.custompaintings;

import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.registry.VanillaPaintingRegistry;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  private static UUID serverId = null;

  @Override
  public void onInitialize() {
    CustomPaintingsConfig.getInstance().init();
    CustomPaintingsPerWorldConfig.getInstance().init();

    Networking.registerS2CPayloads();
    Networking.registerC2SPayloads();

    ServerNetworking.registerReceivers();

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      CustomPaintingsCommand.register(dispatcher);
    });

    ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
      serverId = null;
    });

    ServerWorldEvents.LOAD.register((server, world) -> {
      server.getRegistryManager().getWrapperOrThrow(Registries.PAINTING_VARIANT.getKey());
      VanillaPaintingRegistry.init();
      ServerPaintingRegistry.init(server);
      ServerPaintingManager.init(world);
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayerEntity player = handler.getPlayer();
      ServerPaintingRegistry.getInstance().sendSummaryToPlayer(player);
      ServerPaintingManager.getInstance(player.getServerWorld()).syncAllDataForPlayer(player);
    });

    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
      ServerPaintingManager.getInstance(destination).syncAllDataForPlayer(player);
    });

    UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return ActionResult.PASS;
      }

      if (player.isSpectator() || !player.isSneaking()) {
        return ActionResult.PASS;
      }

      painting.setCustomNameVisible(!painting.isCustomNameVisible());
      return ActionResult.SUCCESS_NO_ITEM_USED;
    });
  }

  public static UUID getOrGenerateServerId() {
    if (serverId == null) {
      serverId = UUID.randomUUID();
    }
    return serverId;
  }
}
