package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.roundalib.client.gui.widget.AlwaysSelectedFlowListWidget;
import me.roundaround.roundalib.client.gui.widget.LabelWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class GroupListWidget extends AlwaysSelectedFlowListWidget<GroupListWidget.Entry> {
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

  @Environment(value = EnvType.CLIENT)
  public static class Entry extends AlwaysSelectedFlowListWidget.Entry {
    private final Consumer<String> onSelect;
    private final Group group;
    private final LabelWidget label;

    public Entry(
        TextRenderer textRenderer, Consumer<String> onSelect, Group group, int index, int left, int top, int width
    ) {
      super(index, left, top, width, 20);
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

      this.addDrawableChild(this.label);
    }

    @Override
    public void refreshPositions() {
      super.refreshPositions();
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
    public Text getNarration() {
      return Text.literal(this.group.name());
    }

    private void press() {
      this.onSelect.accept(this.group.id());
    }
  }
}
