package me.roundaround.custompaintings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.resource.PackResource;
import me.roundaround.custompaintings.resource.legacy.CustomPaintingsJson;
import me.roundaround.custompaintings.roundalib.event.ResourceManagerEvents;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.gradle.api.annotation.Entrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entrypoint(Entrypoint.MAIN)
public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
      .registerTypeAdapter(CustomPaintingsJson.class, new CustomPaintingsJson.TypeAdapter())
      .registerTypeAdapter(PackResource.class, new PackResource.TypeAdapter())
      .create();
  public static final String MSG_CMD_IGNORE = CustomPaintingsMod.MOD_ID + ":" + "ignore";
  public static final String MSG_CMD_OPEN_CONVERT_SCREEN = CustomPaintingsMod.MOD_ID + ":" + "openConvertScreen";

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

    ResourceManagerEvents.CREATING.register(ServerInfo::init);

    ServerWorldEvents.LOAD.register((server, world) -> {
      server.getRegistryManager().getOrThrow(RegistryKeys.PAINTING_VARIANT);
      ServerPaintingRegistry.init(server);
      world.custompaintings$getPaintingManager();
    });

    ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }
      world.custompaintings$getPaintingManager().onEntityLoad(painting);
    });

    ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }
      world.custompaintings$getPaintingManager().onEntityUnload(painting);
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayerEntity player = handler.getPlayer();
      ServerPaintingRegistry.getInstance().sendSummaryToPlayer(player);
      player.getWorld().custompaintings$getPaintingManager().syncAllDataForPlayer(player);
    });

    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
      destination.custompaintings$getPaintingManager().syncAllDataForPlayer(player);
    });

    UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
      if (!(entity instanceof PaintingEntity painting)) {
        return ActionResult.PASS;
      }

      if (player.isSpectator() || !player.isSneaking()) {
        return ActionResult.PASS;
      }

      painting.setCustomNameVisible(!painting.isCustomNameVisible());
      return ActionResult.SUCCESS;
    });
  }
}
