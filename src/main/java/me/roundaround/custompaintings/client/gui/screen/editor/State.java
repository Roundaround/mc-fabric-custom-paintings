package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.util.Observable;

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
  public final Observable<String> iconHash = this.addObservable(
      Observable.computed(this.icon, (icon) -> icon == null ? "" : icon.getHash()));
  public final Observable<List<PackData.Painting>> paintings = this.addObservable(
      Observable.of(List.of()));

  private final Observable<PackData> lastSaved;

  public final Observable<Boolean> idDirty;
  public final Observable<Boolean> nameDirty;
  public final Observable<Boolean> descriptionDirty;
  public final Observable<Boolean> iconDirty;
  public final Observable<Boolean> paintingsDirty;

  public final Observable<Boolean> dirty;

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
            (id, lastSaved) -> !id.equals(lastSaved.id())));
    this.nameDirty = this.addObservable(
        Observable.computed(this.name, this.lastSaved,
            (name, lastSaved) -> !name.equals(lastSaved.name())));
    this.descriptionDirty = this.addObservable(
        Observable.computed(this.description,
            this.lastSaved,
            (description, lastSaved) -> !description.equals(lastSaved.description())));
    this.iconDirty = this.addObservable(
        Observable.computed(this.iconHash, this.icon, this.lastSaved,
            (iconHash, icon, lastSaved) -> !iconHash.equals(lastSaved.iconHash())
                || !PackData.imagesEqual(icon, lastSaved.icon())));
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
  }

  @Override
  public void close() {
    for (Observable<?> observable : this.observables) {
      observable.clear();
    }
    this.observables.clear();
  }

  public PackData getLastSaved() {
    return this.lastSaved.get();
  }

  private <T> Observable<T> addObservable(Observable<T> observable) {
    this.observables.add(observable);
    return observable;
  }
}
