package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.util.UnknownPainting;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.TextAlignment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;

public class ApplyToAllScreen extends Screen {
  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int PADDING = 8;

  private final Screen parent;
  private final UnknownPainting selected;
  private final BiConsumer<UnknownPainting, Boolean> callback;

  public ApplyToAllScreen(
      Screen parent, int count, UnknownPainting selected, BiConsumer<UnknownPainting, Boolean> callback
  ) {
    super(Text.translatable("custompaintings.applytoall.title", count));
    this.parent = parent;
    this.selected = selected;
    this.callback = callback;
  }

  @Override
  public void init() {
    this.addDrawableChild(ButtonWidget.builder(ScreenTexts.YES, (button) -> {
          this.client.setScreen(this.parent);
          this.callback.accept(this.selected, true);
        })
        .position((this.width - PADDING) / 2 - BUTTON_WIDTH / 2, this.height / 2)
        .size(BUTTON_WIDTH / 2, BUTTON_HEIGHT)
        .build());

    this.addDrawableChild(ButtonWidget.builder(ScreenTexts.NO, (button) -> {
      this.client.setScreen(this.parent);
      this.callback.accept(this.selected, false);
    }).position((this.width + PADDING) / 2, this.height / 2).size(BUTTON_WIDTH / 2, BUTTON_HEIGHT).build());

    this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
          this.close();
        })
        .position((this.width - BUTTON_WIDTH) / 2, this.height - BUTTON_HEIGHT - PADDING)
        .size(BUTTON_WIDTH, BUTTON_HEIGHT)
        .build());
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }

  @Override
  public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
    GuiUtil.drawWrappedText(drawContext, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFFFF, true,
        BUTTON_WIDTH + PADDING, 0, 0, TextAlignment.CENTER
    );

    super.render(drawContext, mouseX, mouseY, partialTicks);
  }
}
