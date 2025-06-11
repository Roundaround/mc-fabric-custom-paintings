package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
import me.roundaround.custompaintings.client.gui.screen.editor.PackData.Painting;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.observable.Computed;
import me.roundaround.custompaintings.roundalib.observable.Observable;
import me.roundaround.custompaintings.roundalib.observable.Subject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class State implements AutoCloseable {
  private final ArrayList<Observable<?>> observables = new ArrayList<>();

  public final Subject<UUID> uuid = this.addObservable(
      Subject.of(null));
  public final Subject<String> id = this.addObservable(
      Subject.of(""));
  public final Subject<String> name = this.addObservable(
      Subject.of(""));
  public final Subject<String> description = this.addObservable(
      Subject.of(""));
  public final Subject<Image> icon = this.addObservable(
      Subject.of(null));
  public final Subject<List<Painting>> paintings = this.addObservable(
      Subject.of(List.of()));

  public final Observable<Boolean> idDirty;
  public final Observable<Boolean> nameDirty;
  public final Observable<Boolean> descriptionDirty;
  public final Observable<Boolean> iconDirty;
  public final Observable<Boolean> paintingsDirty;
  public final Observable<Boolean> dirty;

  private final Subject<PackData> lastSaved;
  private final Observable<List<Image>> images;

  public State(@NotNull PackData pack) {
    this(pack, pack);
  }

  public State(@NotNull PackData pack, @NotNull PackData lastSavedPack) {
    this.uuid.set(pack.uuid());
    this.id.set(pack.id());
    this.name.set(pack.name());
    this.icon.set(pack.icon());
    this.description.set(pack.description());
    this.paintings.set(pack.paintings());

    this.lastSaved = this.addObservable(
        Subject.of(lastSavedPack));

    this.idDirty = this.addObservable(
        Computed.of(this.id, this.lastSaved,
            (id, lastSaved) -> !Objects.equals(id, lastSaved.id())));
    this.nameDirty = this.addObservable(
        Computed.of(this.name, this.lastSaved,
            (name, lastSaved) -> !Objects.equals(name, lastSaved.name())));
    this.descriptionDirty = this.addObservable(
        Computed.of(this.description, this.lastSaved,
            (description, lastSaved) -> !Objects.equals(description, lastSaved.description())));
    this.iconDirty = this.addObservable(
        Computed.of(this.icon, this.lastSaved,
            (icon, lastSaved) -> !Objects.equals(icon, lastSaved.icon())));
    this.paintingsDirty = this.addObservable(
        Computed.of(this.paintings, this.lastSaved,
            (paintings, lastSaved) -> !paintings.equals(lastSaved.paintings())));

    this.dirty = this.addObservable(
        Computed.of(
            this.idDirty,
            this.nameDirty,
            this.descriptionDirty,
            this.iconDirty,
            this.paintingsDirty,
            (idDirty, nameDirty, descriptionDirty, iconDirty, paintingsDirty) -> idDirty
                || nameDirty
                || descriptionDirty
                || iconDirty
                || paintingsDirty));

    this.images = this.addObservable(
        Computed.of(this.icon, this.paintings,
            (icon, paintings) -> {
              List<Image> images = new ArrayList<>();
              if (icon != null) {
                images.add(icon);
              }
              images.addAll(paintings.stream().map(Painting::image).toList());
              return images;
            }));

    this.images.pairwise().subscribe((t) -> {
      HashSet<Identifier> used = new HashSet<>();

      List<Image> prevImages = t.t1();
      List<Image> currImages = t.t2();

      currImages.forEach((image) -> {
        used.add(TextureManager.registerTexture(image));
      });

      prevImages.forEach((image) -> {
        Identifier key = TextureManager.getImageTextureId(image);
        if (!used.contains(key)) {
          TextureManager.disposeTexture(key);
        }
      });
    });
  }

  @Override
  public void close() {
    this.observables.forEach(Observable::close);
    this.observables.clear();

    this.images.get().forEach((image) -> {
      TextureManager.disposeTexture(image);
    });
  }

  public State clone() {
    return new State(this.getPack(), this.getLastSaved());
  }

  public PackData getPack() {
    return new PackData(
        this.uuid.get(),
        this.id.get(),
        this.name.get(),
        this.description.get(),
        this.icon.get(),
        List.copyOf(this.paintings.get()));
  }

  public PackData getLastSaved() {
    return this.lastSaved.get();
  }

  public void setPainting(int paintingIndex, PackData.Painting painting) {
    List<PackData.Painting> srcPaintings = this.paintings.get();
    if (paintingIndex < 0 || paintingIndex >= srcPaintings.size()) {
      return;
    }

    List<PackData.Painting> paintings = new ArrayList<>(srcPaintings);
    paintings.set(paintingIndex, painting);
    this.paintings.set(paintings);
  }

  public void movePaintingUp(int paintingIndex) {
    List<PackData.Painting> srcPaintings = this.paintings.get();
    if (paintingIndex <= 0 || paintingIndex >= srcPaintings.size()) {
      return;
    }

    List<PackData.Painting> paintings = new ArrayList<>(srcPaintings);

    PackData.Painting painting = paintings.get(paintingIndex);
    PackData.Painting previousPainting = paintings.get(paintingIndex - 1);

    paintings.set(paintingIndex, previousPainting);
    paintings.set(paintingIndex - 1, painting);

    this.paintings.set(paintings);
  }

  public void movePaintingDown(int paintingIndex) {
    List<PackData.Painting> srcPaintings = this.paintings.get();
    if (paintingIndex < 0 || paintingIndex >= srcPaintings.size() - 1) {
      return;
    }

    List<PackData.Painting> paintings = new ArrayList<>(srcPaintings);

    PackData.Painting painting = paintings.get(paintingIndex);
    PackData.Painting nextPainting = paintings.get(paintingIndex + 1);

    paintings.set(paintingIndex, nextPainting);
    paintings.set(paintingIndex + 1, painting);

    this.paintings.set(paintings);
  }

  private <O extends Observable<T>, T> O addObservable(O observable) {
    this.observables.add(observable);
    return observable;
  }

  public static Identifier getImageTextureId(Image image) {
    return TextureManager.getImageTextureId(image);
  }

  private static class TextureManager {
    private static final Lock lock = new ReentrantLock();
    private static final HashMap<Identifier, Integer> refCounts = new HashMap<>();
    private static final HashMap<Identifier, NativeImageBackedTexture> textures = new HashMap<>();

    public static Identifier getImageTextureId(Image image) {
      if (image == null || image.isEmpty()) {
        return MissingSprite.getMissingSpriteId();
      }
      String path = image.width() + "_" + image.height() + "_" + image.hash();
      return Identifier.of(Constants.MOD_ID, path);
    }

    public static Identifier registerTexture(Image image) {
      if (image == null || image.isEmpty()) {
        return MissingSprite.getMissingSpriteId();
      }

      Identifier key = getImageTextureId(image);
      lock.lock();
      try {
        int refCount = refCounts.merge(key, 1, Integer::sum);
        if (refCount == 1) {
          NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> image.hash(), image.toNativeImage());
          textures.put(key, texture);
          MinecraftClient.getInstance().getTextureManager().registerTexture(key, texture);
        }
      } finally {
        lock.unlock();
      }
      return key;
    }

    public static void disposeTexture(Image image) {
      if (image == null || image.isEmpty()) {
        return;
      }
      disposeTexture(getImageTextureId(image));
    }

    public static void disposeTexture(Identifier key) {
      lock.lock();
      try {
        int refCount = refCounts.merge(key, -1, Integer::sum);
        if (refCount <= 0) {
          MinecraftClient.getInstance().getTextureManager().destroyTexture(key);
          textures.remove(key).close();
        }
      } finally {
        lock.unlock();
      }
    }
  }
}
