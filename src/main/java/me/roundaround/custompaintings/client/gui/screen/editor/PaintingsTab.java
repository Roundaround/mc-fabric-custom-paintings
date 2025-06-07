package me.roundaround.custompaintings.client.gui.screen.editor;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class PaintingsTab extends PackEditorTab {
  public PaintingsTab(@NotNull MinecraftClient client, @NotNull State state) {
    super(client, state, Text.translatable("custompaintings.editor.editor.tab.paintings.title"));

    this.layout.add(
        LabelWidget.builder(this.client.textRenderer, Text.of("Paintings"))
            .hideBackground()
            .showShadow()
            .build(),
        (parent, self) -> self.setWidth(this.getContentWidth()));
  }
}
