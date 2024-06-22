package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Objects;

public abstract class PaintingEditScreen extends Screen {
  protected final PaintingEditState state;

  protected PaintingEditScreen(Text title, PaintingEditState state) {
    super(title);
    this.state = state;
  }

  public PaintingEditState getState() {
    return this.state;
  }

  protected void saveEmpty() {
    this.saveSelection(PaintingData.EMPTY);
  }

  public void saveSelection(PaintingData paintingData) {
    ClientNetworking.sendSetPaintingPacket(this.state.getPaintingUuid(), paintingData);
    Objects.requireNonNull(this.client).setScreen(null);
  }
}
