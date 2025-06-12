package me.roundaround.custompaintings.client.gui.screen.editor.painting;

import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.roundalib.observable.Subject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

    // TODO: i18n
    this.addNumField(Text.of("Block width"), this.state.blockWidth);
    // TODO: i18n
    this.addNumField(Text.of("Block height"), this.state.blockHeight);

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

  private void addNumField(
      Text label,
      Subject<Integer> obs) {
    LinearLayoutWidget row = LinearLayoutWidget.horizontal().spacing(GuiUtil.PADDING / 2);

    row.add(
        LabelWidget.builder(this.textRenderer, label)
            .hideBackground()
            .showShadow()
            .build(),
        (parent, self) -> {
          self.setWidth(parent.getUnusedSpace(self));
        });

    TextFieldWidget field = row.add(
        new TextFieldWidget(this.textRenderer, this.textRenderer.getWidth("9999") + 10, 20, label) {
          @Override
          public boolean charTyped(char chr, int keyCode) {
            if (!Character.isDigit(chr)) {
              return false;
            }
            return super.charTyped(chr, keyCode);
          }
        });
    field.setMaxLength(4);
    field.setText(obs.get().toString());
    field.setChangedListener((text) -> {
      if (text == null || text == "") {
        field.setEditableColor(GuiUtil.ERROR_COLOR);
        return;
      }

      int value;
      try {
        value = Integer.parseInt(text);
      } catch (Exception e) {
        field.setEditableColor(GuiUtil.ERROR_COLOR);
        return;
      }

      if (value > 0 && value <= 8) {
        obs.set(value);
        field.setEditableColor(GuiUtil.LABEL_COLOR);
      } else {
        field.setEditableColor(GuiUtil.ERROR_COLOR);
      }
    });
    obs.subscribe((i) -> {
      String text = field.getText();
      String value = i.toString();
      if (!text.equals(value)) {
        field.setText(value);
        field.setCursorToEnd(false);
        field.setSelectionStart(text.length());
        field.setSelectionEnd(text.length());
      }
    });

    // TODO: Step buttons

    this.layout.add(row, (parent, self) -> {
      self.setWidth(parent.getInnerWidth());
    });
  }
}
