package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.resource.PaintingImage;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
    server.getPlayerManager().getPlayerList().forEach((player) -> {
      ServerPlayNetworking.send(player, new Networking.SummaryS2C(packs, combinedImageHash));
    });
  }

  public static void sendSummaryPacket(ServerPlayerEntity player, List<PaintingPack> packs, String combinedImageHash) {
    ServerPlayNetworking.send(player, new Networking.SummaryS2C(packs, combinedImageHash));
  }

  public static void sendImagesPacket(ServerPlayerEntity player, List<Identifier> ids) {
    ServerPlayNetworking.send(player, new Networking.ImagesS2C(ids));
  }

  public static void sendImagePacket(ServerPlayerEntity player, Identifier id, PaintingImage image) {
    ServerPlayNetworking.send(player, new Networking.ImageS2C(id, image));
  }

  public static void sendEditPaintingPacket(
      ServerPlayerEntity player, UUID paintingUuid, int paintingId, BlockPos pos, Direction facing
  ) {
    ServerPlayNetworking.send(player, new Networking.EditPaintingS2C(paintingUuid, paintingId, pos, facing));
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
      Entity entity = world.getEntity(payload.paintingUuid());
      if (!(entity instanceof PaintingEntity painting)) {
        return;
      }

      if (painting.getEditor() == null || !painting.getEditor().equals(player.getUuid())) {
        return;
      }

      if (payload.customPaintingInfo().isEmpty()) {
        entity.damage(player.getDamageSources().playerAttack(player), 0f);
        return;
      }

      if (payload.customPaintingInfo().isVanilla()) {
        painting.setVariant(payload.customPaintingInfo().id());
      }

      ServerPaintingManager.getInstance(world).setPaintingDataAndPropagate(painting, payload.customPaintingInfo());

      if (!painting.canStayAttached()) {
        painting.damage(player.getDamageSources().playerAttack(player), 0f);
      }
    });
  }
}
