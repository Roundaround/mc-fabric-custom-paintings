package me.roundaround.custompaintings.client.registry;

import me.roundaround.custompaintings.resource.file.Image;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ImageChunkBuilder {
  private int width;
  private int height;
  private int totalChunks;
  private final HashMap<Integer, byte[]> chunks = new HashMap<>();

  public ImageChunkBuilder() {
    this.width = 0;
    this.height = 0;
    this.totalChunks = -1;
  }

  public boolean set(int width, int height, int totalChunks) {
    this.width = width;
    this.height = height;
    this.totalChunks = totalChunks;
    // TODO: Also check that each index is accounted for.
    return this.chunks.size() == this.totalChunks;
  }

  public boolean set(int index, byte[] chunk) {
    this.chunks.put(index, chunk);
    // TODO: Also check that each index is accounted for.
    return this.chunks.size() == this.totalChunks;
  }

  public Image generate() {
    // TODO: Error handling - if we never received the header (this.totalChunks == -1),
    //   request the image again or something.

    int length = this.chunks.values().stream().mapToInt((chunk) -> chunk.length).sum();
    int pointer = 0;
    byte[] bytes = new byte[length];
    ArrayDeque<byte[]> sortedChunks = this.chunks.entrySet()
        .stream()
        .sorted(Comparator.comparingInt(Map.Entry::getKey))
        .map(Map.Entry::getValue)
        .collect(Collectors.toCollection(ArrayDeque::new));

    for (byte[] chunk : sortedChunks) {
      System.arraycopy(chunk, 0, bytes, pointer, chunk.length);
      pointer += chunk.length;
    }

    return Image.fromBytes(bytes, this.width, this.height);
  }
}
