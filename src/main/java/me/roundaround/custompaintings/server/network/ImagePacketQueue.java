package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.network.CustomId;
import me.roundaround.custompaintings.network.Networking;
import me.roundaround.custompaintings.resource.Image;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
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

  public void add(ServerPlayerEntity player, HashMap<CustomId, Image> images) {
    ArrayDeque<Entry> queue = new ArrayDeque<>();
    Summary summary = images.entrySet()
        .stream()
        .map((entry) -> this.add(queue, player, entry.getKey(), entry.getValue()))
        .reduce(Summary.empty(), Summary::merge);

    ServerNetworking.sendDownloadSummaryPacket(player, images.keySet(), summary.imageCount, summary.byteCount);

    while (!queue.isEmpty()) {
      Entry entry = queue.poll();
      if (isThrottled()) {
        this.queue.push(entry);
      } else {
        ServerPlayNetworking.send(entry.player, entry.payload);
      }
    }
  }

  private Summary add(ArrayDeque<Entry> queue, ServerPlayerEntity player, CustomId id, Image image) {
    int maxBytes = CustomPaintingsPerWorldConfig.getInstance().maxImagePacketSize.getValue() * 1024;
    if (!isThrottled() || maxBytes == 0 || image.getSize() <= maxBytes) {
      queue.add(new Entry(player, new Networking.ImageS2C(id, image)));
      return Summary.singlePacketImage(image);
    }

    byte[] bytes = image.getBytes();
    int totalSize = bytes.length;
    ArrayDeque<Networking.ImageChunkS2C> chunks = new ArrayDeque<>();
    Summary summary = Summary.multiPacketImage();

    for (int start = 0, i = 0; start < totalSize; start += maxBytes, i++) {
      int end = Math.min(start + maxBytes, totalSize);
      byte[] chunk = new byte[end - start];
      System.arraycopy(bytes, start, chunk, 0, end - start);
      chunks.add(new Networking.ImageChunkS2C(id, i, chunk));
      summary.add(chunk.length);
    }

    queue.add(new Entry(player, new Networking.ImageHeaderS2C(id, image.width(), image.height(), chunks.size())));
    while (!chunks.isEmpty()) {
      queue.add(new Entry(player, chunks.pop()));
    }

    return summary;
  }

  public void tick() {
    if (this.queue.isEmpty()) {
      return;
    }

    if (!isThrottled()) {
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

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isThrottled() {
    if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
      return false;
    }
    return CustomPaintingsPerWorldConfig.getInstance().throttleImageDownloads.getValue();
  }

  private record Entry(ServerPlayerEntity player, CustomPayload payload) {
  }

  public static class Summary {
    public int imageCount;
    public int byteCount;

    private Summary(int imageCount, int byteCount) {
      this.imageCount = imageCount;
      this.byteCount = byteCount;
    }

    public static Summary empty() {
      return new Summary(0, 0);
    }

    public static Summary singlePacketImage(Image image) {
      return new Summary(1, image.getSize());
    }

    public static Summary multiPacketImage() {
      return new Summary(1, 0);
    }

    public void add(int byteCount) {
      this.byteCount += byteCount;
    }

    public static Summary merge(Summary a, Summary b) {
      a.imageCount += b.imageCount;
      a.byteCount += b.byteCount;
      return a;
    }
  }
}
