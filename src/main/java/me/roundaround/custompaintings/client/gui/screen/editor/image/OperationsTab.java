package me.roundaround.custompaintings.client.gui.screen.editor.image;

import java.util.List;
import java.util.function.Consumer;

import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.roundalib.client.gui.icon.BuiltinIcon;
import me.roundaround.custompaintings.roundalib.client.gui.layout.linear.LinearLayoutWidget;
import me.roundaround.custompaintings.roundalib.client.gui.util.GuiUtil;
import me.roundaround.custompaintings.roundalib.client.gui.widget.FlowListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.IconButtonWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.ParentElementEntryListWidget;
import me.roundaround.custompaintings.roundalib.client.gui.widget.drawable.LabelWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class OperationsTab extends ImageTab {
  private final List<Image.Operation> operations;
  private final Image originalImage;
  private final Consumer<Image> modifyImage;
  private final OperationList operationList;

  public OperationsTab(
      MinecraftClient client,
      List<Image.Operation> operations,
      Image originalImage,
      Consumer<Image> modifyImage) {
    // TODO: i18n
    super(client, Text.of("Operations"));

    this.operations = operations;
    this.originalImage = originalImage;
    this.modifyImage = modifyImage;

    this.layout.add(
        ButtonWidget.builder(
            Text.of("Invert"),
            (b) -> this.addOperation(Image.Operation.invert()))
            .build(),
        (parent, self) -> {
          self.setWidth(Math.min(parent.getInnerWidth(), ButtonWidget.DEFAULT_WIDTH_SMALL));
        });

    this.operationList = this.layout.add(
        new OperationList(
            this.client,
            this.layout.getInnerWidth(),
            this.layout.getUnusedSpace(null),
            this::removeOperation,
            operations),
        (parent, self) -> {
          self.setDimensions(parent.getInnerWidth(), parent.getUnusedSpace(self));
        });
  }

  private void addOperation(Image.Operation operation) {
    this.operations.add(operation);
    this.operationList.addOperation(operation);
    this.modifyImage.accept(this.originalImage.apply(this.operations));
  }

  private void removeOperation(int index) {
    if (index < 0 || index >= this.operations.size()) {
      return;
    }
    this.operations.remove(index);
    this.operationList.removeAndReflow(this.operations);
    this.modifyImage.accept(this.originalImage.apply(this.operations));
  }

  private static class OperationList extends ParentElementEntryListWidget<OperationList.Entry> {
    private final Consumer<Integer> onDeletePress;

    public OperationList(
        MinecraftClient client,
        int width,
        int height,
        Consumer<Integer> onDeletePress,
        List<Image.Operation> operations) {
      super(client, 0, 0, width, height);

      this.onDeletePress = onDeletePress;
      for (int i = 0; i < operations.size(); i++) {
        this.addEntry(Entry.factory(this.client.textRenderer, this.onDeletePress, operations.get(i)));
      }
    }

    public void removeAndReflow(List<Image.Operation> operations) {
      this.removeEntry();
      // TODO: Do I need better checks here?
      for (int i = 0; i < Math.min(operations.size(), this.getEntryCount()); i++) {
        this.getEntry(i).setOperation(operations.get(i));
      }
    }

    public void addOperation(Image.Operation operation) {
      this.addEntry(Entry.factory(this.client.textRenderer, this.onDeletePress, operation));
    }

    static class Entry extends ParentElementEntryListWidget.Entry {
      private final LinearLayoutWidget layout;
      private final LabelWidget label;

      public Entry(
          TextRenderer textRenderer,
          int index,
          int left,
          int top,
          int width,
          Consumer<Integer> onDeletePress,
          Image.Operation operation) {
        super(index, left, top, width, 20);
        this.layout = LinearLayoutWidget.horizontal()
            .spacing(GuiUtil.PADDING)
            .defaultOffAxisContentAlignCenter()
            .mainAxisContentAlignCenter();

        this.label = this.layout.add(
            LabelWidget.builder(textRenderer, operation.getName())
                .hideBackground()
                .showShadow()
                .build(),
            (parent, self) -> {
              self.setWidth(parent.getUnusedSpace(self));
            });

        // TODO: i18n
        this.layout.add(IconButtonWidget.builder(BuiltinIcon.CANCEL_13, Constants.MOD_ID)
            .medium()
            .messageAndTooltip(Text.of("Delete"))
            .onPress((button) -> onDeletePress.accept(this.index))
            .build());

        this.addLayout(this.layout, (self) -> {
          self.setPositionAndDimensions(
              this.getContentLeft(),
              this.getContentTop(),
              this.getContentWidth(),
              this.getContentHeight());
        });
        this.layout.forEachChild(this::addDrawableChild);
      }

      public void setOperation(Image.Operation operation) {
        this.label.setText(operation.getName());
      }

      public static FlowListWidget.EntryFactory<Entry> factory(
          TextRenderer textRenderer,
          Consumer<Integer> onDeletePress,
          Image.Operation operation) {
        return (index, left, top, width) -> new Entry(textRenderer, index, left, top, width, onDeletePress, operation);
      }
    }
  }
}
