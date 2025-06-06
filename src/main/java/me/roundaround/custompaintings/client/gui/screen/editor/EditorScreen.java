package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.Parent;
import me.roundaround.custompaintings.client.gui.screen.Screen;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.Axis;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.roundalib.util.Observable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class EditorScreen extends Screen {
  private static final int PREFERRED_WIDTH = 300;
  private static final Identifier TAB_HEADER_BACKGROUND_TEXTURE = Identifier
      .ofVanilla("textures/gui/tab_header_background.png");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final TabManager tabManager = new TabManager(
      (element) -> this.addDrawableChild(element),
      (child) -> this.remove(child));

  private TabNavigationWidget tabNavigation;
  private State state;

  public EditorScreen(
      @NotNull Parent parent,
      @NotNull MinecraftClient client,
      @NotNull PackData pack) {
    super(Text.translatable("custompaintings.editor.editor.title"), parent, client);
    this.state = new State(pack);
  }

  @Override
  protected void init() {
    this.tabNavigation = TabNavigationWidget.builder(this.tabManager, this.width)
        .tabs(new MetadataTab(), new PaintingsTab(), new MigrationsTab())
        .build();
    this.addDrawableChild(this.tabNavigation);

    ButtonWidget doneButton = this.layout.addFooter(ButtonWidget.builder(
        this.getDoneButtonMessage(this.state.dirty.get()),
        (b) -> this.close())
        .width(ButtonWidget.field_49479)
        .build());
    this.state.dirty.subscribe((dirty) -> doneButton.setMessage(this.getDoneButtonMessage(dirty)));

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild((child) -> {
      child.setNavigationOrder(1);
      this.addDrawableChild(child);
    });
    this.tabNavigation.selectTab(0, false);
    this.refreshWidgetPositions();
  }

  @Override
  protected void refreshWidgetPositions() {
    if (this.tabNavigation == null) {
      return;
    }

    this.tabNavigation.setWidth(this.width);
    this.tabNavigation.init();

    int headerFooterHeight = this.tabNavigation.getNavigationFocus().getBottom();
    ScreenRect tabArea = new ScreenRect(0, headerFooterHeight, this.width,
        this.height - this.layout.getFooterHeight() - headerFooterHeight);
    this.tabManager.setTabArea(tabArea);

    this.layout.setHeaderHeight(headerFooterHeight);
    this.layout.refreshPositions();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    super.render(context, mouseX, mouseY, deltaTicks);
    context.drawTexture(
        RenderLayer::getGuiTextured, Screen.FOOTER_SEPARATOR_TEXTURE, 0,
        this.height - this.layout.getFooterHeight() - 2, 0, 0, this.width, 2, 32, 2);
  }

  @Override
  protected void renderDarkening(DrawContext context) {
    context.drawTexture(
        RenderLayer::getGuiTextured,
        TAB_HEADER_BACKGROUND_TEXTURE,
        0, 0, 0, 0,
        this.width,
        this.layout.getHeaderHeight(),
        16, 16);
    this.renderDarkening(context, 0, this.layout.getHeaderHeight(), this.width, this.height);
  }

  @Override
  public void close() {
    this.state.close();
    super.close();
  }

  private Text getDoneButtonMessage(boolean dirty) {
    MutableText text = ScreenTexts.DONE.copy();
    if (dirty) {
      text.append(" *");
    }
    return text;
  }

  abstract class PackEditorTab implements Tab {
    protected final Text title;
    protected final LinearLayoutWidget layout = new LinearLayoutWidget(Axis.VERTICAL)
        .mainAxisContentAlignStart()
        .defaultOffAxisContentAlignCenter()
        .spacing(0);

    protected PackEditorTab(Text title) {
      this.title = title;
    }

    @Override
    public Text getTitle() {
      return this.title;
    }

    @Override
    public void forEachChild(Consumer<ClickableWidget> consumer) {
      this.layout.forEachChild(consumer);
    }

    @Override
    public void refreshGrid(ScreenRect tabArea) {
      this.layout.setPositionAndDimensions(
          tabArea.getLeft(),
          tabArea.getTop(),
          tabArea.width(),
          tabArea.height());
      this.layout.refreshPositions();
    }

    protected TextRenderer textRenderer() {
      return EditorScreen.this.client.textRenderer;
    }

    protected State state() {
      return EditorScreen.this.state;
    }

    protected int getContentWidth() {
      return Math.min(PREFERRED_WIDTH, this.layout.getWidth() - 2 * GuiUtil.PADDING);
    }
  }

  class MetadataTab extends PackEditorTab {
    private final TextFieldWidget idField;

    public MetadataTab() {
      super(Text.translatable("custompaintings.editor.editor.tab.metadata.title"));

      MetadataList list = this.layout.add(new MetadataList(
          EditorScreen.this.client,
          this.layout),
          (parent, self) -> {
            self.setDimensionsAndPosition(
                parent.getWidth(),
                parent.getHeight(),
                parent.getX(),
                parent.getY());
          });

      MetadataList.TextFieldEntry idEntry = list.addEntry(MetadataList.TextFieldEntry.factory(
          this.textRenderer(),
          "id",
          this.state().id,
          this.state().idDirty,
          () -> this.state().getLastSaved().id()));
      this.idField = idEntry.getField();

      list.addEntry(MetadataList.TextFieldEntry.factory(
          this.textRenderer(),
          "name",
          this.state().name,
          this.state().nameDirty,
          () -> this.state().getLastSaved().name()));
      list.addEntry(MetadataList.TextFieldEntry.factory(
          this.textRenderer(),
          "description",
          this.state().description,
          this.state().descriptionDirty,
          () -> this.state().getLastSaved().description(),
          255));

      this.layout.refreshPositions();

      EditorScreen.this.setInitialFocus(this.idField);
    }
  }

  static class MetadataList extends ParentElementEntryListWidget<MetadataList.Entry> {
    public MetadataList(MinecraftClient client, LinearLayoutWidget layout) {
      super(client, layout.getX(), layout.getY(), layout.getWidth(), layout.getHeight());
      this.setContentPadding(2 * GuiUtil.PADDING);
    }

    @Override
    protected void renderListBackground(DrawContext context) {
      // Disable background
    }

    @Override
    protected void renderListBorders(DrawContext context) {
      // Disable borders
    }

    @Override
    protected int getPreferredContentWidth() {
      return VANILLA_LIST_WIDTH_L;
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
          Observable<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
          int maxLength) {
        super(textRenderer, index, x, y, width, HEIGHT);

        Text label = Text.translatable("custompaintings.editor.editor.tab.metadata." + id);

        LinearLayoutWidget layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter();

        layout.add(LabelWidget.builder(this.textRenderer, label)
            .alignTextLeft()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
              self.setDimensions(this.getLabelWidth(parent), this.getContentHeight());
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
        valueObservable.subscribe((value) -> {
          String text = this.field.getText();
          if (!text.equals(value)) {
            this.field.setText(value);
            this.field.setCursorToEnd(false);
            this.field.setSelectionStart(0);
            this.field.setSelectionEnd(0);
          }
        });

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
        dirtyObservable.subscribe((dirty) -> resetButton.active = dirty);

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

      private int getLabelWidth(LinearLayoutWidget layout) {
        return layout.getWidth()
            - 2 * layout.getSpacing()
            - this.getControlWidth(layout)
            - IconButtonWidget.SIZE_V;
      }

      private int getControlWidth(LinearLayoutWidget layout) {
        return Math.max(CONTROL_MIN_WIDTH, Math.round(layout.getWidth() * 0.6f));
      }

      public static FlowListWidget.EntryFactory<TextFieldEntry> factory(
          TextRenderer textRenderer,
          String id,
          Observable<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved) {
        return factory(textRenderer, id, valueObservable, dirtyObservable, getLastSaved, 32);
      }

      public static FlowListWidget.EntryFactory<TextFieldEntry> factory(
          TextRenderer textRenderer,
          String id,
          Observable<String> valueObservable,
          Observable<Boolean> dirtyObservable,
          Supplier<String> getLastSaved,
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
            maxLength);
      }
    }
  }

  class PaintingsTab extends PackEditorTab {
    public PaintingsTab() {
      super(Text.translatable("custompaintings.editor.editor.tab.paintings.title"));

      this.layout.add(
          LabelWidget.builder(this.textRenderer(), Text.of("Paintings"))
              .hideBackground()
              .showShadow()
              .build(),
          (parent, self) -> self.setWidth(this.getContentWidth()));
    }
  }

  class MigrationsTab extends PackEditorTab {
    public MigrationsTab() {
      super(Text.translatable("custompaintings.editor.editor.tab.migrations.title"));

      this.layout.add(
          LabelWidget.builder(this.textRenderer(), Text.of("Migrations"))
              .hideBackground()
              .showShadow()
              .build(),
          (parent, self) -> self.setWidth(this.getContentWidth()));
    }
  }
}
