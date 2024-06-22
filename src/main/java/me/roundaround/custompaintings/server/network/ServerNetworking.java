package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class ServerNetworking {
  private ServerNetworking() {
  }

  public static void sendEditPaintingPacket(
      ServerPlayerEntity player, UUID paintingUuid, int paintingId, BlockPos pos, Direction facing
  ) {
    ServerPlayNetworking.send(player, new Networking.EditPaintingS2C(paintingUuid, paintingId, pos, facing));
  }

  public static void sendOpenManageScreenPacket(ServerPlayerEntity player) {
    ServerPlayNetworking.send(player, new Networking.OpenManageScreenS2C());
  }

  private static void sendListUnknownPacket(ServerPlayerEntity player, Collection<UnknownPainting> paintings) {
    ServerPlayNetworking.send(player, new Networking.ListUnknownS2C(List.copyOf(paintings)));
  }

  private static void sendListMismatchedPacket(ServerPlayerEntity player, Collection<MismatchedPainting> paintings) {
    ServerPlayNetworking.send(player, new Networking.ListMismatchedS2C(List.copyOf(paintings)));
  }

  public static void registerReceivers() {
    ServerPlayNetworking.registerGlobalReceiver(Networking.SetPaintingC2S.ID, ServerNetworking::handleSetPainting);
    ServerPlayNetworking.registerGlobalReceiver(Networking.DeclareKnownPaintingsC2S.ID,
        ServerNetworking::handleDeclareKnownPaintings
    );
    ServerPlayNetworking.registerGlobalReceiver(Networking.RequestUnknownC2S.ID,
        ServerNetworking::handleRequestUnknown
    );
    ServerPlayNetworking.registerGlobalReceiver(Networking.RequestMismatchedC2S.ID,
        ServerNetworking::handleRequestMismatched
    );
    ServerPlayNetworking.registerGlobalReceiver(Networking.ReassignC2S.ID, ServerNetworking::handleReassign);
    ServerPlayNetworking.registerGlobalReceiver(Networking.ReassignAllC2S.ID, ServerNetworking::handleReassignAll);
    ServerPlayNetworking.registerGlobalReceiver(Networking.UpdatePaintingC2S.ID,
        ServerNetworking::handleUpdatePainting
    );
    ServerPlayNetworking.registerGlobalReceiver(Networking.RemovePaintingC2S.ID,
        ServerNetworking::handleRemovePainting
    );
    ServerPlayNetworking.registerGlobalReceiver(Networking.RemoveAllPaintingsC2S.ID,
        ServerNetworking::handleRemoveAllPaintings
    );
    ServerPlayNetworking.registerGlobalReceiver(Networking.ApplyMigrationC2S.ID,
        ServerNetworking::handleApplyMigration
    );
  }

  private static void handleSetPainting(Networking.SetPaintingC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      Entity entity = context.player().getServerWorld().getEntity(payload.paintingUuid());
      if (!(entity instanceof PaintingEntity basePainting)) {
        return;
      }

      ExpandedPaintingEntity painting = (ExpandedPaintingEntity) basePainting;
      if (painting.getEditor() == null || !painting.getEditor().equals(context.player().getUuid())) {
        return;
      }

      if (payload.customPaintingInfo().isEmpty()) {
        entity.damage(context.player().getDamageSources().playerAttack(context.player()), 0f);
        return;
      }

      if (payload.customPaintingInfo().isVanilla()) {
        painting.setVariant(payload.customPaintingInfo().id());
      }

      painting.setCustomData(payload.customPaintingInfo());

      if (!basePainting.canStayAttached()) {
        basePainting.damage(context.player().getDamageSources().playerAttack(context.player()), 0f);
      }
    });
  }

  private static void handleDeclareKnownPaintings(
      Networking.DeclareKnownPaintingsC2S payload, ServerPlayNetworking.Context context
  ) {
    CustomPaintingsMod.knownPaintings.put(context.player().getUuid(), new HashSet<>(payload.paintings()));
  }

  private static void handleRequestUnknown(Networking.RequestUnknownC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      sendListUnknownPacket(context.player(), ServerPaintingManager.getUnknownPaintings(context.player()));
    });
  }

  private static void handleRequestMismatched(
      Networking.RequestMismatchedC2S payload, ServerPlayNetworking.Context context
  ) {
    context.player().server.execute(() -> {
      sendListMismatchedPacket(context.player(), ServerPaintingManager.getMismatchedPaintings(context.player()));
    });
  }

  private static void handleReassign(Networking.ReassignC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPaintingManager.reassign(context.player(), payload.paintingUuid(), payload.id());
      sendListUnknownPacket(context.player(), ServerPaintingManager.getUnknownPaintings(context.player()));
      sendListMismatchedPacket(context.player(), ServerPaintingManager.getMismatchedPaintings(context.player()));
    });
  }

  private static void handleReassignAll(Networking.ReassignAllC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPaintingManager.reassign(context.player(), payload.from(), payload.to());
      sendListUnknownPacket(context.player(), ServerPaintingManager.getUnknownPaintings(context.player()));
      sendListMismatchedPacket(context.player(), ServerPaintingManager.getMismatchedPaintings(context.player()));
    });
  }

  private static void handleUpdatePainting(Networking.UpdatePaintingC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPaintingManager.updatePainting(context.player(), payload.paintingUuid());
      sendListMismatchedPacket(context.player(), ServerPaintingManager.getMismatchedPaintings(context.player()));
    });
  }

  private static void handleRemovePainting(Networking.RemovePaintingC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      ServerPaintingManager.removePainting(context.player(), payload.paintingUuid());
      sendListUnknownPacket(context.player(), ServerPaintingManager.getUnknownPaintings(context.player()));
      sendListMismatchedPacket(context.player(), ServerPaintingManager.getMismatchedPaintings(context.player()));
    });
  }

  private static void handleRemoveAllPaintings(
      Networking.RemoveAllPaintingsC2S payload, ServerPlayNetworking.Context context
  ) {
    context.player().server.execute(() -> {
      ServerPaintingManager.removePaintings(context.player(), payload.id());
      sendListUnknownPacket(context.player(), ServerPaintingManager.getUnknownPaintings(context.player()));
      sendListMismatchedPacket(context.player(), ServerPaintingManager.getMismatchedPaintings(context.player()));
    });
  }

  private static void handleApplyMigration(Networking.ApplyMigrationC2S payload, ServerPlayNetworking.Context context) {
    context.player().server.execute(() -> {
      int updated = ServerPaintingManager.applyMigration(context.player(), payload.migration());
      if (updated == 0) {
        context.player().sendMessage(Text.translatable("custompaintings.migrations.none"), false);
      } else {
        context.player().sendMessage(Text.translatable("custompaintings.migrations.success", updated), false);
      }
    });
  }
}
