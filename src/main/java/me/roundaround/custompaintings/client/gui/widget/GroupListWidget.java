package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
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

  public GroupListWidget(
      Screen parent,
      MinecraftClient minecraftClient,
      int width,
      int height,
      int y,
      Consumer<String> onGroupSelect) {
    super(minecraftClient, width, height, y, ITEM_HEIGHT);
    this.parent = parent;
    this.onGroupSelect = onGroupSelect;
    setRenderHeader(false, 0);
  }

  public void setGroups(Collection<Group> groups) {
    this.clearEntries();
    groups.stream().map(GroupEntry::new).forEach(this::addEntry);
    if (this.getEntryCount() > 0) {
      this.setSelected(this.getEntry(0));
    }
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
  public boolean isMouseOver(double mouseX, double mouseY) {
    boolean hovered = super.isMouseOver(mouseX, mouseY);
    if (hovered) {
      GroupEntry entry = getEntryAtPosition(mouseX, mouseY);
      if (entry != null) {
        setSelected(entry);
      }
    }
    return hovered;
  }

  @Override
  public void appendClickableNarrations(NarrationMessageBuilder builder) {
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
