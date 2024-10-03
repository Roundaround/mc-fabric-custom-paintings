package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.resource.Image;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;

public class ImagePacketQueue {
  private static ImagePacketQueue instance = null;

  public static ImagePacketQueue getInstance() {
    if (instance == null) {
      instance = new ImagePacketQueue();
    }
    return instance;
  }

  private final ArrayDeque<Entry> queue = new ArrayDeque<>();

  private long lastSend = -1L;

  private ImagePacketQueue() {
  }

  public void tick() {
    if (this.queue.isEmpty()) {
      return;
    }

    if (!this.isThrottled()) {
      while (!this.queue.isEmpty()) {
        Entry entry = this.queue.poll();
        ServerPlayNetworking.send(entry.player, entry.payload);
      }
    }

    if (Util.getMeasuringTimeMs() - this.lastSend > this.getSendCooldown()) {
      Entry entry = this.queue.poll();
      if (entry != null) {
        this.lastSend = Util.getMeasuringTimeMs();
        ServerPlayNetworking.send(entry.player, entry.payload);
      }
    }
  }

  public void add(ServerPlayerEntity player, Identifier id, Image image) {
    if (!this.isThrottled()) {
      ServerPlayNetworking.send(player, new Networking.ImageS2C(id, image));
      return;
    }

    int maxBytes = CustomPaintingsPerWorldConfig.getInstance().maxImagePacketSize.getValue() * 1024;
    if (maxBytes == 0 || image.getSize() <= maxBytes) {
      this.queue.add(new Entry(player, new Networking.ImageS2C(id, image)));
      return;
    }

    byte[] bytes = image.getBytes();
    int totalSize = bytes.length;
    ArrayDeque<Networking.ImageChunkS2C> chunks = new ArrayDeque<>();

    for (int start = 0, i = 0; start < totalSize; start += maxBytes, i++) {
      int end = Math.min(start + maxBytes, totalSize);
      byte[] chunk = new byte[end - start];
      System.arraycopy(bytes, start, chunk, 0, end - start);
      chunks.add(new Networking.ImageChunkS2C(id, i, chunk));
    }

    this.queue.add(new Entry(player, new Networking.ImageHeaderS2C(id, image.width(), image.height(), chunks.size())));

    while (!chunks.isEmpty()) {
      this.queue.add(new Entry(player, chunks.pop()));
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isThrottled() {
    if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
      return false;
    }
    return CustomPaintingsPerWorldConfig.getInstance().throttleImageDownloads.getValue();
  }

  private int getSendCooldown() {
    return MathHelper.floor(1000f / CustomPaintingsPerWorldConfig.getInstance().maxImagePacketsPerSecond.getValue());
  }

  private record Entry(ServerPlayerEntity player, CustomPayload payload) {
  }
}
