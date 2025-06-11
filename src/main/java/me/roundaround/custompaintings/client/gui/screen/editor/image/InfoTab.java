package me.roundaround.custompaintings.client.gui.screen.editor.image;

import java.util.function.Consumer;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class InfoTab extends ImageTab {
  private final LabelWidget widthLabel;
  private final LabelWidget heightLabel;

  public InfoTab(
      MinecraftClient client,
      Image image,
      Consumer<Image> modifyImage) {
    // TODO: i18n
    super(client, Text.of("Info"));

    this.widthLabel = this.layout.add(
        LabelWidget.builder(this.textRenderer, this.getWidthText(image))
            .hideBackground()
            .showShadow()
            .build());
    this.heightLabel = this.layout.add(
        LabelWidget.builder(this.textRenderer, this.getHeightText(image))
            .hideBackground()
            .showShadow()
            .build());
  }

  public void onImageChange(Image image) {
    this.widthLabel.setText(this.getWidthText(image));
    this.heightLabel.setText(this.getHeightText(image));
  }

  private Text getWidthText(Image image) {
    // TODO: i18n
    return Text.of("Width: " + image.width() + "px");
  }

  private Text getHeightText(Image image) {
    // TODO: i18n
    return Text.of("Height: " + image.height() + "px");
  }
}
