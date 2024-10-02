package me.roundaround.custompaintings.client.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.ClientPaintingManager;
import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.edit.PackSelectScreen;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.network.PaintingIdPair;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientNetworking {
  private static final AtomicReference<Networking.SyncAllDataS2C> cachedSyncPayloadRef = new AtomicReference<>();

  private ClientNetworking() {
  }

  public static void sendHashesPacket(Map<Identifier, String> hashes) {
    ClientPlayNetworking.send(new Networking.HashesC2S(hashes));
  }

  public static void sendSetPaintingPacket(int paintingId, Identifier dataId) {
    ClientPlayNetworking.send(new Networking.SetPaintingC2S(paintingId, dataId));
  }

  public static void registerReceivers() {
    ClientPlayNetworking.registerGlobalReceiver(Networking.SummaryS2C.ID, ClientNetworking::handleSummary);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageS2C.ID, ClientNetworking::handleImage);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageIdsS2C.ID, ClientNetworking::handleImageIds);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageHeaderS2C.ID, ClientNetworking::handleImageHeader);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageChunkS2C.ID, ClientNetworking::handleImageChunk);
    ClientPlayNetworking.registerGlobalReceiver(Networking.EditPaintingS2C.ID, ClientNetworking::handleEditPainting);
    ClientPlayNetworking.registerGlobalReceiver(Networking.SetPaintingS2C.ID, ClientNetworking::handleSetPainting);
    ClientPlayNetworking.registerGlobalReceiver(Networking.SyncAllDataS2C.ID, ClientNetworking::handleSyncAllData);
  }

  private static void handleSummary(Networking.SummaryS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      HashMap<String, PaintingPack> packs = new HashMap<>(payload.packs().size());
      payload.packs().forEach((pack) -> packs.put(pack.id(), pack));
      ClientPaintingRegistry.getInstance().setPacks(packs);

      ClientPaintingRegistry.getInstance().checkCombinedImageHash(payload.combinedImageHash());

      Networking.SyncAllDataS2C cachedSyncPayload = cachedSyncPayloadRef.getAndSet(null);
      if (cachedSyncPayload != null) {
        processSyncPayload(context.client().world, cachedSyncPayload);
      }
    });
  }

  private static void handleImage(Networking.ImageS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      CustomPaintingsMod.LOGGER.info(
          "Received full image for {} ({}KB).", payload.id(), formatBytes(payload.image().getBytes().length));
      ClientPaintingRegistry.getInstance().setPaintingImage(payload.id(), payload.image());
    });
  }

  private static void handleImageIds(Networking.ImageIdsS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      ClientPaintingRegistry.getInstance().trackNeededImages(payload.ids());
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
      ClientPaintingManager.getInstance()
          .trySetPaintingData(context.player().getWorld(), payload.paintingId(), payload.dataId());
    });
  }

  private static void handleSyncAllData(Networking.SyncAllDataS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      if (!ClientPaintingRegistry.getInstance().hasReceivedPacks()) {
        cachedSyncPayloadRef.set(payload);
        return;
      }
      processSyncPayload(context.client().world, payload);
    });
  }

  private static void processSyncPayload(World world, Networking.SyncAllDataS2C payload) {
    for (PaintingIdPair ids : payload.paintings()) {
      ClientPaintingManager.getInstance().trySetPaintingData(world, ids.paintingId(), ids.dataId());
    }
  }

  private static String formatBytes(int bytes) {
    return new DecimalFormat("0.##").format(bytes / 1024f);
  }
}
