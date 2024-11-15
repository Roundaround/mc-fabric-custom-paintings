package me.roundaround.custompaintings.client.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.ClientPaintingManager;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.custompaintings.client.gui.screen.MigrationsScreen;
import me.roundaround.custompaintings.client.gui.screen.edit.PackSelectScreen;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.StringUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public final class ClientNetworking {
  private ClientNetworking() {
  }

  public static void sendHashesPacket(Map<CustomId, String> hashes) {
    ClientPlayNetworking.send(new Networking.HashesC2S(hashes));
  }

  public static void sendReloadPacket() {
    sendReloadPacket(List.of(), List.of());
  }

  public static void sendReloadPacket(List<String> toActivate, List<String> toDeactivate) {
    ClientPlayNetworking.send(new Networking.ReloadC2S(toActivate, toDeactivate));
  }

  public static void sendSetPaintingPacket(int paintingId, CustomId dataId) {
    ClientPlayNetworking.send(new Networking.SetPaintingC2S(paintingId, dataId));
  }

  public static void sendRunMigrationPacket(CustomId id) {
    ClientPlayNetworking.send(new Networking.RunMigrationC2S(id));
  }

  public static void registerReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(Networking.SummaryS2C.ID, ClientNetworking::handleSummary);
    ClientPlayNetworking.registerGlobalReceiver(
        Networking.DownloadSummaryS2C.ID, ClientNetworking::handleDownloadSummary);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageS2C.ID, ClientNetworking::handleImage);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageHeaderS2C.ID, ClientNetworking::handleImageHeader);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageChunkS2C.ID, ClientNetworking::handleImageChunk);
    ClientPlayNetworking.registerGlobalReceiver(Networking.EditPaintingS2C.ID, ClientNetworking::handleEditPainting);
    ClientPlayNetworking.registerGlobalReceiver(Networking.SetPaintingS2C.ID, ClientNetworking::handleSetPainting);
    ClientPlayNetworking.registerGlobalReceiver(Networking.SyncAllDataS2C.ID, ClientNetworking::handleSyncAllData);
    ClientPlayNetworking.registerGlobalReceiver(
        Networking.MigrationFinishS2C.ID, ClientNetworking::handleMigrationFinish);
    ClientPlayNetworking.registerGlobalReceiver(Networking.OpenMenuS2C.ID, ClientNetworking::handleOpenMenu);
  }

  private static void handleSummary(Networking.SummaryS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      MinecraftClient client = context.client();
      if (client.isInSingleplayer() && payload.skipped()) {
        // TODO: i18n
        Toast toast = SystemToast.create(client, SystemToast.Type.PACK_LOAD_FAILURE,
            Text.of("Custom Paintings Skipped"),
            Text.of("Skipped loading Custom Paintings packs because the world was loaded in safe mode")
        );
        client.getToastManager().add(toast);
      }
      if (client.player != null && (client.isInSingleplayer() || client.player.hasPermissionLevel(3)) &&
          payload.loadErrorOrSkipCount() > 0) {
        // TODO: i18n
        Toast toast = SystemToast.create(client, SystemToast.Type.PACK_LOAD_FAILURE,
            Text.of("Errors in Custom Painting pack loading"),
            Text.of("Some Custom Paintings packs failed to load. Check logs for details")
        );
        client.getToastManager().add(toast);
      }
      ClientPaintingRegistry.getInstance()
          .processSummary(payload.packs(), payload.serverId(), payload.combinedImageHash(),
              payload.finishedMigrations()
          );
    });
  }

  private static void handleDownloadSummary(
      Networking.DownloadSummaryS2C payload, ClientPlayNetworking.Context context
  ) {
    context.client().execute(() -> {
      ClientPaintingRegistry.getInstance()
          .trackExpectedPackets(payload.ids(), payload.imageCount(), payload.byteCount());
    });
  }

  private static void handleImage(Networking.ImageS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      CustomPaintingsMod.LOGGER.info(
          "Received full image for {} ({}KB).", payload.id(), StringUtil.formatBytes(payload.image().getSize()));
      ClientPaintingRegistry.getInstance().setPaintingImage(payload.id(), payload.image());
    });
  }

  private static void handleImageHeader(Networking.ImageHeaderS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      CustomPaintingsMod.LOGGER.info("Received image header for {}.", payload.id());
      ClientPaintingRegistry.getInstance()
          .setPaintingHeader(payload.id(), payload.width(), payload.height(), payload.totalChunks());
    });
  }

  private static void handleImageChunk(Networking.ImageChunkS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      CustomPaintingsMod.LOGGER.info("Received image chunk #{} for {} ({}KB).", payload.index(), payload.id(),
          StringUtil.formatBytes(payload.bytes().length)
      );
      ClientPaintingRegistry.getInstance().setPaintingChunk(payload.id(), payload.index(), payload.bytes());
    });
  }

  private static void handleEditPainting(Networking.EditPaintingS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      PaintingEditState state = new PaintingEditState(
          context.client(), payload.paintingId(), payload.pos(), payload.facing());

      context.client().setScreen(new PackSelectScreen(state));
    });
  }

  private static void handleSetPainting(Networking.SetPaintingS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      ClientPaintingManager.getInstance().trySetPaintingData(context.player().getWorld(), payload.assignment());
    });
  }

  private static void handleSyncAllData(Networking.SyncAllDataS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      World world = context.player().getWorld();
      for (PaintingAssignment assignment : payload.assignments()) {
        ClientPaintingManager.getInstance().trySetPaintingData(world, assignment);
      }
    });
  }

  private static void handleMigrationFinish(
      Networking.MigrationFinishS2C payload, ClientPlayNetworking.Context context
  ) {
    context.client().execute(() -> {
      ClientPaintingRegistry.getInstance().markMigrationFinished(payload.id(), payload.succeeded());
      Screen currentScreen = context.client().currentScreen;
      if (!(currentScreen instanceof MigrationsScreen screen)) {
        return;
      }
      screen.onMigrationFinished(payload.id(), payload.succeeded());
    });
  }

  private static void handleOpenMenu(
      Networking.OpenMenuS2C payload, ClientPlayNetworking.Context context
  ) {
    context.client().execute(() -> {
      MinecraftClient client = context.client();
      Screen screen = client.currentScreen;
      if (screen == null || screen instanceof ChatScreen) {
        client.setScreen(new MainMenuScreen(null));
      }
    });
  }
}
