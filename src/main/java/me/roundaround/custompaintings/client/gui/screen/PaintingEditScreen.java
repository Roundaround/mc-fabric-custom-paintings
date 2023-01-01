package me.roundaround.custompaintings.client.gui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import me.roundaround.custompaintings.client.CustomPaintingManager;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.widget.FilterButtonWidget;
import me.roundaround.custompaintings.client.gui.widget.GroupsListWidget;
import me.roundaround.custompaintings.client.gui.widget.PaintingButtonWidget;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.PaintingVariantTags;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class PaintingEditScreen extends Screen {
  private final HashMap<String, Group> allPaintings = new HashMap<>();
  private final UUID paintingUuid;
  private final int paintingId;
  private final BlockPos blockPos;
  private final Direction facing;
  private Group currentGroup = null;
  private State currentState = State.GROUP_SELECT;
  private State nextState = State.GROUP_SELECT;
  private Action onStateSwitch = null;
  private int currentPainting = 0;
  private GroupsListWidget groupsListWidget;
  private int selectedIndex = 0;

  private static final Predicate<Entity> DECORATION_PREDICATE = (
      entity) -> entity instanceof AbstractDecorationEntity;
  private static final int BUTTON_WIDTH = 100;
  private static final int BUTTON_HEIGHT = 20;

  public PaintingEditScreen(UUID paintingUuid, int paintingId, BlockPos blockPos, Direction facing) {
    super(Text.translatable("custompaintings.painting.title"));
    this.paintingUuid = paintingUuid;
    this.paintingId = paintingId;
    this.blockPos = blockPos;
    this.facing = facing;
  }

  @Override
  public void init() {
    if (allPaintings.isEmpty()) {
      refreshPaintings();
    }

    if (allPaintings.isEmpty()) {
      saveEmpty();
    }

    if (!hasMultipleGroups()) {
      currentGroup = allPaintings.values().stream().findFirst().get();
      setStateImmediate(State.PAINTING_SELECT);
    }

    switch (currentState) {
      case PAINTING_SELECT:
        initForPaintingSelection();
        break;
      case GROUP_SELECT:
      default:
        initForGroupSelection();
    }

    if (selectedIndex >= 0 && selectedIndex < children().size()) {
      setInitialFocus(children().get(selectedIndex));
    }
  }

  private void initForGroupSelection() {
    groupsListWidget = new GroupsListWidget(
        this,
        client,
        width,
        height,
        getHeaderHeight(null),
        height - getFooterHeight());
    groupsListWidget.setGroups(allPaintings.values());
    addSelectableChild(groupsListWidget);

    addDrawableChild(
        new ButtonWidget(
            (width - BUTTON_WIDTH) / 2,
            height - BUTTON_HEIGHT - 10,
            BUTTON_WIDTH,
            BUTTON_HEIGHT,
            ScreenTexts.CANCEL,
            button -> {
              saveEmpty();
            }));
  }

  private void initForPaintingSelection() {
    PaintingData paintingData = currentGroup.paintings().get(currentPainting);
    boolean canStay = canStay(paintingData);

    int headerHeight = getHeaderHeight(paintingData);
    int footerHeight = getFooterHeight();

    int maxWidth = width / 2;
    int maxHeight = height - headerHeight - footerHeight - 20;

    int scaledWidth = PaintingButtonWidget.getScaledWidth(paintingData, maxWidth, maxHeight);
    int scaledHeight = PaintingButtonWidget.getScaledHeight(paintingData, maxWidth, maxHeight);

    PaintingButtonWidget paintingButton = new PaintingButtonWidget(
        (width - scaledWidth) / 2,
        (height + headerHeight - footerHeight - scaledHeight) / 2,
        maxWidth,
        maxHeight,
        (button) -> {
          saveSelection(paintingData);
        },
        paintingData);

    if (!canStay) {
      paintingButton.active = false;
    }

    ButtonWidget prevButton = new ButtonWidget(
        width / 2 - BUTTON_WIDTH - 2,
        height - 2 * BUTTON_HEIGHT - 10 - 4,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.painting.previous"),
        (button) -> {
          previousPainting();
        });

    ButtonWidget nextButton = new ButtonWidget(
        width / 2 + 2,
        height - 2 * BUTTON_HEIGHT - 10 - 4,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        Text.translatable("custompaintings.painting.next"),
        (button) -> {
          nextPainting();
        });

    if (!hasMultiplePaintings()) {
      prevButton.active = false;
      nextButton.active = false;
    }

    ButtonWidget cancelButton = new ButtonWidget(
        width / 2 - BUTTON_WIDTH - 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.CANCEL,
        (button) -> {
          if (hasMultipleGroups()) {
            clearGroup();
          } else {
            saveEmpty();
          }
        });

    ButtonWidget doneButton = new ButtonWidget(
        width / 2 + 2,
        height - BUTTON_HEIGHT - 10,
        BUTTON_WIDTH,
        BUTTON_HEIGHT,
        ScreenTexts.DONE,
        (button) -> {
          saveCurrentSelection();
        });

    if (!canStay) {
      doneButton.active = false;
    }

    FilterButtonWidget filterButton = new FilterButtonWidget(
        this.width - FilterButtonWidget.WIDTH - 10,
        this.height - FilterButtonWidget.HEIGHT - 10,
        this);

    addDrawableChild(paintingButton);
    addDrawableChild(prevButton);
    addDrawableChild(nextButton);
    addDrawableChild(cancelButton);
    addDrawableChild(doneButton);
    addDrawableChild(filterButton);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    switch (currentState) {
      case GROUP_SELECT:
        if (keyPressedForGroupSelect(keyCode, scanCode, modifiers)) {
          return true;
        }
      case PAINTING_SELECT:
        if (keyPressedForPaintingSelect(keyCode, scanCode, modifiers)) {
          return true;
        }
    }

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  private boolean keyPressedForGroupSelect(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE:
        saveEmpty();
        return true;
    }

    return false;
  }

  private boolean keyPressedForPaintingSelect(int keyCode, int scanCode, int modifiers) {
    switch (keyCode) {
      case GLFW.GLFW_KEY_LEFT:
        if (hasMultiplePaintings()) {
          client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
          previousPainting();
          return true;
        }
        break;
      case GLFW.GLFW_KEY_RIGHT:
        if (hasMultiplePaintings()) {
          client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1f));
          nextPainting();
          return true;
        }
        break;
      case GLFW.GLFW_KEY_ESCAPE:
        clearGroup();
        return true;
    }

    return false;
  }

  @Override
  public boolean changeFocus(boolean lookForwards) {
    return super.changeFocus(lookForwards);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    checkAndAdvanceState();

    switch (currentState) {
      case GROUP_SELECT:
        renderBackgroundForGroupSelect(matrixStack, mouseX, mouseY, partialTicks);
        break;
      case PAINTING_SELECT:
        renderBackgroundForPaintingSelect(matrixStack, mouseX, mouseY, partialTicks);
        break;
    }

    matrixStack.push();
    matrixStack.translate(0, 0, 12);
    renderTexts(matrixStack, mouseX, mouseY, partialTicks);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    matrixStack.pop();
  }

  private void renderBackgroundForGroupSelect(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    matrixStack.push();
    matrixStack.translate(0, 0, 10);
    groupsListWidget.render(matrixStack, mouseX, mouseY, partialTicks);
    matrixStack.pop();

    matrixStack.push();
    matrixStack.translate(0, 0, 11);
    renderBackgroundInRegion(0, getHeaderHeight(null), 0, width);
    renderBackgroundInRegion(height - getFooterHeight(), height, 0, width);
    matrixStack.pop();
  }

  private void renderBackgroundForPaintingSelect(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackgroundInRegion(0, height, 0, width);
  }

  private int getHeaderHeight(PaintingData paintingData) {
    switch (currentState) {
      case GROUP_SELECT:
        return 10 + textRenderer.fontHeight + 2 + 10;
      case PAINTING_SELECT:
        // 10 pixel padding, 4 lines of text, 2 pixel padding between lines, 10 pixel
        // padding
        int height = 10 + 4 * textRenderer.fontHeight + 2 * 3 + 10;
        if (paintingData.hasName() || paintingData.hasArtist()) {
          height += textRenderer.fontHeight + 2;
        }
        return height;
    }

    return 0;
  }

  private int getFooterHeight() {
    switch (currentState) {
      case GROUP_SELECT:
        return 10 + BUTTON_HEIGHT + 10;
      case PAINTING_SELECT:
        return 10 + 2 * BUTTON_HEIGHT + 4 + 10;
    }

    return 0;
  }

  private void renderBackgroundInRegion(int top, int bottom, int left, int right) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferBuilder = tessellator.getBuffer();
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND_TEXTURE);
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

    bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    bufferBuilder
        .vertex(left, bottom, 0)
        .texture(left / 32f, bottom / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder
        .vertex(right, bottom, 0)
        .texture(right / 32f, bottom / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder
        .vertex(right, top, 0)
        .texture(right / 32f, top / 32f)
        .color(64, 64, 64, 255)
        .next();
    bufferBuilder
        .vertex(left, top, 0)
        .texture(left / 32f, top / 32f)
        .color(64, 64, 64, 255)
        .next();
    tessellator.draw();
  }

  private void renderTexts(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    switch (currentState) {
      case GROUP_SELECT:
        renderTextsForGroupSelect(matrixStack, mouseX, mouseY, partialTicks);
        break;
      case PAINTING_SELECT:
        renderTextsForPaintingSelect(matrixStack, mouseX, mouseY, partialTicks);
        break;
    }
  }

  private void renderTextsForGroupSelect(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    drawCenteredTextWithShadow(
        matrixStack,
        textRenderer,
        Text.translatable("custompaintings.painting.choose").asOrderedText(),
        width / 2,
        11,
        0xFFFFFFFF);
  }

  private void renderTextsForPaintingSelect(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    int posY = 11;
    PaintingData paintingData = currentGroup.paintings().get(currentPainting);

    if (hasMultipleGroups()) {
      drawCenteredTextWithShadow(
          matrixStack,
          textRenderer,
          Text.literal(currentGroup.name()).asOrderedText(),
          width / 2,
          posY,
          0xFFFFFFFF);
    }

    posY += textRenderer.fontHeight + 2;

    drawCenteredTextWithShadow(
        matrixStack,
        textRenderer,
        Text.translatable("custompaintings.painting.number", currentPainting + 1, currentGroup.paintings().size())
            .asOrderedText(),
        width / 2,
        posY,
        0xFFFFFFFF);

    posY += textRenderer.fontHeight + 2;

    drawCenteredTextWithShadow(
        matrixStack,
        textRenderer,
        Text.translatable("custompaintings.painting.dimensions", paintingData.width(), paintingData.height())
            .asOrderedText(),
        width / 2,
        posY,
        0xFFFFFFFF);

    if (paintingData.hasName() || paintingData.hasArtist()) {
      posY += textRenderer.fontHeight + 2;

      List<OrderedText> parts = new ArrayList<>();
      if (paintingData.hasName()) {
        parts.add(Text.literal("\"" + paintingData.name() + "\"").asOrderedText());
      }
      if (paintingData.hasName() && paintingData.hasArtist()) {
        parts.add(Text.of(" - ").asOrderedText());
      }
      if (paintingData.hasArtist()) {
        parts.add(OrderedText.styledForwardsVisitedString(paintingData.artist(), Style.EMPTY.withItalic(true)));
      }

      drawCenteredTextWithShadow(
          matrixStack,
          textRenderer,
          OrderedText.concat(parts),
          width / 2,
          posY,
          0xFFFFFFFF);
    }

    posY += textRenderer.fontHeight + 2;

    drawCenteredTextWithShadow(
        matrixStack,
        textRenderer,
        OrderedText.styledForwardsVisitedString("(" + paintingData.id().toString() + ")",
            Style.EMPTY.withItalic(true).withColor(Formatting.GRAY)),
        width / 2,
        posY,
        0xFFFFFFFF);
  }

  private boolean hasMultipleGroups() {
    return allPaintings.keySet().size() > 1;
  }

  private boolean hasMultiplePaintings() {
    return currentGroup.paintings().size() > 1;
  }

  private void saveEmpty() {
    saveSelection(PaintingData.EMPTY);
  }

  private void saveCurrentSelection() {
    if (currentGroup == null || currentPainting >= currentGroup.paintings().size()) {
      saveEmpty();
    }
    saveSelection(currentGroup.paintings().get(currentPainting));
  }

  private void saveSelection(PaintingData paintingData) {
    ClientNetworking.sendSetPaintingPacket(paintingUuid, paintingData);
    close();
  }

  private void setState(State state, Action onSwitch) {
    nextState = state;
    onStateSwitch = onSwitch;
  }

  private void checkAndAdvanceState() {
    if (currentState != nextState) {
      currentState = nextState;

      if (onStateSwitch != null) {
        onStateSwitch.execute();
        onStateSwitch = null;
      }

      selectedIndex = 0;
      clearAndInit();
    }
  }

  private void setStateImmediate(State state) {
    currentState = state;
    nextState = state;
    onStateSwitch = null;
  }

  public void selectGroup(String id) {
    if (!allPaintings.containsKey(id)) {
      return;
    }

    setState(State.PAINTING_SELECT, () -> {
      currentGroup = allPaintings.get(id);
      currentPainting = 0;
    });
  }

  private void clearGroup() {
    setState(State.GROUP_SELECT, () -> {
      currentGroup = null;
      currentPainting = 0;
    });
  }

  private void previousPainting() {
    currentPainting = (currentGroup.paintings().size() + currentPainting - 1) % currentGroup.paintings().size();
    selectedIndex = getSelectedIndex();
    clearAndInit();
  }

  private void nextPainting() {
    currentPainting = (currentPainting + 1) % currentGroup.paintings().size();
    selectedIndex = getSelectedIndex();
    clearAndInit();
  }

  private int getSelectedIndex() {
    return Math.max(0, children().indexOf(getFocused()));
  }

  private void refreshPaintings() {
    allPaintings.clear();

    Registry.PAINTING_VARIANT.stream()
        .forEach((vanillaVariant) -> {
          Identifier id = Registry.PAINTING_VARIANT.getId(vanillaVariant);
          RegistryKey<PaintingVariant> key = RegistryKey.of(Registry.PAINTING_VARIANT_KEY, id);
          Optional<RegistryEntry<PaintingVariant>> maybeEntry = Registry.PAINTING_VARIANT.getEntry(key);

          if (!maybeEntry.isPresent()) {
            return;
          }

          RegistryEntry<PaintingVariant> entry = maybeEntry.get();
          boolean placeable = entry.isIn(PaintingVariantTags.PLACEABLE);
          String groupId = id.getNamespace() + (placeable ? "" : "_unplaceable");

          if (!allPaintings.containsKey(groupId)) {
            String groupName = !placeable ? "Minecraft: The Hidden Ones"
                : FabricLoader.getInstance()
                    .getModContainer(groupId)
                    .map((mod) -> mod.getMetadata().getName()).orElse(groupId);
            allPaintings.put(groupId, new Group(groupId, groupName, new ArrayList<>()));
          }

          allPaintings.get(groupId).paintings().add(new PaintingData(vanillaVariant));
        });

    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    paintingManager.getPacks().stream()
        .forEach((pack) -> {
          String groupId = pack.id();
          String groupName = pack.name();

          if (!allPaintings.containsKey(groupId)) {
            allPaintings.put(groupId, new Group(groupId, groupName, new ArrayList<>()));
          }

          pack.paintings().stream()
              .forEach((painting) -> {
                allPaintings.get(groupId).paintings().add(
                    new PaintingData(
                        new Identifier(pack.id(), painting.id()),
                        painting.width().orElse(1),
                        painting.height().orElse(1),
                        painting.name().orElse(""),
                        painting.artist().orElse("")));
              });
        });
  }

  private boolean canStay(PaintingData customPaintingInfo) {
    return canStay(customPaintingInfo.getScaledWidth(), customPaintingInfo.getScaledHeight());
  }

  private boolean canStay(int width, int height) {
    World world = client.player.world;
    Box boundingBox = getBoundingBox(width, height);

    if (!world.isSpaceEmpty(boundingBox)) {
      return false;
    }

    int blocksWidth = Math.max(1, width / 16);
    int blocksHeight = Math.max(1, height / 16);
    BlockPos pos = blockPos.offset(facing.getOpposite());
    Direction direction = facing.rotateYCounterclockwise();
    BlockPos.Mutable mutable = new BlockPos.Mutable();

    for (int x = 0; x < blocksWidth; x++) {
      for (int z = 0; z < blocksHeight; z++) {
        mutable.set(pos)
            .move(direction, x - (blocksWidth - 1) / 2)
            .move(Direction.UP, z - (blocksHeight - 1) / 2);
        BlockState blockState = world.getBlockState(mutable);

        if (!blockState.getMaterial().isSolid()
            && !AbstractRedstoneGateBlock.isRedstoneGate(blockState)) {
          return false;
        }
      }
    }

    Entity entity = world.getEntityById(paintingId);
    PaintingEntity currentPainting = entity != null && entity instanceof PaintingEntity
        ? (PaintingEntity) entity
        : null;

    return world.getOtherEntities(currentPainting, boundingBox, DECORATION_PREDICATE).isEmpty();
  }

  private Box getBoundingBox(int width, int height) {
    double posX = blockPos.getX() + 0.5
        - facing.getOffsetX() * 0.46875
        + facing.rotateYCounterclockwise().getOffsetX() * offsetForEven(width);
    double posY = blockPos.getY() + 0.5
        + offsetForEven(height);
    double posZ = blockPos.getZ() + 0.5
        - facing.getOffsetZ() * 0.46875
        + facing.rotateYCounterclockwise().getOffsetZ() * offsetForEven(width);

    double sizeX = (facing.getAxis() == Direction.Axis.Z ? width : 1) / 32D;
    double sizeY = height / 32D;
    double sizeZ = (facing.getAxis() == Direction.Axis.Z ? 1 : width) / 32D;

    return new Box(
        posX - sizeX,
        posY - sizeY,
        posZ - sizeZ,
        posX + sizeX,
        posY + sizeY,
        posZ + sizeZ);
  }

  private double offsetForEven(int size) {
    return size % 32 == 0 ? 0.5 : 0;
  }

  public record Group(String id, String name, ArrayList<PaintingData> paintings) {
  }

  @FunctionalInterface
  public interface Action {
    public abstract void execute();
  }

  private enum State {
    GROUP_SELECT,
    PAINTING_SELECT;
  }
}
