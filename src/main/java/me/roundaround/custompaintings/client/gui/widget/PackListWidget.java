package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.NarratableEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class PackListWidget extends NarratableEntryListWidget<PackListWidget.Entry> {
  private final Consumer<String> onPackSelect;

  public PackListWidget(MinecraftClient client, ThreeSectionLayoutWidget layout, Consumer<String> onPackSelect) {
    super(client, layout);

    this.setAlternatingRowShading(true);
    this.setAutoPadForShading(false);

    this.onPackSelect = onPackSelect;
  }

  public void setGroups(Collection<PackData> packs) {
    this.clearEntries();
    packs.forEach((pack) -> this.addEntry(this.getEntryFactory(this.client.textRenderer, this.onPackSelect, pack)));
  }

  @Override
  protected int getPreferredContentWidth() {
    return VANILLA_LIST_WIDTH_M;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    Entry hovered = this.getHoveredEntry();
    if (hovered != null && hovered.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  private EntryFactory<Entry> getEntryFactory(TextRenderer textRenderer, Consumer<String> onSelect, PackData pack) {
    return (index, left, top, width) -> new Entry(textRenderer, onSelect, pack, index, left, top, width);
  }

  @Environment(value = EnvType.CLIENT)
  public static class Entry extends NarratableEntryListWidget.Entry {
    protected static final int HEIGHT = 36;

    private final Consumer<String> onSelect;
    private final PackData pack;

    public Entry(
        TextRenderer textRenderer,
        Consumer<String> onSelect,
        PackData pack,
        int index,
        int left,
        int top,
        int width
    ) {
      super(index, left, top, width, HEIGHT);
      this.onSelect = onSelect;
      this.pack = pack;

      LinearLayoutWidget layout = this.addLayout(
          LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING).defaultOffAxisContentAlignCenter(), (self) -> {
            self.setPosition(this.getContentLeft(), this.getContentTop());
            self.setDimensions(this.getContentWidth(), this.getContentHeight());
          }
      );

      layout.add(
          SpriteWidget.create(ClientPaintingRegistry.getInstance()
              .getSprite(PackIcons.customId(this.pack.id()))),
          (parent, self) -> self.setDimensions(this.getIconWidth(), this.getIconHeight())
      );

      LinearLayoutWidget column = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING).mainAxisContentAlignCenter();

      column.add(
          LabelWidget.builder(textRenderer, Text.of(pack.name()))
              .alignTextLeft()
              .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
              .hideBackground()
              .showShadow()
              .build(), (parent, self) -> self.setWidth(parent.getWidth())
      );
      if (pack.description().isPresent() && !pack.description().get().isBlank()) {
        column.add(
            LabelWidget.builder(textRenderer, Text.literal(pack.description().get()).formatted(Formatting.GRAY))
                .alignTextLeft()
                .overflowBehavior(LabelWidget.OverflowBehavior.WRAP)
                .maxLines(2)
                .hideBackground()
                .showShadow()
                .build(), (parent, self) -> self.setWidth(parent.getWidth())
        );
      }

      layout.add(
          column, (parent, self) -> {
            self.setDimensions(this.getContentWidth() - GuiUtil.PADDING - this.getIconWidth(), this.getContentHeight());
          }
      );

      layout.forEachChild(this::addDrawable);
    }

    private int getIconWidth() {
      return this.getContentHeight();
    }

    private int getIconHeight() {
      return this.getContentHeight();
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
      return Text.literal(this.pack.name());
    }

    private void press() {
      this.onSelect.accept(this.pack.id());
    }
  }
}
