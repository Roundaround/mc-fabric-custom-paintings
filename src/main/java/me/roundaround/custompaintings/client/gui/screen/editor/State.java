package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.roundalib.util.Observable;

public class State implements AutoCloseable {
  public final Observable<UUID> uuid = Observable.of(null);
  public final Observable<String> id = Observable.of("");
  public final Observable<String> name = Observable.of("");
  public final Observable<String> description = Observable.of("");
  public final Observable<Boolean> dirty = Observable.of(false);

  private final Observable<PackData> current = Observable.computed(
      this.uuid, this.id, this.name, this.description,
      (uuid, id, name, description) -> new PackData(uuid, id, name, description));

  private @NotNull PackData lastSaved;

  public State(@NotNull PackData pack) {
    this.uuid.set(pack.getUuid());
    this.id.set(pack.getId());
    this.name.set(pack.getName());
    this.description.set(pack.getDescription());
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
  }

  @Override
  public void close() {
    this.uuid.clear();
    this.id.clear();
    this.name.clear();
    this.description.clear();
    this.current.clear();
  }

  private boolean calculateDirty() {
    return !(new PackData(this.uuid.get(), this.id.get(), this.name.get(), this.description.get())
        .equals(this.lastSaved));
  }
}
