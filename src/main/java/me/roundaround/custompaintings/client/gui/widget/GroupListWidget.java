package me.roundaround.custompaintings.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class GroupListWidget extends AlwaysSelectedEntryListWidget<GroupListWidget.GroupEntry> {
  private static final int ITEM_HEIGHT = 20;

  private final Screen parent;
  private final Consumer<String> onGroupSelect;
  private boolean hovered = false;

  public GroupListWidget(
      Screen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int top,
      int bottom,
      Consumer<String> onGroupSelect) {
    super(minecraftClient, width, height, top, bottom, ITEM_HEIGHT);
    this.parent = parent;
    this.onGroupSelect = onGroupSelect;
    setRenderBackground(false);
    setRenderHeader(false, 0);
  }

  public void setGroups(Collection<Group> groups) {
    clearEntries();
    groups.stream().map(GroupEntry::new).forEach(this::addEntry);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    GroupEntry entry = getSelectedOrNull();
    return entry != null && entry.keyPressed(keyCode, scanCode, modifiers) ||
        super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean isFocused() {
    return this.parent.getFocused() == this;
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
  public void render(DrawContext context, int mouseX, int mouseY, float partialTicks) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, partialTicks);
  }

  protected void renderBackground(DrawContext drawContext) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferBuilder = tessellator.getBuffer();

    RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
    RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

    int scrollAmount = Math.round((float) getScrollAmount());

    bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    bufferBuilder.vertex(0, bottom, 0)
        .texture(0, (float) (bottom + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder.vertex(parent.width, bottom, 0)
        .texture(parent.width / 32f, (float) (bottom + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder.vertex(parent.width, top, 0)
        .texture(parent.width / 32f, (float) (top + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    bufferBuilder.vertex(0, top, 0)
        .texture(0, (float) (top + scrollAmount) / 32f)
        .color(32, 32, 32, 255)
        .next();
    tessellator.draw();
  }

  @Environment(value = EnvType.CLIENT)
  public class GroupEntry extends AlwaysSelectedEntryListWidget.Entry<GroupEntry> {
    private final Group group;

    public GroupEntry(Group group) {
      this.group = group;
    }

    @Override
    public void render(
        DrawContext drawContext,
        int index,
        int y,
        int x,
        int entryWidth,
        int entryHeight,
        int mouseX,
        int mouseY,
        boolean hovered,
        float partialTicks) {
      drawContext.drawCenteredTextWithShadow(client.textRenderer,
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
    public Text getNarration() {
      return Text.literal(group.name());
    }

    private void press() {
      client.getSoundManager()
          .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
      GroupListWidget.this.onGroupSelect.accept(group.id());
    }
  }
}
