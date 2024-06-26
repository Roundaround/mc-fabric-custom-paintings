package me.roundaround.custompaintings.client.gui.screen.manage;

import me.roundaround.custompaintings.client.gui.widget.UnknownPaintingListWidget;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.util.UnknownPainting;
import me.roundaround.roundalib.client.gui.GuiUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.HashSet;

public class UnknownPaintingsScreen extends Screen {
  private final ManagePaintingsScreen parent;

  private UnknownPaintingListWidget list;
  private ButtonWidget reassignButton;
  private ButtonWidget removeButton;
  private UnknownPainting selected = null;

  public UnknownPaintingsScreen(ManagePaintingsScreen parent) {
    super(Text.translatable("custompaintings.unknown.title"));
    this.parent = parent;
  }

  public void setUnknownPaintings(HashSet<UnknownPainting> unknownPaintings) {
    if (this.list != null) {
      this.list.receiveData(unknownPaintings);
    }
  }

  public void setSelected(UnknownPainting selected) {
    this.selected = selected;
    if (this.reassignButton != null) {
      this.reassignButton.active = selected != null;
    }
    if (this.removeButton != null) {
      this.removeButton.active = selected != null;
    }
  }

  public void reassignSelection() {
    if (this.selected == null) {
      return;
    }

    int count = this.list.getCountForId(this.selected.currentData().id());
    if (count > 1) {
      this.client.setScreen(new ApplyToAllScreen(this, count, this.selected, (selected, choice) -> {
        this.client.setScreen(new ReassignScreen(this, this.selected, choice));
      }));
      return;
    }

    this.client.setScreen(new ReassignScreen(this, this.selected, false));
  }

  public void removeSelection() {
    if (this.selected == null) {
      return;
    }

    int count = this.list.getCountForId(this.selected.currentData().id());
    if (count > 1) {
      this.client.setScreen(new ApplyToAllScreen(this, count, this.selected, (selected, choice) -> {
        if (choice) {
          ClientNetworking.sendRemoveAllPaintingsPacket(this.selected.currentData().id());
        } else {
          ClientNetworking.sendRemovePaintingPacket(this.selected.uuid());
        }
      }));
      return;
    }

    ClientNetworking.sendRemovePaintingPacket(this.selected.uuid());
  }

  @Override
  public void init() {
    this.list = new UnknownPaintingListWidget(this, this.client, this.width, this.height - 33 - 33, 33);
    addSelectableChild(this.list);

    this.reassignButton = ButtonWidget.builder(Text.translatable("custompaintings.unknown.reassign"), (button) -> {
          reassignSelection();
        })
        .position((this.width - 120) / 2 - 120 - GuiUtil.PADDING, this.height - 20 - GuiUtil.PADDING)
        .size(120, 20)
        .build();
    this.reassignButton.active = false;
    addDrawableChild(this.reassignButton);

    this.removeButton = ButtonWidget.builder(Text.translatable("custompaintings.unknown.remove"), (button) -> {
      removeSelection();
    }).position((this.width - 120) / 2, this.height - 20 - GuiUtil.PADDING).size(120, 20).build();
    this.removeButton.active = false;
    addDrawableChild(this.removeButton);

    addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
      this.close();
    }).position((this.width + 120) / 2 + GuiUtil.PADDING, this.height - 20 - GuiUtil.PADDING).size(120, 20).build());
  }

  @Override
  public void close() {
    this.client.setScreen(this.parent);
  }
}
