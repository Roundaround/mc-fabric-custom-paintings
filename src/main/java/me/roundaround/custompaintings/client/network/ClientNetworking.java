package me.roundaround.custompaintings.client.network;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.edit.PackSelectScreen;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.network.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class ClientNetworking {
  private ClientNetworking() {
  }

  public static void sendHashesPacket(Map<Identifier, String> hashes) {
    ClientPlayNetworking.send(new Networking.HashesC2S(hashes));
  }

  public static void sendSetPaintingPacket(int paintingId, Identifier dataId) {
    ClientPlayNetworking.send(new Networking.SetPaintingC2S(paintingId, dataId));
  }

  public static void registerS2CHandlers() {
    ClientPlayNetworking.registerGlobalReceiver(Networking.SummaryS2C.ID, ClientNetworking::handleSummary);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImagesS2C.ID, ClientNetworking::handleImages);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ImageS2C.ID, ClientNetworking::handleImage);
    ClientPlayNetworking.registerGlobalReceiver(Networking.EditPaintingS2C.ID, ClientNetworking::handleEditPainting);
    ClientPlayNetworking.registerGlobalReceiver(Networking.SetPaintingS2C.ID, ClientNetworking::handleSetPainting);
  }

  private static void handleSummary(Networking.SummaryS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      HashMap<String, PaintingPack> packs = new HashMap<>(payload.packs().size());
      payload.packs().forEach((pack) -> packs.put(pack.id(), pack));
      ClientPaintingRegistry.getInstance().setPacks(packs);

      ClientPaintingRegistry.getInstance().checkCombinedImageHash(payload.combinedImageHash());
    });
  }

  private static void handleImages(Networking.ImagesS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      ClientPaintingRegistry.getInstance().trackNeededImages(payload.ids());
    });
  }

  private static void handleImage(Networking.ImageS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      ClientPaintingRegistry.getInstance().setPaintingImage(payload.id(), payload.image());
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
      PaintingData paintingData = ClientPaintingRegistry.getInstance().get(payload.dataId());
      if (paintingData == null || paintingData.isEmpty()) {
        return;
      }

      Entity entity = context.player().getWorld().getEntityById(payload.paintingId());
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }

      if (paintingData.isVanilla()) {
        painting.setVariant(paintingData.id());
      }
      painting.setCustomData(paintingData);
    });
  }
}
