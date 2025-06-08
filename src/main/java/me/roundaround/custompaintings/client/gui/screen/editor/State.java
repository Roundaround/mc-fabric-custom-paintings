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
import me.roundaround.custompaintings.roundalib.util.Observable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public class State implements AutoCloseable {
  private final List<Observable<?>> observables = new ArrayList<>();

  public final Observable<UUID> uuid = this.addObservable(
      Observable.of(null));
  public final Observable<String> id = this.addObservable(
      Observable.of(""));
  public final Observable<String> name = this.addObservable(
      Observable.of(""));
  public final Observable<String> description = this.addObservable(
      Observable.of(""));
  public final Observable<Image> icon = this.addObservable(
      Observable.of(null));
  public final Observable<List<Painting>> paintings = this.addObservable(
      Observable.of(List.of()));

  public final Observable<Boolean> idDirty;
  public final Observable<Boolean> nameDirty;
  public final Observable<Boolean> descriptionDirty;
  public final Observable<Boolean> iconDirty;
  public final Observable<Boolean> paintingsDirty;
  public final Observable<Boolean> dirty;

  private final Observable<PackData> lastSaved;
  private final Observable<List<Image>> images;
  private final HashMap<Identifier, NativeImage> nativeImages = new HashMap<>();

  public State(@NotNull PackData pack) {
    this.uuid.set(pack.uuid());
    this.id.set(pack.id());
    this.name.set(pack.name());
    this.icon.set(pack.icon());
    this.description.set(pack.description());
    this.paintings.set(pack.paintings());

    this.lastSaved = this.addObservable(
        Observable.of(pack));

    this.idDirty = this.addObservable(
        Observable.computed(this.id, this.lastSaved,
            (id, lastSaved) -> !Objects.equals(id, lastSaved.id())));
    this.nameDirty = this.addObservable(
        Observable.computed(this.name, this.lastSaved,
            (name, lastSaved) -> !Objects.equals(name, lastSaved.name())));
    this.descriptionDirty = this.addObservable(
        Observable.computed(this.description,
            this.lastSaved,
            (description, lastSaved) -> !Objects.equals(description, lastSaved.description())));
    this.iconDirty = this.addObservable(
        Observable.computed(this.icon, this.lastSaved,
            (icon, lastSaved) -> !Objects.equals(icon, lastSaved.icon())));
    this.paintingsDirty = this.addObservable(
        Observable.computed(this.paintings, this.lastSaved,
            (paintings, lastSaved) -> !paintings.equals(lastSaved.paintings())));

    this.dirty = this.addObservable(
        Observable.computed(
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
        Observable.computed(this.icon, this.paintings,
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
    for (Observable<?> observable : this.observables) {
      observable.clear();
    }
    this.observables.clear();

    this.nativeImages.forEach((key, image) -> {
      MinecraftClient.getInstance().getTextureManager().destroyTexture(key);
      image.close();
    });
    this.nativeImages.clear();
  }

  public PackData getLastSaved() {
    return this.lastSaved.get();
  }

  private <T> Observable<T> addObservable(Observable<T> observable) {
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
