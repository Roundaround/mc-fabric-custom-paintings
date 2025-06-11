package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.widget.ImageDisplayWidget;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ImageScreen extends BaseScreen {
  private static final int PANEL_MIN_WIDTH = 140;
  private static final Identifier TEXTURE_ID = Identifier.of(Constants.MOD_ID, "image_editor");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final ArrayList<Image.Operation> operations = new ArrayList<>();
  private final Consumer<Image> saveCallback;
  private final Image originalImage;
  private final NativeImageBackedTexture texture;

  private Image image;
  private LabelWidget widthLabel;
  private LabelWidget heightLabel;
  private OperationList operationList;
  private ImageDisplayWidget imageDisplay;

  public ImageScreen(
      @NotNull Text title,
      @NotNull ScreenParent parent,
      @NotNull MinecraftClient client,
      Image image,
      Consumer<Image> saveCallback) {
    super(title, parent, client);
    this.saveCallback = saveCallback;
    this.originalImage = image;
    this.image = image;

    this.texture = new NativeImageBackedTexture(() -> "Image Editor", this.getNativeImage());
    this.client.getTextureManager().registerTexture(TEXTURE_ID, texture);
  }

  @Override
  public void init() {
    this.layout.getBody().padding(GuiUtil.PADDING);

    this.layout.getBody()
        .flowAxis(Axis.HORIZONTAL)
        .spacing(GuiUtil.PADDING)
        .padding(GuiUtil.PADDING);

    LinearLayoutWidget sidePanel = LinearLayoutWidget.vertical()
        .spacing(GuiUtil.PADDING);

    this.widthLabel = sidePanel.add(
        LabelWidget.builder(this.client.textRenderer, this.getWidthText())
            .hideBackground()
            .showShadow()
            .build());
    this.heightLabel = sidePanel.add(
        LabelWidget.builder(this.client.textRenderer, this.getHeightText())
            .hideBackground()
            .showShadow()
            .build());

    sidePanel.add(
        ButtonWidget.builder(
            Text.of("Invert"),
            (b) -> this.addOperation(Image.Operation.invert()))
            .build(),
        (parent, self) -> {
          self.setWidth(Math.min(parent.getInnerWidth(), ButtonWidget.DEFAULT_WIDTH_SMALL));
        });

    this.operationList = sidePanel.add(
        new OperationList(
            this.client,
            sidePanel.getInnerWidth(),
            sidePanel.getUnusedSpace(null),
            this::removeOperation,
            this.operations),
        (parent, self) -> {
          self.setDimensions(parent.getInnerWidth(), parent.getUnusedSpace(self));
        });

    this.layout.addBody(sidePanel, (parent, self) -> {
      self.setDimensions(this.getPanelWidth(sidePanel), parent.getInnerHeight());
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

    this.layout.forEachChild(this::addDrawableChild);
    this.refreshWidgetPositions();
  }

  @Override
  protected void refreshWidgetPositions() {
    this.layout.refreshPositions();
  }

  @Override
  public void removed() {
    this.client.getTextureManager().destroyTexture(TEXTURE_ID);
  }

  private int getPanelWidth(LinearLayoutWidget layout) {
    return Math.max(PANEL_MIN_WIDTH, Math.round(layout.getInnerWidth() * 0.3f));
  }

  private void addOperation(Image.Operation operation) {
    this.operations.add(operation);
    this.operationList.addOperation(operation);
    this.setImage(this.originalImage.apply(this.operations));
  }

  private void removeOperation(int index) {
    if (index < 0 || index >= this.operations.size()) {
      return;
    }
    this.operations.remove(index);
    this.operationList.removeAndReflow(this.operations);
    this.setImage(this.originalImage.apply(this.operations));
  }

  private void setImage(Image image) {
    this.image = image;
    this.texture.setImage(this.getNativeImage());
    this.texture.upload();
    this.imageDisplay.setImage(image);
    this.widthLabel.setText(this.getWidthText());
    this.heightLabel.setText(this.getHeightText());
    this.refreshWidgetPositions();
  }

  private NativeImage getNativeImage() {
    if (this.image == null) {
      return Image.empty().toNativeImage();
    }
    return this.image.toNativeImage();
  }

  private Text getWidthText() {
    // TODO: i18n
    return Text.of("Width: " + this.image.width() + "px");
  }

  private Text getHeightText() {
    // TODO: i18n
    return Text.of("Height: " + this.image.height() + "px");
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
