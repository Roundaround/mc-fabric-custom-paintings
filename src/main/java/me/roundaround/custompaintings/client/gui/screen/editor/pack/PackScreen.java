package me.roundaround.custompaintings.client.gui.screen.editor.pack;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.client.gui.screen.editor.PackData;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PackScreen extends BaseScreen {
  private static final Identifier TAB_HEADER_BACKGROUND_TEXTURE = Identifier
      .ofVanilla("textures/gui/tab_header_background.png");

  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final TabManager tabManager = new TabManager(
      (element) -> this.addDrawableChild(element),
      (child) -> this.remove(child));

  private TabNavigationWidget tabNavigation;
  private State state;

  public PackScreen(
      @NotNull ScreenParent parent,
      @NotNull MinecraftClient client,
      @NotNull PackData pack) {
    this(parent, client, new State(pack));
  }

  public PackScreen(
      @NotNull ScreenParent parent,
      @NotNull MinecraftClient client,
      @NotNull State state) {
    super(Text.translatable("custompaintings.editor.editor.title"), parent, client);
    this.state = state;
  }

  @Override
  protected void init() {
    this.tabNavigation = TabNavigationWidget.builder(this.tabManager, this.width)
        .tabs(
            new MetadataTab(this.client, this.state, this),
            new PaintingsTab(this.client, this.state, this),
            new MigrationsTab(this.client, this.state, this))
        .build();
    this.addDrawableChild(this.tabNavigation);

    ButtonWidget doneButton = this.layout.addFooter(ButtonWidget.builder(
        this.getDoneButtonMessage(this.state.dirty.get()),
        (b) -> this.close())
        .width(ButtonWidget.field_49479)
        .build());
    this.state.dirty.subscribe((dirty) -> doneButton.setMessage(this.getDoneButtonMessage(dirty)));

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild((child) -> {
      child.setNavigationOrder(1);
      this.addDrawableChild(child);
    });
    this.tabNavigation.selectTab(0, false);
    this.refreshWidgetPositions();
  }

  @Override
  protected void refreshWidgetPositions() {
    if (this.tabNavigation == null) {
      return;
    }

    this.tabNavigation.setWidth(this.width);
    this.tabNavigation.init();

    int headerFooterHeight = this.tabNavigation.getNavigationFocus().getBottom();
    this.layout.setHeaderHeight(headerFooterHeight);

    this.layout.refreshPositions();
    ScreenRect tabArea = new ScreenRect(
        0,
        headerFooterHeight,
        this.width,
        this.height - this.layout.getFooterHeight() - headerFooterHeight);
    this.tabManager.setTabArea(tabArea);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    super.render(context, mouseX, mouseY, deltaTicks);
    context.drawTexture(
        RenderPipelines.GUI_TEXTURED, FOOTER_SEPARATOR_TEXTURE, 0,
        this.height - this.layout.getFooterHeight(), 0, 0, this.width, 2, 32, 2);
  }

  @Override
  protected void renderDarkening(DrawContext context) {
    context.drawTexture(
        RenderPipelines.GUI_TEXTURED,
        TAB_HEADER_BACKGROUND_TEXTURE,
        0, 0, 0, 0,
        this.width,
        this.layout.getHeaderHeight(),
        16, 16);
    this.renderDarkening(context, 0, this.layout.getHeaderHeight(), this.width, this.height);
  }

  @Override
  public void close() {
    this.state.close();
    super.close();
  }

  public ScreenParent getParent() {
    return this.parent;
  }

  private Text getDoneButtonMessage(boolean dirty) {
    MutableText text = ScreenTexts.DONE.copy();
    if (dirty) {
      text.append(" *");
    }
    return text;
  }
}
