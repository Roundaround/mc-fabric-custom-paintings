package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.network.PaintingIdPair;
import me.roundaround.custompaintings.resource.PaintingImage;
import me.roundaround.custompaintings.server.CustomPaintingsServerMod;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.UUID;

public final class ServerNetworking {
  private ServerNetworking() {
  }

  public static void sendSummaryPacketToAll(
      MinecraftServer server, List<PaintingPack> packs, String combinedImageHash
  ) {
    Networking.SummaryS2C payload = new Networking.SummaryS2C(packs, combinedImageHash);
    server.getPlayerManager().getPlayerList().forEach((player) -> {
      sendSummaryPacket(player, payload);
    });
  }

  public static void sendSummaryPacket(ServerPlayerEntity player, List<PaintingPack> packs, String combinedImageHash) {
    sendSummaryPacket(player, new Networking.SummaryS2C(packs, combinedImageHash));
  }

  private static void sendSummaryPacket(ServerPlayerEntity player, Networking.SummaryS2C payload) {
    if (!ServerPlayNetworking.canSend(player, payload.getId())) {
      player.sendMessage(CustomPaintingsServerMod.getDownloadPrompt());
      return;
    }
    ServerPlayNetworking.send(player, payload);
  }

  public static void sendImagesPacket(ServerPlayerEntity player, List<Identifier> ids) {
    ServerPlayNetworking.send(player, new Networking.ImagesS2C(ids));
  }

  public static void sendImagePacket(ServerPlayerEntity player, Identifier id, PaintingImage image) {
    Networking.ImageS2C payload = new Networking.ImageS2C(id, image);
    if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
      ImagePacketQueue.getInstance().add(player, payload);
    } else {
      ServerPlayNetworking.send(player, payload);
    }
  }

  public static void sendEditPaintingPacket(
      ServerPlayerEntity player, UUID paintingUuid, int paintingId, BlockPos pos, Direction facing
  ) {
    ServerPlayNetworking.send(player, new Networking.EditPaintingS2C(paintingUuid, paintingId, pos, facing));
  }

  public static void sendSetPaintingPacketToAll(MinecraftServer server, int paintingId, Identifier dataId) {
    Networking.SetPaintingS2C payload = new Networking.SetPaintingS2C(paintingId, dataId);
    server.getPlayerManager().getPlayerList().forEach((player) -> {
      sendSetPaintingPacket(player, payload);
    });
  }

  public static void sendSetPaintingPacket(ServerPlayerEntity player, Networking.SetPaintingS2C payload) {
    if (ServerPlayNetworking.canSend(player, payload.getId())) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  public static void sendSyncAllDataPacket(ServerPlayerEntity player, List<PaintingIdPair> ids) {
    if (ServerPlayNetworking.canSend(player, Networking.SyncAllDataS2C.ID)) {
      ServerPlayNetworking.send(player, new Networking.SyncAllDataS2C(ids));
    }
  }

  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(Networking.HashesC2S.ID, ServerNetworking::handleHashes);
    ServerPlayNetworking.registerGlobalReceiver(Networking.SetPaintingC2S.ID, ServerNetworking::handleSetPainting);
  }

  private static void handleHashes(Networking.HashesC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPaintingRegistry.getInstance().checkPlayerHashes(context.player(), payload.hashes());
    });
  }

  private static void handleSetPainting(Networking.SetPaintingC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPlayerEntity player = context.player();
      ServerWorld world = player.getServerWorld();
      Entity entity = world.getEntityById(payload.paintingId());
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }

      if (painting.getEditor() == null || !painting.getEditor().equals(player.getUuid())) {
        return;
      }

      PaintingData paintingData = ServerPaintingRegistry.getInstance().get(payload.dataId());
      if (paintingData == null || paintingData.isEmpty()) {
        entity.damage(player.getDamageSources().playerAttack(player), 0f);
        return;
      }

      if (paintingData.isVanilla()) {
        painting.setVariant(paintingData.id());
      }
      painting.setCustomData(paintingData);

      if (!painting.canStayAttached()) {
        painting.damage(player.getDamageSources().playerAttack(player), 0f);
        return;
      }

      ServerPaintingManager.getInstance(world).setPaintingData(painting, paintingData);
    });
  }
}
