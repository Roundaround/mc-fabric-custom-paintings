package me.roundaround.custompaintings.client.gui.screen;

import java.util.UUID;
import java.util.function.Function;

import me.roundaround.custompaintings.client.gui.PaintingEditState;
import me.roundaround.custompaintings.client.gui.PaintingEditState.Group;
import me.roundaround.custompaintings.client.gui.screen.page.FiltersPage;
import me.roundaround.custompaintings.client.gui.screen.page.GroupSelectPage;
import me.roundaround.custompaintings.client.gui.screen.page.PaintingEditScreenPage;
import me.roundaround.custompaintings.client.gui.screen.page.PaintingSelectPage;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PaintingEditScreen extends Screen {
  private final PaintingEditState state;
  private boolean pagesInitialized = false;
  private GroupSelectPage groupSelectPage;
  private PaintingSelectPage paintingSelectPage;
  private FiltersPage filtersPage;
  private PaintingEditScreenPage currentPage;
  private PaintingEditScreenPage nextPage;
  private Runnable onPageSwitch = null;

  public PaintingEditScreen(UUID paintingUuid, int paintingId, BlockPos blockPos, Direction facing) {
    super(Text.translatable("custompaintings.painting.title"));
    this.state = new PaintingEditState(
        this.client,
        paintingUuid,
        paintingId,
        blockPos,
        facing,
        this::onFilterChanged);
  }

  public PaintingEditState getState() {
    return this.state;
  }

  @Override
  public <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
    return super.addDrawableChild(drawableElement);
  }

  @Override
  public <T extends Drawable> T addDrawable(T drawable) {
    return super.addDrawable(drawable);
  }

  @Override
  public <T extends Element & Selectable> T addSelectableChild(T child) {
    return super.addSelectableChild(child);
  }

  @Override
  public void init() {
    this.state.populatePaintings();

    if (this.state.hasNoPaintings()) {
      saveEmpty();
    }

    initPages();

    if (!this.state.hasMultipleGroups() && this.state.getCurrentGroup() == null) {
      this.state.selectFirstGroup();
      setPageImmediate(this.paintingSelectPage);
    }

    this.currentPage.init();
  }

  @Override
  public void resize(MinecraftClient client, int width, int height) {
    this.pagesInitialized = false;
    super.resize(client, width, height);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (this.currentPage.preKeyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }

    if (super.keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }

    return this.currentPage.postKeyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char chr, int keyCode) {
    return this.currentPage.charTyped(chr, keyCode);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    return this.currentPage.mouseScrolled(mouseX, mouseY, amount);
  }

  @Override
  public void tick() {
    this.currentPage.tick();
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    checkAndAdvanceState();

    this.currentPage.renderBackground(matrixStack, mouseX, mouseY, partialTicks);

    matrixStack.push();
    matrixStack.translate(0, 0, 12);
    this.currentPage.renderForeground(matrixStack, mouseX, mouseY, partialTicks);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    matrixStack.pop();
  }

  public void saveEmpty() {
    saveSelection(PaintingData.EMPTY);
  }

  public void saveCurrentSelection() {
    Group currentGroup = this.state.getCurrentGroup();
    PaintingData currentPainting = this.state.getCurrentPainting();
    if (currentGroup == null || currentPainting == null) {
      saveEmpty();
    }
    saveSelection(currentPainting);
  }

  public void saveSelection(PaintingData paintingData) {
    ClientNetworking.sendSetPaintingPacket(this.state.getPaintingUuid(), paintingData);
    close();
  }

  private void setPage(PaintingEditScreenPage page) {
    setPage(page, null);
  }

  private void setPage(PaintingEditScreenPage page, Runnable onSwitch) {
    nextPage = page;
    onPageSwitch = onSwitch;
  }

  private void checkAndAdvanceState() {
    if (currentPage != nextPage) {
      currentPage = nextPage;

      if (onPageSwitch != null) {
        onPageSwitch.run();
        onPageSwitch = null;
      }

      clearAndInit();
    }
  }

  private void setPageImmediate(PaintingEditScreenPage page) {
    currentPage = page;
    nextPage = page;
    onPageSwitch = null;
  }

  public void selectGroup(String id) {
    if (!this.state.hasGroup(id)) {
      return;
    }

    setPage(this.paintingSelectPage, () -> {
      this.state.setCurrentGroup(id);
    });
  }

  public void clearGroup() {
    setPage(this.groupSelectPage, () -> {
      this.state.clearGroup();
    });
  }

  public void openFiltersPage() {
    setPage(this.filtersPage);
  }

  public void returnToPaintingSelect() {
    setPage(this.paintingSelectPage);
  }

  public void setCurrentPainting(PaintingData paintingData) {
    PaintingData currentPainting = this.state.getCurrentPainting();
    if (currentPainting != null && currentPainting.equals(paintingData)) {
      return;
    }

    this.state.setCurrentPainting(paintingData);
    clearAndInit();
  }

  public void setCurrentPainting(Function<PaintingData, PaintingData> mapper) {
    setCurrentPainting(mapper.apply(this.state.getCurrentPainting()));
  }

  private void initPages() {
    if (this.pagesInitialized) {
      return;
    }

    boolean currentlyOnPaintingSelectPage = this.currentPage instanceof PaintingSelectPage;

    this.pagesInitialized = true;
    this.groupSelectPage = new GroupSelectPage(
        this,
        this.client,
        this.width,
        this.height);
    this.paintingSelectPage = new PaintingSelectPage(
        this,
        this.client,
        this.width,
        this.height);
    this.filtersPage = new FiltersPage(
        this,
        this.client,
        this.width,
        this.height);

    this.currentPage = currentlyOnPaintingSelectPage
        ? this.paintingSelectPage
        : this.groupSelectPage;

    this.nextPage = this.currentPage;
  }

  private void onFilterChanged() {
    if (this.currentPage instanceof PaintingSelectPage) {
      ((PaintingSelectPage) this.currentPage).updateFilters();
    }
  }
}
