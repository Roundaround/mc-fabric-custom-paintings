package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.resource.PackIcons;
import me.roundaround.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.widget.NarratableEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.Collection;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public class PackListWidget extends NarratableEntryListWidget<PackListWidget.Entry> {
  private final Consumer<String> onPackSelect;

  public PackListWidget(Minecraft client, ThreeSectionLayoutWidget layout, Consumer<String> onPackSelect) {
    super(client, layout);

    this.setAlternatingRowShading(true);
    this.setAutoPadForShading(false);

    this.onPackSelect = onPackSelect;
  }

  public void setGroups(Collection<PackData> packs) {
    this.clearEntries();
    packs.forEach((pack) -> this.addEntry(this.getEntryFactory(this.client.font, this.onPackSelect, pack)));
  }

  @Override
  protected int getPreferredContentWidth() {
    return VANILLA_LIST_WIDTH_M;
  }

  @Override
  public boolean keyPressed(KeyEvent input) {
    Entry hovered = this.getHoveredEntry();
    if (hovered != null && hovered.keyPressed(input)) {
      return true;
    }
    return super.keyPressed(input);
  }

  private EntryFactory<Entry> getEntryFactory(Font textRenderer, Consumer<String> onSelect, PackData pack) {
    return (index, left, top, width) -> new Entry(textRenderer, onSelect, pack, index, left, top, width);
  }

  @Environment(value = EnvType.CLIENT)
  public static class Entry extends NarratableEntryListWidget.Entry {
    protected static final int HEIGHT = 36;

    private final Consumer<String> onSelect;
    private final PackData pack;

    public Entry(
        Font textRenderer,
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
          (parent, self) -> self.setSize(this.getIconWidth(), this.getIconHeight())
      );

      LinearLayoutWidget column = LinearLayoutWidget.vertical().spacing(GuiUtil.PADDING).mainAxisContentAlignCenter();

      column.add(
          LabelWidget.builder(textRenderer, Component.nullToEmpty(pack.name()))
              .alignTextLeft()
              .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
              .hideBackground()
              .showShadow()
              .build(), (parent, self) -> self.setWidth(parent.getWidth())
      );
      if (pack.description().isPresent() && !pack.description().get().isBlank()) {
        column.add(
            LabelWidget.builder(textRenderer, Component.literal(pack.description().get()).withStyle(ChatFormatting.GRAY))
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

      layout.visitWidgets(this::addDrawable);
    }

    private int getIconWidth() {
      return this.getContentHeight();
    }

    private int getIconHeight() {
      return this.getContentHeight();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      this.press();
      return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
      if (input.isConfirmation()) {
        this.press();
        return true;
      }
      return false;
    }

    @Override
    public Component getNarration() {
      return Component.literal(this.pack.name());
    }

    private void press() {
      this.onSelect.accept(this.pack.id());
    }
  }
}
