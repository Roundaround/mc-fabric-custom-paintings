package me.roundaround.custompaintings.client.gui.screen.manage;

import java.util.HashMap;

import org.lwjgl.glfw.GLFW;

import me.roundaround.custompaintings.client.gui.widget.KnownPaintingListWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ReassignScreen extends Screen implements KnownPaintingsTracker {
  private final Screen parent;
  private final Identifier id;

  private KnownPaintingListWidget list;

  public ReassignScreen(Screen parent, Identifier id) {
    super(Text.translatable("custompaintings.reassign.title"));
    this.parent = parent;
    this.id = id;
  }

  public Identifier getCurrentId() {
    return id;
  }

  @Override
  public void init() {
    this.list = new KnownPaintingListWidget(
        this,
        this.client,
        this.width,
        this.height,
        32,
        this.height - 32);
    addSelectableChild(this.list);
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      this.client.setScreen(this.parent);
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void onKnownPaintingsChanged(HashMap<Identifier, PaintingData> knownPaintings) {
    if (this.list != null) {
      this.list.setPaintings(knownPaintings.values());
    }
  }
}
