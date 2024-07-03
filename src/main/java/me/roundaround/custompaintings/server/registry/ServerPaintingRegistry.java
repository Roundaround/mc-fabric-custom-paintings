package me.roundaround.custompaintings.server.registry;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.PaintingImage;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServerPaintingRegistry extends CustomPaintingRegistry {
  private static ServerPaintingRegistry instance = null;

  private MinecraftServer server;

  private ServerPaintingRegistry() {
  }

  public static void init(MinecraftServer server) {
    getInstance().setServer(server);
  }

  public static ServerPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new ServerPaintingRegistry();
    }
    return instance;
  }

  @Override
  public void close() {
    super.close();
    this.server = null;
  }

  public void setServer(MinecraftServer server) {
    this.server = server;
  }

  public void update(HashMap<String, PaintingPack> packs, HashMap<Identifier, PaintingImage> images) {
    this.setPacks(packs);
    this.setImages(images);

    if (this.server == null) {
      return;
    }

    ServerNetworking.sendSummaryPacketToAll(this.server, this.packsList, this.combinedImageHash);
  }

  public void sendSummaryToPlayer(ServerPlayerEntity player) {
    ServerNetworking.sendSummaryPacket(player, this.packsList, this.combinedImageHash);
  }

  public void checkPlayerHashes(ServerPlayerEntity player, Map<Identifier, String> hashes) {
    ArrayList<Identifier> idsToSend = new ArrayList<>();
    this.imageHashes.forEach((id, hash) -> {
      if (!hash.equals(hashes.get(id))) {
        idsToSend.add(id);
      }
    });
    ServerNetworking.sendImagesPacket(player, idsToSend);

    idsToSend.forEach((id) -> {
      PaintingImage image = this.images.get(id);
      if (image == null) {
        return;
      }
      ServerNetworking.sendImagePacket(player, id, image);
    });
  }
}
