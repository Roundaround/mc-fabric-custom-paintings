package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.LabelWidget;
import me.roundaround.roundalib.client.gui.widget.NarratableEntryListWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class GroupListWidget extends NarratableEntryListWidget<GroupListWidget.Entry> {
  private final Consumer<String> onGroupSelect;

  public GroupListWidget(
      MinecraftClient client, ThreePartsLayoutWidget layout, Consumer<String> onGroupSelect
  ) {
    super(client, layout);

    this.onGroupSelect = onGroupSelect;
  }

  public void setGroups(Collection<Group> groups) {
    this.clearEntries();
    groups.forEach((group) -> this.addEntry(this.getEntryFactory(this.client.textRenderer, this.onGroupSelect, group)));
  }

  private EntryFactory<Entry> getEntryFactory(TextRenderer textRenderer, Consumer<String> onSelect, Group group) {
    return (index, left, top, width) -> new Entry(textRenderer, onSelect, group, index, left, top, width);
  }

  @Override
  protected int getPreferredContentWidth() {
    return VANILLA_LIST_WIDTH_M;
  }

  @Override
  protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, Entry entry) {
    boolean showSelected = entry == this.getSelected() || entry == this.getHoveredEntry();
    if (showSelected) {
      entry.renderSelectionBackground(context);
    }
    super.renderEntry(context, mouseX, mouseY, delta, entry);
    if (showSelected) {
      entry.renderSelectionHighlight(context);
    }
  }

  @Environment(value = EnvType.CLIENT)
  public static class Entry extends NarratableEntryListWidget.Entry {
    private final Consumer<String> onSelect;
    private final Group group;
    private final LabelWidget label;

    public Entry(
        TextRenderer textRenderer, Consumer<String> onSelect, Group group, int index, int left, int top, int width
    ) {
      super(index, left, top, width, 17);
      this.onSelect = onSelect;
      this.group = group;

      this.label = LabelWidget.builder(textRenderer, Text.of(group.name()))
          .refPosition(this.getContentCenterX(), this.getContentCenterY())
          .dimensions(this.getContentWidth(), this.getContentHeight())
          .justifiedCenter()
          .alignedMiddle()
          .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
          .showShadow()
          .hideBackground()
          .build();

      this.addDrawable(this.label);
    }

    @Override
    public void refreshPositions() {
      this.label.batchUpdates(() -> {
        this.label.setPosition(this.getContentCenterX(), this.getContentCenterY());
        this.label.setDimensions(this.getContentWidth(), this.getContentHeight());
      });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.press();
      return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return switch (keyCode) {
        case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
          this.press();
          yield true;
        }
        default -> false;
      };
    }

    @Override
    protected void renderSelectionBackground(DrawContext context) {
      context.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), this.isFocused() ? Colors.BLACK : GuiUtil.BACKGROUND_COLOR);
    }

    @Override
    protected void renderSelectionHighlight(DrawContext context) {
      context.drawBorder(
          this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isFocused() ? Colors.WHITE : Colors.GRAY);
    }

    @Override
    public Text getNarration() {
      return Text.literal(this.group.name());
    }

    private void press() {
      this.onSelect.accept(this.group.id());
    }
  }
}
