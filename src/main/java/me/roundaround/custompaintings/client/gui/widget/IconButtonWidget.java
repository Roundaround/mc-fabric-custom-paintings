package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.CustomPaintingsMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(value = EnvType.CLIENT)
public class IconButtonWidget extends ButtonWidget {
  public static final int FILTER_ICON = 0;
  public static final int RESET_ICON = 1;
  public static final int LEFT_ICON = 2;
  public static final int RIGHT_ICON = 3;
  public static final int WRENCH_ICON = 4;
  public static final int WIDTH = 20;
  public static final int HEIGHT = 20;
  protected static final Identifier BACKGROUND_TEXTURE = new Identifier(
      Identifier.DEFAULT_NAMESPACE,
      "textures/gui/widgets.png");
  protected static final Identifier WIDGETS_TEXTURE = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "textures/gui/widgets.png");

  protected final int textureIndex;

  public static Builder builder(Text tooltip, PressAction onPress, int textureIndex) {
    return new Builder(tooltip, onPress, textureIndex);
  }

  /**
   * @deprecated Use {@link #builder(Text, PressAction, int)} instead.
   */
  public static ButtonWidget.Builder builder(Text message, PressAction onPress) {
    throw new UnsupportedOperationException();
  }

  protected IconButtonWidget(
      int x,
      int y,
      int textureIndex,
      Text tooltip,
      PressAction onPress,
      NarrationSupplier narrationSupplier) {
    super(
        x,
        y,
        WIDTH,
        HEIGHT,
        tooltip,
        onPress,
        narrationSupplier);
    this.textureIndex = textureIndex;
    setTooltip(Tooltip.of(tooltip));
  }

  @Override
  public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
    RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

    RenderSystem.setShaderTexture(0, BACKGROUND_TEXTURE);
    int vOffset = (this.isHovered() ? 2 : 1) * HEIGHT;
    drawTexture(
        matrixStack,
        this.getX(),
        this.getY(),
        0,
        46 + vOffset,
        WIDTH / 2,
        HEIGHT);
    drawTexture(
        matrixStack,
        this.getX() + WIDTH / 2,
        this.getY(),
        200 - WIDTH / 2,
        46 + vOffset,
        WIDTH / 2,
        HEIGHT);

    int uIndex = this.textureIndex % 5;
    int vIndex = this.textureIndex / 5;
    RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
    drawTexture(
        matrixStack,
        this.getX(),
        this.getY(),
        uIndex * WIDTH,
        vIndex * HEIGHT,
        WIDTH,
        HEIGHT,
        WIDTH * 5,
        HEIGHT * 3);
  }

  @Environment(value = EnvType.CLIENT)
  public static class Builder {
    private final PressAction onPress;
    private final Text tooltip;
    private final int textureIndex;

    private int x;
    private int y;
    private NarrationSupplier narrationSupplier = DEFAULT_NARRATION_SUPPLIER;

    public Builder(Text tooltip, PressAction onPress, int textureIndex) {
      this.onPress = onPress;
      this.tooltip = tooltip;
      this.textureIndex = textureIndex;
    }

    public Builder position(int x, int y) {
      this.x = x;
      this.y = y;
      return this;
    }

    public Builder narrationSupplier(NarrationSupplier narrationSupplier) {
      this.narrationSupplier = narrationSupplier;
      return this;
    }

    public IconButtonWidget build() {
      return new IconButtonWidget(
          x,
          y,
          this.textureIndex,
          this.tooltip,
          this.onPress,
          narrationSupplier);
    }
  }
}
