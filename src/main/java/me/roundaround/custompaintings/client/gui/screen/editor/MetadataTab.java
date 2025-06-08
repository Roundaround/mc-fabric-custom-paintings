package me.roundaround.custompaintings.client.gui.screen.editor;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class MetadataTab extends PackEditorTab {
  public MetadataTab(@NotNull MinecraftClient client, @NotNull State state) {
    super(client, state, Text.translatable("custompaintings.editor.editor.metadata.title"));

    MetadataList list = this.layout.add(new MetadataList(
        this.client,
        this.layout),
        (parent, self) -> {
          self.setDimensionsAndPosition(
              parent.getWidth(),
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
}
