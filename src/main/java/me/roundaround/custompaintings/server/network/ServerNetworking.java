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
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

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
    server.getPlayerList()
        .getPlayers()
        .forEach((player) -> sendSummaryPacket(
            player,
            packs,
            combinedImageHash,
            finishedMigrations,
            skipped,
            loadErrorOrSkipCount
        ));
  }

  public static void sendSummaryPacket(
      ServerPlayer player,
      List<PackData> packs,
      String combinedImageHash,
      Map<CustomId, Boolean> finishedMigrations,
      boolean skipped,
      int loadErrorOrSkipCount
  ) {
    if (!ServerPlayNetworking.canSend(player, Networking.SUMMARY_S2C)) {
      player.sendSystemMessage(CustomPaintingsServerMod.getDownloadPrompt());
      return;
    }
    UUID serverId = ServerInfo.getInstance().getServerId();
    ServerPlayNetworking.send(
        player,
        new Networking.SummaryS2C(serverId, packs, combinedImageHash, finishedMigrations, skipped, loadErrorOrSkipCount)
    );
  }

  public static void sendDownloadSummaryPacket(
      ServerPlayer player,
      Collection<CustomId> ids,
      int imageCount,
      int byteCount
  ) {
    ServerPlayNetworking.send(player, new Networking.DownloadSummaryS2C(ids.stream().toList(), imageCount, byteCount));
  }

  public static void sendEditPaintingPacket(
      ServerPlayer player,
      UUID paintingUuid,
      int paintingId,
      BlockPos pos,
      Direction facing
  ) {
    ServerPlayNetworking.send(player, new Networking.EditPaintingS2C(paintingUuid, paintingId, pos, facing));
  }

  public static void sendSetPaintingPacketToAll(ServerLevel world, PaintingAssignment assignment) {
    Networking.SetPaintingS2C payload = new Networking.SetPaintingS2C(assignment);
    world.players().forEach((player) -> {
      sendSetPaintingPacket(player, payload);
    });
  }

  public static void sendSetPaintingPacket(ServerPlayer player, Networking.SetPaintingS2C payload) {
    if (ServerPlayNetworking.canSend(player, payload.type())) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  public static void sendSyncAllDataPacket(ServerPlayer player, List<PaintingAssignment> assignments) {
    if (ServerPlayNetworking.canSend(player, Networking.SyncAllDataS2C.ID)) {
      ServerPlayNetworking.send(player, new Networking.SyncAllDataS2C(assignments));
    }
  }

  public static void sendMigrationFinishPacketToAll(MinecraftServer server, CustomId id, boolean succeeded) {
    Networking.MigrationFinishS2C payload = new Networking.MigrationFinishS2C(id, succeeded);
    server.getPlayerList().getPlayers().forEach((player) -> {
      sendMigrationFinishPacket(player, payload);
    });
  }

  public static void sendMigrationFinishPacket(ServerPlayer player, Networking.MigrationFinishS2C payload) {
    if (ServerPlayNetworking.canSend(player, Networking.MigrationFinishS2C.ID)) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  public static void sendOpenMenuPacket(ServerPlayer player) {
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
    context.server().execute(() -> {
      ServerPaintingRegistry.getInstance().checkPlayerHashes(context.player(), payload.hashes());
    });
  }

  private static void handleReload(Networking.ReloadC2S payload, ServerPlayNetworking.Context context) {
    context.server().execute(() -> {
      if (!context.player().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
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
    context.server().execute(() -> {
      ServerPlayer player = context.player();
      ServerLevel world = player.level();
      Entity entity = world.getEntity(payload.paintingId());
      if (!(entity instanceof Painting painting)) {
        return;
      }

      if (painting.custompaintings$getEditor() == null ||
          !painting.custompaintings$getEditor().equals(player.getUUID())) {
        return;
      }

      PaintingData paintingData = ServerPaintingRegistry.getInstance().get(payload.dataId());
      if (paintingData == null || paintingData.isEmpty()) {
        painting.custompaintings$setEditor(null);
        painting.hurtServer(world, player.damageSources().playerAttack(player), 0f);
        return;
      }

      if (paintingData.vanilla()) {
        painting.custompaintings$setVariant(paintingData.id());
      }
      painting.custompaintings$setData(paintingData);

      if (!painting.survives()) {
        painting.custompaintings$setEditor(null);
        painting.hurtServer(world, player.damageSources().playerAttack(player), 0f);
        return;
      }

      world.custompaintings$getPaintingManager().setPaintingData(painting, paintingData);
      painting.custompaintings$setEditor(null);
    });
  }

  private static void handleRunMigration(Networking.RunMigrationC2S payload, ServerPlayNetworking.Context context) {
    context.server().execute(() -> {
      ServerPaintingManager.runMigration(context.player(), payload.id());
    });
  }
}
