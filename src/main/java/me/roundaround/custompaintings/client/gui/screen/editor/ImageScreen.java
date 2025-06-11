package me.roundaround.custompaintings.client.gui.screen.editor;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.widget.ImageDisplayWidget;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ImageScreen extends BaseScreen {
  private static final Identifier TEXTURE_ID = Identifier.of(Constants.MOD_ID, "image_editor");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Consumer<Image> saveCallback;
  private final NativeImageBackedTexture texture;

  private Image image;
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
    this.layout.getBody().padding(GuiUtil.PADDING);

    this.imageDisplay = this.layout.addBody(
        new ImageDisplayWidget((image) -> TEXTURE_ID, this.image),
        (parent, self) -> {
          self.setDimensions(
              Math.min(300, parent.getInnerWidth()),
              Math.min(300, parent.getInnerHeight()));
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
    this.layout.addFooter(ButtonWidget.builder(
        Text.of("Invert"),
        (b) -> this.setImage(this.image.invert()))
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

  private void setImage(Image image) {
    this.image = image;
    this.texture.setImage(this.getNativeImage());
    this.texture.upload();
    this.imageDisplay.setImage(image);
    this.refreshWidgetPositions();
  }

  private NativeImage getNativeImage() {
    if (this.image == null) {
      return Image.empty().toNativeImage();
    }
    return this.image.toNativeImage();
  }
}
