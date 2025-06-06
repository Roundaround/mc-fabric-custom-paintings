package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.util.Observable;

public class State implements AutoCloseable {
  public final Observable<UUID> uuid = Observable.of(null);
  public final Observable<String> id = Observable.of("");
  public final Observable<String> name = Observable.of("");
  public final Observable<String> description = Observable.of("");
  public final Observable<Image> icon = Observable.of(null);
  public final Observable<String> iconHash = Observable.computed(this.icon,
      (icon) -> icon == null ? "" : icon.getHash());
  public final Observable<List<PackData.Painting>> paintings = Observable.of(List.of());
  public final Observable<Boolean> dirty = Observable.of(false);

  private @NotNull PackData lastSaved;

  public State(@NotNull PackData pack) {
    this.uuid.set(pack.getUuid());
    this.id.set(pack.getId());
    this.name.set(pack.getName());
    this.icon.set(pack.getIcon());
    this.description.set(pack.getDescription());
    this.paintings.set(pack.getPaintings());
    this.lastSaved = pack;

    this.id.subscribe((id) -> {
      this.dirty.set(this.dirty.get()
          ? this.calculateDirty()
          : !id.equals(this.lastSaved.getId()));
    }, Observable.SubscribeOptions.notEmittingImmediately());

    this.name.subscribe((name) -> {
      this.dirty.set(this.dirty.get()
          ? this.calculateDirty()
          : !name.equals(this.lastSaved.getName()));
    }, Observable.SubscribeOptions.notEmittingImmediately());

    this.description.subscribe((description) -> {
      this.dirty.set(this.dirty.get()
          ? this.calculateDirty()
          : !description.equals(this.lastSaved.getDescription()));
    }, Observable.SubscribeOptions.notEmittingImmediately());

    this.iconHash.subscribe((iconHash) -> {
      this.dirty.set(this.dirty.get()
          ? this.calculateDirty()
          : !iconHash.equals(this.lastSaved.getIconHash()));
    }, Observable.SubscribeOptions.notEmittingImmediately());

    this.icon.subscribe((icon) -> {
      this.dirty.set(this.dirty.get()
          ? this.calculateDirty()
          : !PackData.imagesEqual(icon, this.lastSaved.getIcon()));
    }, Observable.SubscribeOptions.notEmittingImmediately());

    this.paintings.subscribe((paintings) -> {
      this.dirty.set(this.dirty.get()
          ? this.calculateDirty()
          : !paintings.equals(this.lastSaved.getPaintings()));
    }, Observable.SubscribeOptions.notEmittingImmediately());
  }

  @Override
  public void close() {
    this.uuid.clear();
    this.id.clear();
    this.name.clear();
    this.description.clear();
    this.icon.clear();
    this.iconHash.clear();
    this.paintings.clear();
  }

  private boolean calculateDirty() {
    return !this.id.get().equals(this.lastSaved.getId())
        || !this.name.get().equals(this.lastSaved.getName())
        || !this.description.get().equals(this.lastSaved.getDescription())
        || !this.iconHash.get().equals(this.lastSaved.getIconHash())
        || !PackData.imagesEqual(this.icon.get(), this.lastSaved.getIcon())
        || !this.paintings.get().equals(this.lastSaved.getPaintings());
  }
}
