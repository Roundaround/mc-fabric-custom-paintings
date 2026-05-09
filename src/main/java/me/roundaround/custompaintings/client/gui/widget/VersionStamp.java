package me.roundaround.custompaintings.client.gui.widget;

import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.roundalib.client.gui.util.GuiUtil;
import me.roundaround.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.roundalib.client.gui.widget.drawable.LabelWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public class VersionStamp {
  private VersionStamp() {
  }

  public static LabelWidget create(Font textRenderer, ThreeSectionLayoutWidget layout) {
    return FabricLoader.getInstance().getModContainer(Constants.MOD_ID).map((mod) -> {
      Component version = Component.nullToEmpty("v" + mod.getMetadata().getVersion().getFriendlyString());
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
