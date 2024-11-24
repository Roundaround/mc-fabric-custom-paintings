package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.network.PaintingAssignment;
import me.roundaround.custompaintings.server.CustomPaintingsServerMod;
import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.custompaintings.util.CustomId;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ServerNetworking {
  private ServerNetworking() {
  }

  public static void sendSummaryPacketToAll(
      MinecraftServer server,
      List<PackData> packs,
      String combinedImageHash,
      Map<CustomId, Boolean> finishedMigrations,
      boolean skipped,
      int loadErrorOrSkipCount
  ) {
    server.getPlayerManager()
        .getPlayerList()
        .forEach((player) -> sendSummaryPacket(player, packs, combinedImageHash, finishedMigrations, skipped,
            loadErrorOrSkipCount
        ));
  }

  public static void sendSummaryPacket(
      ServerPlayerEntity player,
      List<PackData> packs,
      String combinedImageHash,
      Map<CustomId, Boolean> finishedMigrations,
      boolean skipped,
      int loadErrorOrSkipCount
  ) {
    if (!ServerPlayNetworking.canSend(player, Networking.SUMMARY_S2C)) {
      player.sendMessage(CustomPaintingsServerMod.getDownloadPrompt());
      return;
    }
    UUID serverId = ServerInfo.getInstance().getServerId();
    ServerPlayNetworking.send(player,
        new Networking.SummaryS2C(serverId, packs, combinedImageHash, finishedMigrations, skipped, loadErrorOrSkipCount)
    );
  }

  public static void sendDownloadSummaryPacket(
      ServerPlayerEntity player, Collection<CustomId> ids, int imageCount, int byteCount
  ) {
    ServerPlayNetworking.send(player, new Networking.DownloadSummaryS2C(ids.stream().toList(), imageCount, byteCount));
  }

  public static void sendEditPaintingPacket(
      ServerPlayerEntity player, UUID paintingUuid, int paintingId, BlockPos pos, Direction facing
  ) {
    ServerPlayNetworking.send(player, new Networking.EditPaintingS2C(paintingUuid, paintingId, pos, facing));
  }

  public static void sendSetPaintingPacketToAll(ServerWorld world, PaintingAssignment assignment) {
    Networking.SetPaintingS2C payload = new Networking.SetPaintingS2C(assignment);
    world.getPlayers().forEach((player) -> {
      sendSetPaintingPacket(player, payload);
    });
  }

  public static void sendSetPaintingPacket(ServerPlayerEntity player, Networking.SetPaintingS2C payload) {
    if (ServerPlayNetworking.canSend(player, payload.getId())) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  public static void sendSyncAllDataPacket(ServerPlayerEntity player, List<PaintingAssignment> assignments) {
    if (ServerPlayNetworking.canSend(player, Networking.SyncAllDataS2C.ID)) {
      ServerPlayNetworking.send(player, new Networking.SyncAllDataS2C(assignments));
    }
  }

  public static void sendMigrationFinishPacketToAll(
      MinecraftServer server, CustomId id, boolean succeeded
  ) {
    Networking.MigrationFinishS2C payload = new Networking.MigrationFinishS2C(id, succeeded);
    server.getPlayerManager().getPlayerList().forEach((player) -> {
      sendMigrationFinishPacket(player, payload);
    });
  }

  public static void sendMigrationFinishPacket(ServerPlayerEntity player, Networking.MigrationFinishS2C payload) {
    if (ServerPlayNetworking.canSend(player, Networking.MigrationFinishS2C.ID)) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  public static void sendOpenMenuPacket(ServerPlayerEntity player) {
    if (ServerPlayNetworking.canSend(player, Networking.OpenMenuS2C.ID)) {
      ServerPlayNetworking.send(player, new Networking.OpenMenuS2C());
    }
  }

  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(Networking.HashesC2S.ID, ServerNetworking::handleHashes);
    ServerPlayNetworking.registerGlobalReceiver(Networking.ReloadC2S.ID, ServerNetworking::handleReload);
    ServerPlayNetworking.registerGlobalReceiver(Networking.SetPaintingC2S.ID, ServerNetworking::handleSetPainting);
    ServerPlayNetworking.registerGlobalReceiver(Networking.RunMigrationC2S.ID, ServerNetworking::handleRunMigration);
  }

  private static void handleHashes(Networking.HashesC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPaintingRegistry.getInstance().checkPlayerHashes(context.player(), payload.hashes());
    });
  }

  private static void handleReload(Networking.ReloadC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      if (!context.player().hasPermissionLevel(3)) {
        return;
      }
      ServerInfo serverInfo = ServerInfo.getInstance();
      for (String packFileUid : payload.toActivate()) {
        serverInfo.markPackEnabled(packFileUid);
      }
      for (String packFileUid : payload.toDeactivate()) {
        serverInfo.markPackDisabled(packFileUid);
      }
      ServerPaintingRegistry.getInstance().reloadPaintingPacks(ServerPaintingManager::syncAllDataForAllPlayers);
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
        painting.setEditor(null);
        painting.damage(world, player.getDamageSources().playerAttack(player), 0f);
        return;
      }

      if (paintingData.vanilla()) {
        painting.setVariant(paintingData.id());
      }
      painting.setCustomData(paintingData);

      if (!painting.canStayAttached()) {
        painting.setEditor(null);
        painting.damage(world, player.getDamageSources().playerAttack(player), 0f);
        return;
      }

      ServerPaintingManager.getInstance(world).setPaintingData(painting, paintingData);
      painting.setEditor(null);
    });
  }

  private static void handleRunMigration(Networking.RunMigrationC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPaintingManager.runMigration(context.player(), payload.id());
    });
  }
}
