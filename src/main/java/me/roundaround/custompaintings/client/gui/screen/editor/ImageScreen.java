package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.widget.ImageDisplayWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.custompaintings.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import me.roundaround.custompaintings.roundalib.client.gui.util.Axis;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tab.Tab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ImageScreen extends BaseScreen {
  private static final int PANEL_MIN_WIDTH = 140;
  private static final Identifier TEXTURE_ID = Identifier.of(Constants.MOD_ID, "image_editor");
  private static final Identifier TAB_HEADER_BACKGROUND_TEXTURE = Identifier
      .ofVanilla("textures/gui/tab_header_background.png");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final TabManager tabManager = new TabManager(
      (element) -> this.addDrawableChild(element),
      (child) -> this.remove(child));
  private final ArrayList<Image.Operation> operations = new ArrayList<>();
  private final Consumer<Image> saveCallback;
  private final NativeImageBackedTexture texture;

  private Image image;
  private TabNavigationWidget tabNavigation;
  private InfoTab infoTab;
  private OperationsTab operationsTab;
  private FillerWidget tabAreaPlaceholder;
  private ImageDisplayWidget imageDisplay;

  public ImageScreen(
      @NotNull Text title,
      @NotNull ScreenParent parent,
      @NotNull MinecraftClient client,
      Image image,
      Consumer<Image> saveCallback) {
    super(title, parent, client);
    this.saveCallback = saveCallback;
    this.image = image;

    this.texture = new NativeImageBackedTexture(() -> "Image Editor", this.getNativeImage());
    this.client.getTextureManager().registerTexture(TEXTURE_ID, texture);
  }

  @Override
  public void init() {
    this.infoTab = new InfoTab(this.client, this.image, this::setImage);
    this.operationsTab = new OperationsTab(this.client, this.operations, this.image, this::setImage);
    this.tabNavigation = TabNavigationWidget.builder(this.tabManager, this.width)
        .tabs(this.infoTab, this.operationsTab)
        .build();
    this.addDrawableChild(this.tabNavigation);

    this.layout.getBody()
        .flowAxis(Axis.HORIZONTAL)
        .spacing(GuiUtil.PADDING)
        .padding(GuiUtil.PADDING);

    this.tabAreaPlaceholder = this.layout.addBody(FillerWidget.empty(), (parent, self) -> {
      self.setDimensions(this.getPanelWidth(parent), parent.getInnerHeight());
    });

    this.imageDisplay = this.layout.addBody(
        new ImageDisplayWidget((image) -> TEXTURE_ID, this.image),
        (parent, self) -> {
          self.setDimensions(parent.getUnusedSpace(self), parent.getInnerHeight());
        });

    this.layout.addFooter(ButtonWidget.builder(
        ScreenTexts.DONE,
        (b) -> {
          this.saveCallback.accept(this.image);
          this.close();
        })
        .build());
    this.layout.addFooter(ButtonWidget.builder(
        ScreenTexts.CANCEL,
        (b) -> this.close())
        .build());

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
    this.layout.setHeaderHeight(headerFooterHeight);

    this.layout.refreshPositions();

    ScreenRect tabArea = new ScreenRect(
        this.tabAreaPlaceholder.getX(),
        this.tabAreaPlaceholder.getY(),
        this.tabAreaPlaceholder.getWidth(),
        this.tabAreaPlaceholder.getHeight());
    this.tabManager.setTabArea(tabArea);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    super.render(context, mouseX, mouseY, deltaTicks);
    context.drawTexture(
        RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0,
        this.height - this.layout.getFooterHeight(), 0, 0, this.width, 2, 32, 2);
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
  public void removed() {
    this.client.getTextureManager().destroyTexture(TEXTURE_ID);
  }

  private int getPanelWidth(LinearLayoutWidget layout) {
    return Math.max(PANEL_MIN_WIDTH, Math.round(layout.getInnerWidth() * 0.3f));
  }

  private void setImage(Image image) {
    this.image = image;
    this.texture.setImage(this.getNativeImage());
    this.texture.upload();
    this.imageDisplay.setImage(image);
    this.infoTab.onImageChange(image);
    this.refreshWidgetPositions();
  }

  private NativeImage getNativeImage() {
    if (this.image == null) {
      return Image.empty().toNativeImage();
    }
    return this.image.toNativeImage();
  }

  private static abstract class ImageTab implements Tab {
    protected final MinecraftClient client;
    protected final TextRenderer textRenderer;
    protected final Text title;
    protected final LinearLayoutWidget layout = LinearLayoutWidget.vertical()
        .spacing(GuiUtil.PADDING)
        .padding(GuiUtil.PADDING);

    public ImageTab(MinecraftClient client, Text title) {
      this.client = client;
      this.textRenderer = client.textRenderer;
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
  }

  private static class InfoTab extends ImageTab {
    private final LabelWidget widthLabel;
    private final LabelWidget heightLabel;

    public InfoTab(
        MinecraftClient client,
        Image image,
        Consumer<Image> modifyImage) {
      // TODO: i18n
      super(client, Text.of("Info"));

      this.widthLabel = this.layout.add(
          LabelWidget.builder(this.textRenderer, this.getWidthText(image))
              .hideBackground()
              .showShadow()
              .build());
      this.heightLabel = this.layout.add(
          LabelWidget.builder(this.textRenderer, this.getHeightText(image))
              .hideBackground()
              .showShadow()
              .build());
    }

    public void onImageChange(Image image) {
      this.widthLabel.setText(this.getWidthText(image));
      this.heightLabel.setText(this.getHeightText(image));
    }

    private Text getWidthText(Image image) {
      // TODO: i18n
      return Text.of("Width: " + image.width() + "px");
    }

    private Text getHeightText(Image image) {
      // TODO: i18n
      return Text.of("Height: " + image.height() + "px");
    }
  }

  private static class OperationsTab extends ImageTab {
    private final List<Image.Operation> operations;
    private final Image originalImage;
    private final Consumer<Image> modifyImage;
    private final OperationList operationList;

    public OperationsTab(
        MinecraftClient client,
        List<Image.Operation> operations,
        Image originalImage,
        Consumer<Image> modifyImage) {
      // TODO: i18n
      super(client, Text.of("Operations"));

      this.operations = operations;
      this.originalImage = originalImage;
      this.modifyImage = modifyImage;

      this.layout.add(
          ButtonWidget.builder(
              Text.of("Invert"),
              (b) -> this.addOperation(Image.Operation.invert()))
              .build(),
          (parent, self) -> {
            self.setWidth(Math.min(parent.getInnerWidth(), ButtonWidget.DEFAULT_WIDTH_SMALL));
          });

      this.operationList = this.layout.add(
          new OperationList(
              this.client,
              this.layout.getInnerWidth(),
              this.layout.getUnusedSpace(null),
              this::removeOperation,
              operations),
          (parent, self) -> {
            self.setDimensions(parent.getInnerWidth(), parent.getUnusedSpace(self));
          });
    }

    private void addOperation(Image.Operation operation) {
      this.operations.add(operation);
      this.operationList.addOperation(operation);
      this.modifyImage.accept(this.originalImage.apply(this.operations));
    }

    private void removeOperation(int index) {
      if (index < 0 || index >= this.operations.size()) {
        return;
      }
      this.operations.remove(index);
      this.operationList.removeAndReflow(this.operations);
      this.modifyImage.accept(this.originalImage.apply(this.operations));
    }
  }

  private static class OperationList extends ParentElementEntryListWidget<OperationList.Entry> {
    private final Consumer<Integer> onDeletePress;

    public OperationList(
        MinecraftClient client,
        int width,
        int height,
        Consumer<Integer> onDeletePress,
        List<Image.Operation> operations) {
      super(client, 0, 0, width, height);

      this.onDeletePress = onDeletePress;
      for (int i = 0; i < operations.size(); i++) {
        this.addEntry(Entry.factory(this.client.textRenderer, this.onDeletePress, operations.get(i)));
      }
    }

    public void removeAndReflow(List<Image.Operation> operations) {
      this.removeEntry();
      // TODO: Do I need better checks here?
      for (int i = 0; i < Math.min(operations.size(), this.getEntryCount()); i++) {
        this.getEntry(i).setOperation(operations.get(i));
      }
    }

    public void addOperation(Image.Operation operation) {
      this.addEntry(Entry.factory(this.client.textRenderer, this.onDeletePress, operation));
    }

    static class Entry extends ParentElementEntryListWidget.Entry {
      private final LinearLayoutWidget layout;
      private final LabelWidget label;

      public Entry(
          TextRenderer textRenderer,
          int index,
          int left,
          int top,
          int width,
          Consumer<Integer> onDeletePress,
          Image.Operation operation) {
        super(index, left, top, width, 20);
        this.layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.label = this.layout.add(
            LabelWidget.builder(textRenderer, operation.getName())
                .hideBackground()
                .showShadow()
                .build(),
            (parent, self) -> {
              self.setWidth(parent.getUnusedSpace(self));
            });

        // TODO: i18n
        this.layout.add(IconButtonWidget.builder(BuiltinIcon.CANCEL_13, Constants.MOD_ID)
            .medium()
            .messageAndTooltip(Text.of("Delete"))
            .onPress((button) -> onDeletePress.accept(this.index))
            .build());

        this.addLayout(this.layout, (self) -> {
          self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight());
        });
        this.layout.forEachChild(this::addDrawableChild);
      }

      public void setOperation(Image.Operation operation) {
        this.label.setText(operation.getName());
      }

      public static FlowListWidget.EntryFactory<Entry> factory(
          TextRenderer textRenderer,
          Consumer<Integer> onDeletePress,
          Image.Operation operation) {
        return (index, left, top, width) -> new Entry(textRenderer, index, left, top, width, onDeletePress, operation);
      }
    }
  }
}
