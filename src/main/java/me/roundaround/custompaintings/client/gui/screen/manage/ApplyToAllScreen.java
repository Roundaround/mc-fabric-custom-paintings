package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.function.BiConsumer;

import me.roundaround.custompaintings.client.gui.DrawUtils;
import me.roundaround.custompaintings.util.UnknownPainting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ApplyToAllScreen extends Screen {
  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

  private final Screen parent;
  private final UnknownPainting selected;
  private final BiConsumer<UnknownPainting, Boolean> callback;

  public ApplyToAllScreen(
      Screen parent,
      int count,
      UnknownPainting selected,
      BiConsumer<UnknownPainting, Boolean> callback) {
    super(Text.translatable("custompaintings.applytoall.title", count));
    this.parent = parent;
    this.selected = selected;
    this.callback = callback;
  }

  @Override
  public void init() {
    addDrawableChild(new ButtonWidget(
        (this.width - PADDING) / 2 - BUTTON_WIDTH / 2,
        this.height / 2,
        BUTTON_WIDTH / 2,
        BUTTON_HEIGHT,
        ScreenTexts.YES,
        (button) -> {
          this.client.setScreen(this.parent);
          this.callback.accept(this.selected, true);
        }));

    addDrawableChild(new ButtonWidget(
        (this.width + PADDING) / 2,
        this.height / 2,
        BUTTON_WIDTH / 2,
        BUTTON_HEIGHT,
        ScreenTexts.NO,
        (button) -> {
          this.client.setScreen(this.parent);
          this.callback.accept(this.selected, false);
        }));

    addDrawableChild(new ButtonWidget(
        (this.width - BUTTON_WIDTH) / 2,
        this.height - BUTTON_HEIGHT - PADDING,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.CANCEL,
        (button) -> {
          this.close();
        }));
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackgroundTexture(0);

    DrawUtils.drawWrappedCenteredTextWithShadow(
        matrixStack,
        this.textRenderer,
        this.title,
        this.width / 2,
        40,
        0xFFFFFFFF,
        BUTTON_WIDTH + PADDING);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
