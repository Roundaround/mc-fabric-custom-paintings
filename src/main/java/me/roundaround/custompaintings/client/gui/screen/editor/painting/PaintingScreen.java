package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
import me.roundaround.custompaintings.client.gui.widget.ImageDisplayWidget;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.layout.FillerWidget;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import me.roundaround.custompaintings.roundalib.client.gui.util.Axis;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PaintingScreen extends BaseScreen {
  private static final int PANEL_MIN_WIDTH = 140;
  private static final Identifier TEXTURE_ID = Identifier.of(Constants.MOD_ID, "image_editor");
  private static final Identifier TAB_HEADER_BACKGROUND_TEXTURE = Identifier
      .ofVanilla("textures/gui/tab_header_background.png");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final TabManager tabManager = new TabManager(
      (element) -> this.addDrawableChild(element),
      (child) -> this.remove(child));
  private final Consumer<PackData.Painting> saveCallback;
  private final State state;
  private final NativeImageBackedTexture texture;

  private TabNavigationWidget tabNavigation;
  private InfoTab infoTab;
  private ImageTab imageTab;
  private FillerWidget tabAreaPlaceholder;
  private ImageDisplayWidget imageDisplay;

  public PaintingScreen(
      @NotNull Text title,
      @NotNull ScreenParent parent,
      @NotNull MinecraftClient client,
      PackData.Painting painting,
      Consumer<PackData.Painting> saveCallback) {
    super(title, parent, client);
    this.saveCallback = saveCallback;

    this.state = new State(painting);
    this.texture = new NativeImageBackedTexture(() -> "Image Editor", getNativeImage(painting.image()));
    this.client.getTextureManager().registerTexture(TEXTURE_ID, texture);
  }

  @Override
  public void init() {
    this.infoTab = new InfoTab(this.client, this.state);
    this.imageTab = new ImageTab(this.client, this.state);
    this.tabNavigation = TabNavigationWidget.builder(this.tabManager, this.width)
        .tabs(this.infoTab, this.imageTab)
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
        new ImageDisplayWidget((image) -> TEXTURE_ID, this.state.image.get()),
        (parent, self) -> {
          self.setDimensions(parent.getUnusedSpace(self), parent.getInnerHeight());
        });

    this.layout.addFooter(ButtonWidget.builder(
        ScreenTexts.DONE,
        (b) -> {
          this.saveCallback.accept(this.state.getPainting());
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

    this.state.image.subscribe(this::setImage);
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
    this.texture.setImage(getNativeImage(image));
    this.texture.upload();
    this.imageDisplay.setImage(image);
    this.refreshWidgetPositions();
  }

  private static NativeImage getNativeImage(Image image) {
    if (image == null) {
      return Image.empty().toNativeImage();
    }
    return image.toNativeImage();
  }
}
