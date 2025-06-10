package me.roundaround.custompaintings.client.gui.screen.editor;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ImageScreen extends BaseScreen {
  private Image image;

  public ImageScreen(
    @NotNull Text title,
    @NotNull ScreenParent parent,
    @NotNull MinecraftClient client,
    Image image) {
    super(title, parent, client);
    this.image = image;
  }
  
  
}
