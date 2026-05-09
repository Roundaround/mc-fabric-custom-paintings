package me.roundaround.custompaintings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.roundaround.custompaintings.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.resource.PackResource;
import me.roundaround.custompaintings.resource.file.json.CustomPaintingsJson;
import me.roundaround.custompaintings.resource.file.json.LegacyCustomPaintingsJson;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.gradle.api.annotation.Entrypoint;
import me.roundaround.roundalib.event.ResourceManagerEvents;
import me.roundaround.roundalib.observable.Observer;
import me.roundaround.roundalib.observable.Subscription;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.Painting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Entrypoint(Entrypoint.MAIN)
public final class CustomPaintingsMod implements ModInitializer {
  public static final Logger LOGGER = LogManager.getLogger(Constants.MOD_ID);
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
      .registerTypeAdapter(CustomPaintingsJson.class, new CustomPaintingsJson.TypeAdapter())
      .registerTypeAdapter(LegacyCustomPaintingsJson.class, new LegacyCustomPaintingsJson.TypeAdapter())
      .registerTypeAdapter(PackResource.class, new PackResource.TypeAdapter())
      .create();
  public static final String EMPTY_HASH = "$$";

  private static Subscription stonecutterSub = null;
  private static Subscription vanillaStonecutterSub = null;

  public static CompletableFuture<Void> reloadDataPacks(MinecraftServer server) {
    if (server == null) {
      return CompletableFuture.completedFuture(null);
    }
    List<String> enabledPacks = server.getPackRepository().getSelectedPacks().stream().map(Pack::getId).toList();
    return server.reloadResources(enabledPacks).exceptionally((throwable) -> {
      LOGGER.warn("Failed to reload data packs", throwable);
      return null;
    });
  }

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

    ServerLevelEvents.LOAD.register((server, world) -> {
      server.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT);
      ServerPaintingRegistry.init(server);
      world.custompaintings$getPaintingManager();
    });

    ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
      if (!(entity instanceof Painting painting)) {
        return;
      }
      world.custompaintings$getPaintingManager().onEntityLoad(painting);
    });

    ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
      if (!(entity instanceof Painting painting)) {
        return;
      }
      world.custompaintings$getPaintingManager().onEntityUnload(painting);
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayer player = handler.getPlayer();
      ServerPaintingRegistry.getInstance().sendSummaryToPlayer(player);
      player.level().custompaintings$getPaintingManager().syncAllDataForPlayer(player);
    });

    ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
      destination.custompaintings$getPaintingManager().syncAllDataForPlayer(player);
    });

    UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
      if (!(entity instanceof Painting painting)) {
        return InteractionResult.PASS;
      }

      if (player.isSpectator() || !player.isShiftKeyDown()) {
        return InteractionResult.PASS;
      }

      painting.setCustomNameVisible(!painting.isCustomNameVisible());
      return InteractionResult.SUCCESS;
    });

    ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
      if (stonecutterSub != null) {
        stonecutterSub.close();
      }
      if (vanillaStonecutterSub != null) {
        vanillaStonecutterSub.close();
      }
      Observer.P0 onChange = () -> reloadDataPacks(server);
      stonecutterSub = CustomPaintingsPerWorldConfig.getInstance().pickPaintingWithStoneCutter.savedValue.cold()
          .subscribe(onChange);
      vanillaStonecutterSub =
          CustomPaintingsPerWorldConfig.getInstance().pickVanillaPaintingWithStoneCutter.savedValue.cold()
          .subscribe(onChange);
    });

    ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
      if (stonecutterSub != null) {
        stonecutterSub.close();
        stonecutterSub = null;
      }
      if (vanillaStonecutterSub != null) {
        vanillaStonecutterSub.close();
        vanillaStonecutterSub = null;
      }
    });
  }
}
