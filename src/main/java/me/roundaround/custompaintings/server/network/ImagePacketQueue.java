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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.UUID;

public class ImagePacketQueue {
  private static ImagePacketQueue instance = null;

  public static ImagePacketQueue getInstance() {
    if (instance == null) {
      instance = new ImagePacketQueue();
    }
    return instance;
  }

  private final ArrayDeque<Entry> queue = new ArrayDeque<>();
  private final ArrayDeque<Long> sentTimestamps = new ArrayDeque<>();
  private final HashMap<UUID, ArrayDeque<Long>> perPlayerSentTimestamps = new HashMap<>();

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

    // Remove everything older than 1 second
    long timestamp = Util.getMeasuringTimeMs();
    while (!this.sentTimestamps.isEmpty() && (timestamp - this.sentTimestamps.peekFirst() > 1000)) {
      this.sentTimestamps.pollFirst();
    }
    for (HashMap.Entry<UUID, ArrayDeque<Long>> perPlayer : this.perPlayerSentTimestamps.entrySet()) {
      ArrayDeque<Long> timestamps = perPlayer.getValue();
      while (!timestamps.isEmpty() && (timestamp - timestamps.peekFirst() > 1000)) {
        timestamps.pollFirst();
      }
    }

    int maxTotal = CustomPaintingsPerWorldConfig.getInstance().maxImagePacketsPerSecond.getValue();
    int maxPerPlayer = CustomPaintingsPerWorldConfig.getInstance().maxPerClientImagePacketsPerSecond.getValue();
    ArrayDeque<Entry> deferred = new ArrayDeque<>();

    while (!this.queue.isEmpty() && this.sentTimestamps.size() <= maxTotal) {
      Entry entry = this.queue.poll();

      ArrayDeque<Long> sentToPlayer = this.perPlayerSentTimestamps.computeIfAbsent(
          entry.player().getUuid(), (uuid) -> new ArrayDeque<>());
      if (sentToPlayer.size() >= maxPerPlayer) {
        deferred.push(entry);
        continue;
      }

      sentToPlayer.push(timestamp);
      this.sentTimestamps.push(timestamp);
      ServerPlayNetworking.send(entry.player, entry.payload);
    }

    this.queue.addAll(deferred);
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

  private record Entry(ServerPlayerEntity player, CustomPayload payload) {
  }
}
