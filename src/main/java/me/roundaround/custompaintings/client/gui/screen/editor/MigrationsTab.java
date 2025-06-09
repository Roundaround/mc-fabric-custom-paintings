package me.roundaround.custompaintings.client.gui.screen.editor;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class MigrationsTab extends PackEditorTab {
  public MigrationsTab(
      @NotNull MinecraftClient client,
      @NotNull State state,
      @NotNull EditorScreen screen) {
    super(client,
        state,
        screen,
        Text.translatable("custompaintings.editor.editor.migrations.title"));

    this.layout.add(
        LabelWidget.builder(this.client.textRenderer, Text.of("Migrations"))
            .hideBackground()
            .showShadow()
            .build(),
        (parent, self) -> self.setWidth(this.getContentWidth()));
  }
}
