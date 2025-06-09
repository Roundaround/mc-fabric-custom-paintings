package me.roundaround.custompaintings.client.gui.screen.editor;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jetbrains.annotations.NotNull;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.gui.widget.VersionStamp;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.resource.file.Metadata;
import me.roundaround.custompaintings.resource.file.PackReader;
import me.roundaround.custompaintings.resource.file.Painting;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.layout.screen.ThreeSectionLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.screen.BaseScreen;
import me.roundaround.custompaintings.roundalib.client.gui.screen.ScreenParent;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import me.roundaround.custompaintings.util.CustomId;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ZeroScreen extends BaseScreen {
  private final ThreeSectionLayoutWidget layout = new ThreeSectionLayoutWidget(this);
  private final AtomicReference<JFileChooser> fileChooser = new AtomicReference<>(null);

  public ZeroScreen(
      @NotNull ScreenParent parent,
      @NotNull MinecraftClient client) {
    super(Text.translatable("custompaintings.editor.zero.title"), parent, client);
  }

  @Override
  protected void init() {
    this.layout.addHeader(this.textRenderer, this.title);

    ZeroList list = this.layout.addBody(new ZeroList(this.client, this.layout));

    list.addEntry(ZeroList.BodyEntry.factory(
        this.textRenderer,
        LabelWidget.builder(
            this.textRenderer,
            List.of(
                Text.translatable("custompaintings.editor.zero.body.1"),
                Text.translatable("custompaintings.editor.zero.body.2")))
            .hideBackground()
            .showShadow()
            .alignTextCenterX()
            .lineSpacing(GuiUtil.PADDING / 2)
            .build()));

    list.addEntry(ZeroList.ButtonEntry.factory(
        this.textRenderer,
        Text.translatable("custompaintings.editor.zero.new.label"),
        ButtonWidget.builder(
            Text.translatable("custompaintings.editor.zero.new.button"),
            this::newPack)
            .width(ButtonWidget.DEFAULT_WIDTH_SMALL).build()));

    list.addEntry(ZeroList.ButtonEntry.factory(
        this.textRenderer,
        Text.translatable("custompaintings.editor.zero.open.label"),
        ButtonWidget.builder(
            Text.translatable("custompaintings.editor.zero.open.button"),
            this::openPack)
            .width(ButtonWidget.DEFAULT_WIDTH_SMALL).build()));

    if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
      list.addEntry(ZeroList.ButtonEntry.factory(
          this.textRenderer,
          Text.of("Open Famous Paintings from assets"),
          ButtonWidget.builder(
              Text.translatable("custompaintings.editor.zero.open.button"),
              (button) -> {
                Path path = FabricLoader.getInstance()
                    .getGameDir()
                    .getParent()
                    .resolve("assets/FamousPaintings-1.0.0.zip");
                PackData packData = this.getPackData(path);
                if (packData == null) {
                  return;
                }
                this.navigateToEditor(packData);
              })
              .width(ButtonWidget.DEFAULT_WIDTH_SMALL).build()));
    }

    this.layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, this::done)
        .width(ButtonWidget.field_49479)
        .build());

    VersionStamp.create(this.textRenderer, this.layout);

    this.layout.forEachChild(this::addDrawableChild);
    this.refreshWidgetPositions();
  }

  @Override
  public void removed() {
    this.fileChooser.updateAndGet((fileChooser) -> {
      if (fileChooser != null) {
        fileChooser.cancelSelection();
        fileChooser.setVisible(false);
      }
      return null;
    });
    super.removed();
  }

  @Override
  public void onFilesDropped(List<Path> paths) {
    Path path = paths.isEmpty() ? null : paths.getFirst();
    if (path == null) {
      return;
    }

    PackData packData = this.getPackData(path);
    if (packData == null) {
      return;
    }

    this.navigateToEditor(packData);
  }

  private void navigateToEditor(PackData packData) {
    this.client.setScreen(new EditorScreen(new ScreenParent(this), this.client, packData));
  }

  private void newPack(ButtonWidget button) {
    this.navigateToEditor(new PackData());
  }

  private void openPack(ButtonWidget button) {
    EventQueue.invokeLater(() -> {
      JFileChooser fileChooser = this.fileChooser.updateAndGet((fc) -> {
        if (fc != null) {
          fc.cancelSelection();
          fc.setVisible(false);
        }

        fc = new JFileChooser();
        fc.setDialogTitle(Text.translatable("custompaintings.editor.open_file").getString());
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("Directories and zip files (*.zip)", "zip"));

        return fc;
      });

      int result = fileChooser.showOpenDialog(null);
      if (result != JFileChooser.APPROVE_OPTION) {
        return;
      }

      PackData packData = this.getPackData(Paths.get(fileChooser.getSelectedFile().getAbsolutePath()));
      if (packData == null) {
        return;
      }

      this.client.execute(() -> {
        if (this.client.currentScreen == this) {
          this.navigateToEditor(packData);
        }
      });
    });
  }

  private boolean isDirectory(Path path) {
    return Files.isDirectory(path);
  }

  private boolean isZipFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip");
  }

  private PackData getPackData(Path path) {
    if (!this.isDirectory(path) && !this.isZipFile(path)) {
      return null;
    }

    try {
      Metadata meta = PackReader.readMetadata(path);
      HashMap<CustomId, Image> images = PackReader.readPaintingImages(meta);

      List<PackData.Painting> paintings = new ArrayList<>();
      for (Painting painting : meta.pack().paintings()) {
        CustomId id = new CustomId(meta.pack().id(), painting.id());
        Image image = images.remove(id);

        paintings.add(new PackData.Painting(
            painting.id(),
            painting.name(),
            painting.artist(),
            painting.width(),
            painting.height(),
            image));
      }

      for (Map.Entry<CustomId, Image> entry : images.entrySet()) {
        CustomId id = entry.getKey();
        Image image = entry.getValue();

        int pixelWidth = image.width();
        int pixelHeight = image.height();

        paintings.add(new PackData.Painting(
            id.resource(),
            id.resource(),
            "",
            pixelWidth,
            pixelHeight,
            image));
      }

      return new PackData(
          meta.pack().id(),
          meta.pack().name(),
          meta.pack().description(),
          meta.icon(),
          paintings);
    } catch (Exception e) {
      CustomPaintingsMod.LOGGER.warn("Failed to read metadata for {}", path, e);
      return null;
    }
  }

  @Override
  protected void refreshWidgetPositions() {
    this.layout.refreshPositions();
  }

  private static class ZeroList extends ParentElementEntryListWidget<ZeroList.Entry> {
    public ZeroList(
        @NotNull MinecraftClient client,
        @NotNull ThreeSectionLayoutWidget layout) {
      super(client, layout);
    }

    private static class Entry extends ParentElementEntryListWidget.Entry {
      protected final TextRenderer textRenderer;

      public Entry(TextRenderer textRenderer, int index, int x, int y, int width, int contentHeight) {
        super(index, x, y, width, contentHeight);
        this.textRenderer = textRenderer;
      }
    }

    private static class BodyEntry extends Entry {
      public BodyEntry(TextRenderer textRenderer, int index, int x, int y, int width, LabelWidget widget) {
        super(textRenderer, index, x, y, width, widget.getHeight() + 4 * GuiUtil.PADDING);

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal()
                .mainAxisContentAlignCenter()
                .defaultOffAxisContentAlignCenter(),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(),
                  this.getContentTop(),
                  this.getContentWidth(),
                  this.getContentHeight());
            });

        layout.add(widget);

        layout.forEachChild(this::addDrawableChild);
      }

      public static FlowListWidget.EntryFactory<BodyEntry> factory(
          TextRenderer textRenderer,
          LabelWidget widget) {
        return (index, left, top, width) -> new BodyEntry(
            textRenderer,
            index,
            left,
            top,
            width,
            widget);
      }
    }

    private static class ButtonEntry extends Entry {
      public ButtonEntry(TextRenderer textRenderer, int index, int x, int y, int width, Text text,
          ButtonWidget widget) {
        super(textRenderer, index, x, y, width, ButtonWidget.DEFAULT_HEIGHT);

        LinearLayoutWidget layout = this.addLayout(
            LinearLayoutWidget.horizontal()
                .spacing(GuiUtil.PADDING)
                .defaultOffAxisContentAlignCenter(),
            (self) -> {
              self.setPositionAndDimensions(
                  this.getContentLeft(),
                  this.getContentTop(),
                  this.getContentWidth(),
                  this.getContentHeight());
            });

        layout.add(LabelWidget.builder(this.textRenderer, text)
            .alignTextLeft()
            .alignTextCenterY()
            .overflowBehavior(LabelWidget.OverflowBehavior.SCROLL)
            .hideBackground()
            .showShadow()
            .build(), (parent, self) -> {
              self.setDimensions(this.getLabelWidth(parent, widget), this.getContentHeight());
            });

        layout.add(widget);

        layout.forEachChild(this::addDrawableChild);
      }

      private int getLabelWidth(LinearLayoutWidget layout, ButtonWidget widget) {
        return layout.getWidth() - 2 * layout.getSpacing() - widget.getWidth();
      }

      public static FlowListWidget.EntryFactory<ButtonEntry> factory(
          TextRenderer textRenderer,
          Text text,
          ButtonWidget widget) {
        return (index, left, top, width) -> new ButtonEntry(
            textRenderer,
            index,
            left,
            top,
            width,
            text,
            widget);
      }
    }
  }
}
