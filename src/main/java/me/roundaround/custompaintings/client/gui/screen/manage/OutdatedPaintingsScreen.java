package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.HashSet;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.widget.OutdatedPaintingListWidget;
import me.roundaround.custompaintings.server.ServerPaintingManager.OutdatedPainting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class OutdatedPaintingsScreen extends Screen {
  private final ManagePaintingsScreen parent;

  private OutdatedPaintingListWidget list;

  public OutdatedPaintingsScreen(ManagePaintingsScreen parent) {
    super(Text.translatable("custompaintings.outdated.title"));
    this.parent = parent;
  }

  public void setOutdatedPaintings(HashSet<OutdatedPainting> outdatedPaintings) {
    if (this.list != null) {
      this.list.receiveData(outdatedPaintings);
    }
  }

  @Override
  public void init() {
    this.list = new OutdatedPaintingListWidget(
        this,
        this.client,
        this.width,
        this.height,
        32,
        this.height - 32);
        addSelectableChild(this.list);
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      this.client.setScreen(this.parent);
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.list.render(matrixStack, mouseX, mouseY, partialTicks);
    
    drawCenteredText(matrixStack, this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFF);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
