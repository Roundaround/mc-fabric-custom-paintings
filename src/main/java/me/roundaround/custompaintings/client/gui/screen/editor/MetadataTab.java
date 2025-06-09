package me.roundaround.custompaintings.client.gui.screen.editor;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class MetadataTab extends PackEditorTab {
  private final int MIN_WIDTH = 240;

  public MetadataTab(@NotNull MinecraftClient client, @NotNull State state) {
    super(client, state, Text.translatable("custompaintings.editor.editor.metadata.title"));

    MetadataList list = this.layout.add(new MetadataList(
        this.client,
        this.getWidth(this.layout),
        this.layout.getHeight()),
        (parent, self) -> {
          self.setDimensionsAndPosition(
              this.getWidth(parent),
              parent.getHeight(),
              parent.getX(),
              parent.getY());
        });

    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.textRenderer,
        "id",
        this.state.id,
        this.state.idDirty,
        () -> this.state.getLastSaved().id()));
    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.textRenderer,
        "name",
        this.state.name,
        this.state.nameDirty,
        () -> this.state.getLastSaved().name()));
    list.addEntry(MetadataList.TextFieldEntry.factory(
        this.client.textRenderer,
        "description",
        this.state.description,
        this.state.descriptionDirty,
        () -> this.state.getLastSaved().description(),
        255));

    this.layout.refreshPositions();
  }

  private int getWidth(LinearLayoutWidget layout) {
    return Math.max(MIN_WIDTH, Math.round(layout.getWidth() * 0.8f));
  }
}
