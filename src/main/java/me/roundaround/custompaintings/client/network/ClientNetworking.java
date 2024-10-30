package me.roundaround.custompaintings.client.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.ClientPaintingManager;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.edit.PackSelectScreen;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.network.PaintingAssignment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.text.DecimalFormat;
import java.util.Map;

public final class ClientNetworking {
  private ClientNetworking() {
  }

  public static void sendHashesPacket(Map<Identifier, String> hashes) {
    ClientPlayNetworking.send(new Networking.HashesC2S(hashes));
  }

  public static void sendReloadPacket() {
    ClientPlayNetworking.send(new Networking.ReloadC2S());
  }

  public static void sendSetPaintingPacket(int paintingId, Identifier dataId) {
    ClientPlayNetworking.send(new Networking.SetPaintingC2S(paintingId, dataId));
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
  }

  private static void handleSummary(Networking.SummaryS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      if (context.client().isInSingleplayer() && payload.skipped()) {
        context.player().sendMessage(Text.translatable("custompaintings.loadingSkipped"));
      }
      ClientPaintingRegistry.getInstance()
          .processSummary(payload.packs(), payload.serverId(), payload.combinedImageHash());
    });
  }

  private static void handleDownloadSummary(
      Networking.DownloadSummaryS2C payload, ClientPlayNetworking.Context context
  ) {
    context.client().execute(() -> {
      ClientPaintingRegistry.getInstance()
          .trackExpectedPackets(payload.ids(), payload.imageCount(), payload.packetCount(), payload.byteCount());
    });
  }

  private static void handleImage(Networking.ImageS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      CustomPaintingsMod.LOGGER.info(
          "Received full image for {} ({}KB).", payload.id(), formatBytes(payload.image().getSize()));
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
          formatBytes(payload.bytes().length)
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

  private static String formatBytes(int bytes) {
    return new DecimalFormat("0.##").format(bytes / 1024f);
  }
}
