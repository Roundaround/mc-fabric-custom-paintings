package me.roundaround.custompaintings.client.gui;

import java.util.Collection;

import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen;
import me.roundaround.custompaintings.client.gui.screen.PaintingEditScreen.Group;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Environment(value = EnvType.CLIENT)
public class GroupsListWidget extends AlwaysSelectedEntryListWidget<GroupsListWidget.GroupEntry> {
  private final PaintingEditScreen parent;

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
    setRenderHorizontalShadows(false);
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
  protected boolean isFocused() {
    return parent.getFocused() == this;
  }

  @Environment(value = EnvType.CLIENT)
  public class GroupEntry extends AlwaysSelectedEntryListWidget.Entry<GroupEntry> {
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
          y + (entryHeight - client.textRenderer.fontHeight) / 2,
          0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      parent.selectGroup(group.id());
      return true;
    }

    @Override
    public Text getNarration() {
      return null;
    }
  }
}
