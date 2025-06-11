package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.observable.Observable;
import me.roundaround.custompaintings.roundalib.observable.Subject;
import net.minecraft.util.Identifier;

public class State implements AutoCloseable {
  public static final Identifier TEXTURE_ID = Identifier.of(Constants.MOD_ID, "image_editor");

  private final ArrayList<Observable<?>> observables = new ArrayList<>();
  private final Image originalImage;

  public final Subject<String> id = this.addObservable(
      Subject.of(""));
  public final Subject<String> name = this.addObservable(
      Subject.of(""));
  public final Subject<String> artist = this.addObservable(
      Subject.of(""));
  public final Subject<Integer> blockWidth = this.addObservable(
      Subject.of(16));
  public final Subject<Integer> blockHeight = this.addObservable(
      Subject.of(16));
  public final Subject<Image> image = this.addObservable(
      Subject.of(null));
  public final Subject<List<Image.Operation>> operations = this.addObservable(
      Subject.of(List.of()));

  public State(@NotNull PackData.Painting painting) {
    this.originalImage = painting.image();
    this.id.set(painting.id());
    this.name.set(painting.name());
    this.artist.set(painting.artist());
    this.blockWidth.set(painting.blockWidth());
    this.blockHeight.set(painting.blockHeight());

    this.operations.subscribe((operations) -> {
      this.image.set(this.originalImage.apply(operations));
    });
  }

  @Override
  public void close() {
    this.observables.forEach(Observable::close);
    this.observables.clear();
  }

  public PackData.Painting getPainting() {
    return new PackData.Painting(
        this.id.get(),
        this.name.get(),
        this.artist.get(),
        this.blockWidth.get(),
        this.blockHeight.get(),
        this.image.get());
  }

  public void addOperation(Image.Operation operation) {
    List<Image.Operation> srcOperations = this.operations.get();
    ArrayList<Image.Operation> operations = new ArrayList<>(srcOperations);
    operations.add(operation);
    this.operations.set(operations);
  }

  public void removeOperation(int index) {
    List<Image.Operation> srcOperations = this.operations.get();
    if (index < 0 || index >= srcOperations.size()) {
      return;
    }

    ArrayList<Image.Operation> operations = new ArrayList<>(srcOperations);
    operations.remove(index);
    this.operations.set(operations);
  }

  private <O extends Observable<T>, T> O addObservable(O observable) {
    this.observables.add(observable);
    return observable;
  }
}
