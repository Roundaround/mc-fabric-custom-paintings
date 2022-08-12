package me.roundaround.custompaintings.client.gui;

import java.util.Collection;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen.Group;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;

@Environment(value = EnvType.CLIENT)
public class GroupsListWidget extends EntryListWidget<GroupsListWidget.GroupEntry> {
  private final PaintingEditScreen parent;
  private boolean hovered = false;

  public GroupsListWidget(
      PaintingEditScreen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom) {
    super(minecraftClient, width, height, top, bottom, 20);
    this.parent = parent;
    setRenderBackground(false);
    setRenderHeader(false, 0);
  }

  public void setGroups(Collection<Group> groups) {
    clearEntries();
    groups.stream()
        .map(GroupEntry::new)
        .forEach(this::addEntry);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    GroupEntry entry = (GroupEntry) getSelectedOrNull();
    return entry != null && entry.keyPressed(keyCode, scanCode, modifiers)
        || super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean changeFocus(boolean lookForwards) {
    if (getEntryCount() == 0) {
      return false;
    }

    GroupEntry previouslySelected = !isFocused() ? null : getSelectedOrNull();

    if (previouslySelected == null) {
      setSelected(getEntry(lookForwards ? 0 : getEntryCount() - 1));
    } else {
      moveSelection(lookForwards ? MoveDirection.DOWN : MoveDirection.UP);
    }

    GroupEntry nowSelected = getSelectedOrNull();
    if (nowSelected == null || nowSelected.equals(previouslySelected)) {
      return false;
    }

    ensureSelectedEntryVisible();
    return true;
  }

  @Override
  protected boolean isFocused() {
    return parent.getFocused() == this;
  }

  @Override
  protected boolean isSelectedEntry(int index) {
    return (isFocused() || hovered) && super.isSelectedEntry(index);
  }

  @Override
  public boolean isMouseOver(double mouseX, double mouseY) {
    hovered = super.isMouseOver(mouseX, mouseY);
    if (hovered) {
      GroupEntry entry = getEntryAtPosition(mouseX, mouseY);
      if (entry != null) {
        setSelected(entry);
      }
    }
    return hovered;
  }

  @Override
  public void appendNarrations(NarrationMessageBuilder builder) {
  }

  @Override
  protected void renderBackground(MatrixStack matrixStack) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferBuilder = tessellator.getBuffer();

    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

    int scrollAmount = Math.round((float) getScrollAmount());

    bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    bufferBuilder
        .vertex(0, bottom, 0)
        .texture(0, (float) (bottom + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder
        .vertex(parent.width, bottom, 0)
        .texture(parent.width / 32f, (float) (bottom + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder
        .vertex(parent.width, top, 0)
        .texture(parent.width / 32f, (float) (top + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder
        .vertex(0, top, 0)
        .texture(0, (float) (top + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    tessellator.draw();
  }

  @Environment(value = EnvType.CLIENT)
  public class GroupEntry extends EntryListWidget.Entry<GroupEntry> {
    private final Group group;

    public GroupEntry(Group group) {
      this.group = group;
    }

    @Override
    public void render(
        MatrixStack matrixStack,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      drawCenteredText(
          matrixStack,
          client.textRenderer,
          group.name(),
          x + entryWidth / 2,
          y + (entryHeight - client.textRenderer.fontHeight + 1) / 2,
          0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      press();
      return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      switch (keyCode) {
        case GLFW.GLFW_KEY_ENTER:
        case GLFW.GLFW_KEY_KP_ENTER:
          press();
          return true;
      }

      return false;
    }

    @Override
    public boolean changeFocus(boolean lookForwards) {
      return false;
    }

    private void press() {
      client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
      parent.selectGroup(group.id());
    }
  }
}
