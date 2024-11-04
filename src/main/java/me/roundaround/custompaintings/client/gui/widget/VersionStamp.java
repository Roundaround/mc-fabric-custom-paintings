package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.roundalib.client.gui.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

public class VersionStamp {
  private VersionStamp() {
  }

  public static LabelWidget create(TextRenderer textRenderer, ThreeSectionLayoutWidget layout) {
    return FabricLoader.getInstance().getModContainer(CustomPaintingsMod.MOD_ID).map((mod) -> {
      Text version = Text.of("v" + mod.getMetadata().getVersion().getFriendlyString());
      return layout.addNonPositioned(LabelWidget.builder(textRenderer, version)
          .hideBackground()
          .showShadow()
          .alignSelfRight()
          .alignSelfTop()
          .alignTextRight()
          .build(), (parent, self) -> self.setPosition(layout.getWidth() - GuiUtil.PADDING, GuiUtil.PADDING));
    }).orElse(null);
  }
}
