package me.roundaround.custompaintings.network;

import java.util.UUID;

import io.netty.buffer.Unpooled;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EditPaintingPacket {
  private static final Identifier EDIT_PAINTING_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "edit_painting_packet");

  public static void registerReceive() {
    ClientPlayNetworking.registerGlobalReceiver(EDIT_PAINTING_PACKET, ((client, handler, buffer, responseSender) -> {
      UUID paintingUuid = buffer.readUuid();
      BlockPos pos = buffer.readBlockPos();
      Direction facing = Direction.byId(buffer.readInt());

      client.execute(() -> {
        client.setScreen(new PaintingEditScreen(paintingUuid, pos, facing));
      });
    }));
  }

  public static void sendToPlayer(ServerPlayerEntity player, UUID paintingUuid, BlockPos pos, Direction facing) {
    PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    buf.writeUuid(paintingUuid);
    buf.writeBlockPos(pos);
    buf.writeInt(facing.getId());
    ServerPlayNetworking.send(player, EDIT_PAINTING_PACKET, buf);
  }
}
