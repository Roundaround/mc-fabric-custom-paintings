package me.roundaround.custompaintings.client.gui.screen.set;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Objects;

public abstract class BaseSetPaintingScreen extends Screen {
  protected final PaintingEditState state;

  protected BaseSetPaintingScreen(Text title, PaintingEditState state) {
    super(title);
    this.state = state;
    this.state.updatePaintingList();
  }

  protected void navigate(Screen screen) {
    this.state.clearStateChangedListener();
    Objects.requireNonNull(this.client).setScreen(screen);
  }

  protected void saveEmpty() {
    this.saveSelection(PaintingData.EMPTY);
  }

  protected void saveSelection(PaintingData paintingData) {
    ClientNetworking.sendSetPaintingPacket(this.state.getPaintingId(), paintingData.id());
    Objects.requireNonNull(this.client).setScreen(null);
  }
}
