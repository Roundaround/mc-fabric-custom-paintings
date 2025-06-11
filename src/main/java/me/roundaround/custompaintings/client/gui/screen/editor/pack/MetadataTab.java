package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.roundalib.observable.Observable;
import me.roundaround.custompaintings.roundalib.observable.Subject;
import me.roundaround.custompaintings.roundalib.observable.Subscription;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class MetadataTab extends PackTab {
  private final int MIN_WIDTH = 240;

  public MetadataTab(
      @NotNull MinecraftClient client,
      @NotNull State state,
      @NotNull PackScreen screen) {
    super(client,
        state,
        screen,
        Text.translatable("custompaintings.editor.editor.metadata.title"));

    MetadataList list = this.layout.add(new MetadataList(
        this.client,
        this.getWidth(this.layout),
        this.layout.getHeight()),
        (parent, self) -> {
          self.setDimensionsAndPosition(
              this.getWidth(parent),
              parent.getHeight(),
              parent.getX(),
              parent.getY());
        });

    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.textRenderer,
        "id",
        this.state.id,
        this.state.idDirty,
        () -> this.state.getLastSaved().id(),
        this.subscriptions::add));
    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.textRenderer,
        "name",
        this.state.name,
        this.state.nameDirty,
        () -> this.state.getLastSaved().name(),
        this.subscriptions::add));
    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.textRenderer,
        "description",
        this.state.description,
        this.state.descriptionDirty,
        () -> this.state.getLastSaved().description(),
        this.subscriptions::add,
        255));

    this.layout.refreshPositions();
  }

  private int getWidth(LinearLayoutWidget layout) {
    return Math.max(MIN_WIDTH, Math.round(layout.getWidth() * 0.8f));
  }

  static class MetadataList extends ParentElementEntryListWidget<MetadataList.Entry> {
    public MetadataList(MinecraftClient client, int width, int height) {
      super(client, 0, 0, width, height);
      this.setContentPadding(GuiUtil.PADDING);
    }

    @Override
    protected void renderListBackground(DrawContext context) {
      // Disable background
    }

    @Override
    protected void renderListBorders(DrawContext context) {
      // Disable borders
    }

    static class Entry extends ParentElementEntryListWidget.Entry {
      protected static final int HEIGHT = 20;
      protected static final int CONTROL_MIN_WIDTH = 140;

      protected final TextRenderer textRenderer;

      public Entry(TextRenderer textRenderer, int index, int x, int y, int width, int contentHeight) {
        super(index, x, y, width, contentHeight);
        this.textRenderer = textRenderer;
      }
    }

    static class TextFieldEntry extends Entry {
      private final TextFieldWidget field;

      public TextFieldEntry(
          TextRenderer textRenderer,
          int index,
          int x,
          int y,
          int width,
          String id,
          Subject<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
          Consumer<Subscription> addSubscription,
          int maxLength) {
        super(textRenderer, index, x, y, width, HEIGHT);

        Text label = Text.translatable("custompaintings.editor.editor.metadata." + id);

        LinearLayoutWidget layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter();

        layout.add(LabelWidget.builder(this.textRenderer, label)
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
              self.setDimensions(parent.getUnusedSpace(self), this.getContentHeight());
            });

        this.field = layout.add(
            new TextFieldWidget(
                this.textRenderer,
                this.getControlWidth(layout),
                HEIGHT,
                label),
            (parent, self) -> {
              self.setWidth(this.getControlWidth(parent));
            });
        this.field.setMaxLength(maxLength);
        this.field.setText(valueObservable.get());

        // TODO: If the initial value is too long show a warning tooltip

        this.field.setChangedListener(valueObservable::set);
        addSubscription.accept(valueObservable.subscribe((value) -> {
          String text = this.field.getText();
          if (!text.equals(value)) {
            this.field.setText(value);
            this.field.setCursorToEnd(false);
            this.field.setSelectionStart(0);
            this.field.setSelectionEnd(0);
          }
        }));

        IconButtonWidget resetButton = layout.add(IconButtonWidget.builder(BuiltinIcon.UNDO_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.translatable("custompaintings.editor.editor.revert"))
            .onPress((button) -> {
              String value = getLastSaved.get();
              if (value.length() > maxLength) {
                value = value.substring(0, maxLength);
              }
              this.field.setText(value);
            })
            .build());
        addSubscription.accept(dirtyObservable.subscribe((dirty) -> resetButton.active = dirty));

        this.addLayout(layout, (self) -> {
          self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight());
        });
        layout.forEachChild(this::addDrawableChild);
      }

      public TextFieldWidget getField() {
        return this.field;
      }

      private int getControlWidth(LinearLayoutWidget layout) {
        return Math.max(CONTROL_MIN_WIDTH, Math.round(layout.getWidth() * 0.6f));
      }

      public static FlowListWidget.EntryFactory<TextFieldEntry> factory(
          TextRenderer textRenderer,
          String id,
          Subject<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
          Consumer<Subscription> addSubscription) {
        return factory(textRenderer, id, valueObservable, dirtyObservable, getLastSaved, addSubscription, 32);
      }

      public static FlowListWidget.EntryFactory<TextFieldEntry> factory(
          TextRenderer textRenderer,
          String id,
          Subject<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
          Consumer<Subscription> addSubscription,
          int maxLength) {
        return (index, left, top, width) -> new TextFieldEntry(
            textRenderer,
            index,
            left,
            top,
            width,
            id,
            valueObservable,
            dirtyObservable,
            getLastSaved,
            addSubscription,
            maxLength);
      }
    }
  }
}
