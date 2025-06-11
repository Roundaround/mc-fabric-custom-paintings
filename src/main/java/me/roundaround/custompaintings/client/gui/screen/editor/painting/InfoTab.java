package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class InfoTab extends PaintingTab {
  private final LabelWidget widthLabel;
  private final LabelWidget heightLabel;

  public InfoTab(
      MinecraftClient client,
      State state) {
    // TODO: i18n
    super(client, state, Text.of("Info"));

    this.widthLabel = this.layout.add(
        LabelWidget.builder(this.textRenderer, this.getWidthText())
            .hideBackground()
            .showShadow()
            .build());
    this.heightLabel = this.layout.add(
        LabelWidget.builder(this.textRenderer, this.getHeightText())
            .hideBackground()
            .showShadow()
            .build());

    this.state.image.subscribe(() -> {
      this.widthLabel.setText(this.getWidthText());
      this.heightLabel.setText(this.getHeightText());
      this.layout.refreshPositions();
    });
  }

  private Text getWidthText() {
    // TODO: i18n
    return Text.of("Width: " + this.state.image.get().width() + "px");
  }

  private Text getHeightText() {
    // TODO: i18n
    return Text.of("Height: " + this.state.image.get().height() + "px");
  }
}
