package me.roundaround.custompaintings.client.network;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.edit.GroupSelectScreen;
import me.roundaround.custompaintings.client.gui.screen.edit.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.edit.PaintingSelectScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.ManagePaintingsScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.MismatchedPaintingsScreen;
import me.roundaround.custompaintings.client.gui.screen.manage.UnknownPaintingsScreen;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.util.Migration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class ClientNetworking {
  private ClientNetworking() {
  }

  public static void sendSetPaintingPacket(UUID paintingUuid, PaintingData customPaintingInfo) {
    ClientPlayNetworking.send(new Networking.SetPaintingC2S(paintingUuid, customPaintingInfo));
  }

  public static void sendDeclareKnownPaintingsPacket(List<PaintingData> knownPaintings) {
    ClientPlayNetworking.send(new Networking.DeclareKnownPaintingsC2S(knownPaintings));
  }

  public static void sendRequestUnknownPacket() {
    ClientPlayNetworking.send(new Networking.RequestUnknownC2S());
  }

  public static void sendRequestMismatchedPacket() {
    ClientPlayNetworking.send(new Networking.RequestMismatchedC2S());
  }

  public static void sendReassignPacket(UUID paintingUuid, Identifier id) {
    ClientPlayNetworking.send(new Networking.ReassignC2S(paintingUuid, id));
  }

  public static void sendReassignAllPacket(Identifier from, Identifier to) {
    ClientPlayNetworking.send(new Networking.ReassignAllC2S(from, to));
  }

  public static void sendUpdatePaintingPacket(UUID paintingUuid) {
    ClientPlayNetworking.send(new Networking.UpdatePaintingC2S(paintingUuid));
  }

  public static void sendRemovePaintingPacket(UUID paintingUuid) {
    ClientPlayNetworking.send(new Networking.RemovePaintingC2S(paintingUuid));
  }

  public static void sendRemoveAllPaintingsPacket(Identifier id) {
    ClientPlayNetworking.send(new Networking.RemoveAllPaintingsC2S(id));
  }

  public static void sendApplyMigrationPacket(Migration migration) {
    ClientPlayNetworking.send(new Networking.ApplyMigrationC2S(migration));
  }

  public static void registerS2CHandlers() {
    ClientPlayNetworking.registerGlobalReceiver(Networking.EditPaintingS2C.ID, ClientNetworking::handleEditPainting);
    ClientPlayNetworking.registerGlobalReceiver(
        Networking.OpenManageScreenS2C.ID, ClientNetworking::handleOpenManageScreen);
    ClientPlayNetworking.registerGlobalReceiver(Networking.ListUnknownS2C.ID, ClientNetworking::handleListUnknown);
    ClientPlayNetworking.registerGlobalReceiver(
        Networking.ListMismatchedS2C.ID, ClientNetworking::handleListMismatched);
  }

  private static void handleEditPainting(Networking.EditPaintingS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      PaintingEditState state = new PaintingEditState(
          context.client(), payload.paintingUuid(), payload.paintingId(), payload.pos(), payload.facing());

      PaintingEditScreen screen = state.hasMultipleGroups() ?
          new GroupSelectScreen(state) :
          new PaintingSelectScreen(state);

      context.client().setScreen(screen);
    });
  }

  private static void handleOpenManageScreen(
      Networking.OpenManageScreenS2C payload, ClientPlayNetworking.Context context
  ) {
    context.client().execute(() -> {
      context.client().setScreen(new ManagePaintingsScreen());
    });
  }

  private static void handleListUnknown(Networking.ListUnknownS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      if (context.client().currentScreen instanceof UnknownPaintingsScreen screen) {
        screen.setUnknownPaintings(new HashSet<>(payload.paintings()));
      }
    });
  }

  private static void handleListMismatched(Networking.ListMismatchedS2C payload, ClientPlayNetworking.Context context) {
    context.client().execute(() -> {
      if (context.client().currentScreen instanceof MismatchedPaintingsScreen screen) {
        screen.setMismatchedPaintings(new HashSet<>(payload.paintings()));
      }
    });
  }
}
