package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.CustomPaintingsMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
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
  protected static final Identifier WIDGETS_TEXTURE =
      new Identifier(CustomPaintingsMod.MOD_ID, "textures/gui/widgets.png");

  protected final int textureIndex;

  public static Builder builder(Text tooltip, PressAction onPress, int textureIndex) {
    return new Builder(tooltip, onPress, textureIndex);
  }

  protected IconButtonWidget(
      int x,
      int y,
      int textureIndex,
      Text tooltip,
      PressAction onPress,
      NarrationSupplier narrationSupplier) {
    super(x, y, WIDTH, HEIGHT, Text.empty(), onPress, narrationSupplier);
    this.textureIndex = textureIndex;
    setTooltip(Tooltip.of(tooltip));
  }

  @Override
  public void renderButton(DrawContext drawContext, int mouseX, int mouseY, float delta) {
    super.renderButton(drawContext, mouseX, mouseY, delta);

    float brightness = this.active ? 1f : 0.6f;
    int uIndex = this.textureIndex % 5;
    int vIndex = this.textureIndex / 5;

    RenderSystem.setShaderColor(brightness, brightness, brightness, 1f);
    drawContext.drawTexture(WIDGETS_TEXTURE,
        getX(),
        getY(),
        uIndex * WIDTH,
        vIndex * HEIGHT,
        this.width,
        this.height,
        100,
        60);
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
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
      return new IconButtonWidget(x,
          y,
          this.textureIndex,
          this.tooltip,
          this.onPress,
          narrationSupplier);
    }
  }
}
