package me.roundaround.custompaintings.registry;

import com.google.common.collect.ImmutableMap;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.TriConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ImageStore {
  private final HashMap<CustomId, StoredImage> store = new HashMap<>();

  public ImageStore() {}

  private ImageStore(Map<CustomId, StoredImage> store) {
    this.store.putAll(store);
  }

  public void clear() {
    this.store.clear();
  }

  public void putAll(Map<CustomId, Image> images, Map<CustomId, String> hashes) {
    images.forEach((id, image) -> {
      this.store.put(id, new StoredImage(image, hashes.get(id)));
    });
  }

  public void setAll(Map<CustomId, Image> images, Map<CustomId, String> hashes) {
    this.clear();
    this.putAll(images, hashes);
  }

  public StoredImage put(CustomId id, Image image) {
    return this.store.put(id, new StoredImage(image));
  }

  public StoredImage put(CustomId id, Image image, String hash) {
    return this.store.put(id, new StoredImage(image, hash));
  }

  public boolean contains(CustomId id) {
    return this.store.containsKey(id);
  }

  public Image getImage(CustomId id) {
    StoredImage stored = this.store.get(id);
    if (stored == null) {
      return null;
    }
    return stored.image();
  }

  public StoredImage get(CustomId id) {
    return this.store.get(id);
  }

  public void forEach(TriConsumer<CustomId, Image, String> consumer) {
    this.store.forEach((id, stored) -> {
      consumer.accept(id, stored.image(), stored.hash());
    });
  }

  @SuppressWarnings("UnusedReturnValue")
  public StoredImage remove(CustomId id) {
    return this.store.remove(id);
  }

  @SuppressWarnings("UnusedReturnValue")
  public boolean removeIf(Predicate<CustomId> predicate) {
    return this.store.keySet().removeIf(predicate);
  }

  public ImageStore copy() {
    return new ImageStore(this.store);
  }

  public ImmutableMap<CustomId, String> getHashes() {
    ImmutableMap.Builder<CustomId, String> builder = ImmutableMap.builder();
    this.forEach((id, image, hash) -> {
      if (hash != null) {
        builder.put(id, hash);
      }
    });
    return builder.build();
  }

  public record StoredImage(Image image, String hash) {
    public StoredImage(Image image) {
      this(image, null);
    }

    public StoredImage(Image image, String hash) {
      this.image = image;
      this.hash = hash == null ? image.getHash() : hash;
    }
  }
}
