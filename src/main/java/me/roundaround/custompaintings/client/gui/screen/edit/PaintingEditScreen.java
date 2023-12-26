package me.roundaround.custompaintings.client.gui.screen.edit;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.text.Text;

public abstract class PaintingEditScreen extends BaseScreen {
  protected final PaintingEditState state;

  protected PaintingEditScreen(Text title, PaintingEditState state) {
    super(title);
    this.state = state;
  }

  public PaintingEditState getState() {
    return this.state;
  }

  protected void saveEmpty() {
    saveSelection(PaintingData.EMPTY);
  }

  public void saveSelection(PaintingData paintingData) {
    ClientNetworking.sendSetPaintingPacket(this.state.getPaintingUuid(), paintingData);
    close();
  }
}
