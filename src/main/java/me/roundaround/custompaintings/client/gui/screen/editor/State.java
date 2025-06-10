package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData.Painting;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.observable.Computed;
import me.roundaround.custompaintings.roundalib.observable.Observable;
import me.roundaround.custompaintings.roundalib.observable.Subject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class State implements AutoCloseable {
  private final List<Observable<?>> observables = new ArrayList<>();

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
  private final HashMap<Identifier, NativeImage> nativeImages = new HashMap<>();

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

    this.images.subscribe((images) -> {
      TextureManager manager = MinecraftClient.getInstance().getTextureManager();

      HashSet<Identifier> keys = new HashSet<>();
      images.forEach((image) -> {
        if (image == null || image.isEmpty()) {
          return;
        }

        Identifier key = getImageTextureId(image);
        keys.add(key);
        if (this.nativeImages.containsKey(key)) {
          return;
        }

        NativeImage nativeImage = image.toNativeImage();
        this.nativeImages.put(key, nativeImage);
        manager.registerTexture(key, new NativeImageBackedTexture(() -> image.hash(), nativeImage));
      });

      Set.copyOf(this.nativeImages.entrySet()).forEach((entry) -> {
        if (!keys.contains(entry.getKey())) {
          manager.destroyTexture(entry.getKey());
          entry.getValue().close();
          this.nativeImages.remove(entry.getKey());
        }
      });
    });
  }

  @Override
  public void close() {
    this.observables.forEach(Observable::close);
    this.observables.clear();

    this.nativeImages.forEach((key, image) -> {
      MinecraftClient.getInstance().getTextureManager().destroyTexture(key);
      image.close();
    });
    this.nativeImages.clear();
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

  public void setImage(int paintingIndex, Image image) {
    List<PackData.Painting> paintings = this.paintings.get();
    if (paintingIndex < 0 || paintingIndex >= paintings.size()) {
      return;
    }

    paintings.set(paintingIndex, paintings.get(paintingIndex).withImage(image));
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
    if (image == null || image.isEmpty()) {
      return MissingSprite.getMissingSpriteId();
    }
    String path = image.width() + "_" + image.height() + "_" + image.hash();
    return Identifier.of(Constants.MOD_ID, path);
  }
}
