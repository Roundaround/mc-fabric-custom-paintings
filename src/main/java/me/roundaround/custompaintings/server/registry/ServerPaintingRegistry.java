package me.roundaround.custompaintings.server.registry;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.text.DecimalFormat;
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

  public void update(HashMap<String, PaintingPack> packs, HashMap<Identifier, Image> images) {
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
    HashMap<Identifier, Image> images = new HashMap<>();
    this.imageHashes.forEach((id, hash) -> {
      if (hash.equals(hashes.get(id))) {
        return;
      }
      Image image = this.images.get(id);
      if (image == null) {
        return;
      }
      images.put(id, image);
    });

    CustomPaintingsMod.LOGGER.info(
        "{} needs to download {} image(s). Sending to client.", player.getName().getString(), images.size());
    long timer = Util.getMeasuringTimeMs();
    ImagePacketQueue.getInstance().add(player, images);

    DecimalFormat format = new DecimalFormat("0.##");
    CustomPaintingsMod.LOGGER.info("Sent {} images to {} in {}s", images.size(), player.getName().getString(),
        format.format((Util.getMeasuringTimeMs() - timer) / 1000.0)
    );
  }
}
