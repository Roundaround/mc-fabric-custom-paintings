package me.roundaround.custompaintings.server.network;

import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
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

  private boolean isThrottled;
  private int sendCooldown;
  private long lastSend = -1L;

  private ImagePacketQueue() {
    this.updateThrottleSettings();
  }

  public void tick() {
    if (this.queue.isEmpty()) {
      return;
    }

    if (!this.isThrottled) {
      while (!this.queue.isEmpty()) {
        Entry entry = this.queue.poll();
        ServerPlayNetworking.send(entry.player, entry.payload);
      }
    }

    if (Util.getMeasuringTimeMs() - this.lastSend > this.sendCooldown) {
      Entry entry = this.queue.poll();
      if (entry != null) {
        this.lastSend = Util.getMeasuringTimeMs();
        ServerPlayNetworking.send(entry.player, entry.payload);
      }
    }
  }

  public void add(ServerPlayerEntity player, CustomPayload payload) {
    if (!this.isThrottled) {
      ServerPlayNetworking.send(player, payload);
      return;
    }
    this.queue.add(new Entry(player, payload));
  }

  private void updateThrottleSettings() {
    this.isThrottled = CustomPaintingsPerWorldConfig.getInstance().throttleImageDownloads.getValue();
    if (this.isThrottled) {
      this.sendCooldown = MathHelper.floor(
          1000f / CustomPaintingsPerWorldConfig.getInstance().maxImagePacketsPerSecond.getValue());
    } else {
      this.sendCooldown = 0;
    }
  }

  private record Entry(ServerPlayerEntity player, CustomPayload payload) {
  }
}
