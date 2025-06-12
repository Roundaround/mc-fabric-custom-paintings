package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
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
import me.roundaround.custompaintings.roundalib.client.gui.util.FloatRect;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.util.IntRect;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.roundalib.observable.Observable;
import me.roundaround.custompaintings.roundalib.observable.Subject;
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
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public class PaintingScreen extends BaseScreen {
  private static final int PANEL_MIN_WIDTH = 140;
  private static final Identifier IMAGE_TEXTURE = Identifier.of(Constants.MOD_ID, "image_editor");
  private static final Identifier TAB_HEADER_BACKGROUND_TEXTURE = Identifier
      .ofVanilla("textures/gui/tab_header_background.png");
  private static final Identifier SHADOW_TEXTURE = Identifier.of(Constants.MOD_ID, "shadow_8px");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final TabManager tabManager = new TabManager(
      (element) -> this.addDrawableChild(element),
      (child) -> this.remove(child));
  private final Consumer<PackData.Painting> saveCallback;
  private final State state;
  private final NativeImageBackedTexture texture;
  private final Subject<IntRect> imageRegionBounds = Subject.of(null);
  private final Subject<Boolean> showBackground = Subject.of(true);

  private TabNavigationWidget tabNavigation;
  private InfoTab infoTab;
  private ImageTab imageTab;
  private FillerWidget tabRegion;
  private IntRect frameBounds;
  private float pixelsPerBlock;
  private FloatRect imageBounds;
  private Background background = Background.DARK_OAK;

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
    this.client.getTextureManager().registerTexture(IMAGE_TEXTURE, texture);
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

    this.tabRegion = this.layout.addBody(FillerWidget.empty(), (parent, self) -> {
      self.setDimensions(this.getPanelWidth(parent), parent.getInnerHeight());
    });

    LinearLayoutWidget imageRegion = this.layout.addBody(
        LinearLayoutWidget.vertical()
            .spacing(GuiUtil.PADDING / 2)
            .defaultOffAxisContentAlignCenter(),
        (parent, self) -> {
          self.setDimensions(parent.getUnusedSpace(self), parent.getInnerHeight());
        });

    imageRegion.add(FillerWidget.empty(), (parent, self) -> {
      self.setDimensions(parent.getInnerWidth(), parent.getUnusedSpace(self));
      this.imageRegionBounds.set(IntRect.fromWidget(self));
    });

    LinearLayoutWidget buttonRow = LinearLayoutWidget.horizontal()
        .spacing(GuiUtil.PADDING);

    // TODO: Better icon + i18n
    IconButtonWidget changeBackgroundButton = buttonRow.add(
        IconButtonWidget.builder(BuiltinIcon.ROTATE_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.of("Change background texture"))
            .onPress((button) -> {
              this.background = this.background.next();
            })
            .build());
    this.showBackground.subscribe((showBackground) -> {
      changeBackgroundButton.active = showBackground;
    });

    // TODO: Better icon (or actual checkbox?) + i18n
    buttonRow.add(
        IconButtonWidget.builder(BuiltinIcon.CHECKMARK_18, Constants.MOD_ID)
            .vanillaSize()
            .messageAndTooltip(Text.of("Toggle background"))
            .onPress((button) -> {
              this.showBackground.update((showBackground) -> !showBackground);
            })
            .build());

    imageRegion.add(buttonRow);

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

    this.state.image.subscribe((image) -> {
      this.texture.setImage(getNativeImage(image));
      this.texture.upload();
      this.refreshWidgetPositions();
    });

    Observable.subscribeAll(
        this.imageRegionBounds,
        this.state.blockWidth,
        this.state.blockHeight,
        this.showBackground,
        (region, width, height, showBackground) -> {
          if (region == null) {
            return;
          }

          int regionWidth = region.getWidth();
          int regionHeight = region.getHeight();
          int blockWidth = width + (showBackground ? 2 : 0);
          int blockHeight = height + (showBackground ? 2 : 0);

          float scale = Math.min(
              (float) regionWidth / blockWidth,
              (float) regionHeight / blockHeight);
          int scaledWidth = Math.round(scale * blockWidth);
          int scaledHeight = Math.round(scale * blockHeight);

          this.pixelsPerBlock = (float) scaledWidth / blockWidth;
          this.frameBounds = IntRect.byDimensions(
              region.left() + (regionWidth - scaledWidth) / 2,
              region.top() + (regionHeight - scaledHeight) / 2,
              scaledWidth,
              scaledHeight);
          this.imageBounds = this.frameBounds
              .toFloatRect()
              .reduce(showBackground ? this.pixelsPerBlock : 1);

          this.layout.refreshPositions();
        });
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
        this.tabRegion.getX(),
        this.tabRegion.getY(),
        this.tabRegion.getWidth(),
        this.tabRegion.getHeight());
    this.tabManager.setTabArea(tabArea);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    super.render(context, mouseX, mouseY, deltaTicks);

    GuiUtil.drawTexturedQuad(
        context,
        RenderLayer::getGuiTextured,
        IMAGE_TEXTURE,
        this.imageBounds.left(),
        this.imageBounds.right(),
        this.imageBounds.top(),
        this.imageBounds.bottom());

    context.drawTexture(
        RenderLayer::getGuiTextured, FOOTER_SEPARATOR_TEXTURE, 0,
        this.height - this.layout.getFooterHeight(), 0, 0, this.width, 2, 32, 2);
  }

  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    super.renderBackground(context, mouseX, mouseY, deltaTicks);

    if (!this.showBackground.get()) {
      GuiUtil.drawBorder(
          context,
          this.imageBounds,
          Colors.BLACK,
          true);
      return;
    }

    for (int x = 0; x < this.state.blockWidth.get() + 2; x++) {
      for (int y = 0; y < this.state.blockHeight.get() + 2; y++) {
        float posX = this.frameBounds.left() + (x * this.pixelsPerBlock);
        float posY = this.frameBounds.top() + (y * this.pixelsPerBlock);
        GuiUtil.drawTexturedQuad(
            context,
            RenderLayer::getGuiTextured,
            this.background.get(),
            posX,
            posX + this.pixelsPerBlock,
            posY,
            posY + this.pixelsPerBlock);
      }
    }

    float shadowSize = 2 * this.pixelsPerBlock / 16; // 2 "block pixels"
    GuiUtil.drawSpriteNineSliced(
        context,
        RenderLayer::getGuiTextured,
        SHADOW_TEXTURE,
        this.imageBounds.left() - shadowSize,
        this.imageBounds.top() - shadowSize,
        this.imageBounds.getWidth() + shadowSize * 2,
        this.imageBounds.getHeight() + shadowSize * 2,
        32,
        32,
        GuiUtil.genColorInt(1f, 1f, 1f, 0.3f),
        8);
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
    this.client.getTextureManager().destroyTexture(IMAGE_TEXTURE);
  }

  private int getPanelWidth(LinearLayoutWidget layout) {
    return Math.max(PANEL_MIN_WIDTH, Math.round(layout.getInnerWidth() * 0.3f));
  }

  private static NativeImage getNativeImage(Image image) {
    if (image == null) {
      return Image.empty().toNativeImage();
    }
    return image.toNativeImage();
  }

  private enum Background {
    DARK_OAK(Identifier.ofVanilla("textures/block/dark_oak_planks.png")),
    DEEPSLATE(Identifier.ofVanilla("textures/block/deepslate_bricks.png")),
    QUARTZ(Identifier.ofVanilla("textures/block/quartz_block_side.png"));

    private Identifier id;

    Background(Identifier id) {
      this.id = id;
    }

    public Identifier get() {
      return this.id;
    }

    public Background next() {
      return values()[(this.ordinal() + 1) % values().length];
    }
  }
}
