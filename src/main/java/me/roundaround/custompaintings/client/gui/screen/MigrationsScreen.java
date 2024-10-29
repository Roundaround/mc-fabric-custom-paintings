package me.roundaround.custompaintings.client.gui.screen;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.Objects;

public class MigrationsScreen extends Screen {
  private static final int BUTTON_WIDTH = ButtonWidget.field_49479;

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final Screen parent;

  private MigrationList list;

  protected MigrationsScreen(Screen parent) {
    super(Text.translatable("custompaintings.migrate.title"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    assert this.client != null;

    this.layout.addHeader(this.textRenderer, this.title);

    this.list = this.layout.addBody(new MigrationList(this.client, this.layout));

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::close).width(BUTTON_WIDTH).build());

    FabricLoader.getInstance().getModContainer(CustomPaintingsMod.MOD_ID).ifPresent((mod) -> {
      Text version = Text.of("v" + mod.getMetadata().getVersion().getFriendlyString());
      this.layout.addNonPositioned(LabelWidget.builder(this.textRenderer, version)
          .hideBackground()
          .showShadow()
          .alignSelfRight()
          .alignSelfBottom()
          .alignTextRight()
          .build(), (parent, self) -> self.setPosition(this.width - GuiUtil.PADDING, this.height - GuiUtil.PADDING));
    });

    this.layout.forEachChild(this::addDrawableChild);
    this.initTabNavigation();
  }

  @Override
  protected void initTabNavigation() {
    this.layout.refreshPositions();
  }

  @Override
  public void close() {
    Objects.requireNonNull(this.client).setScreen(this.parent);
  }

  private void close(ButtonWidget button) {
    this.close();
  }

  private static class MigrationList extends ParentElementEntryListWidget<MigrationList.Entry> {
    public MigrationList(MinecraftClient client, ThreeSectionLayoutWidget layout) {
      super(client, layout);
    }

    private static abstract class Entry extends ParentElementEntryListWidget.Entry {
      protected Entry(int index, int left, int top, int width, int contentHeight) {
        super(index, left, top, width, contentHeight);
      }
    }
  }
}
