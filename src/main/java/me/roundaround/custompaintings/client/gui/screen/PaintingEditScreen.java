package me.roundaround.custompaintings.client.gui.screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import me.roundaround.custompaintings.client.CustomPaintingManager;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.screen.page.GroupSelectPage;
import me.roundaround.custompaintings.client.gui.screen.page.PaintingEditScreenPage;
import me.roundaround.custompaintings.client.gui.screen.page.PaintingSelectPage;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.tag.PaintingVariantTags;
import net.minecraft.text.Text;
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
  private final HashMap<String, Boolean> canStayHashMap = new HashMap<>();
  private final UUID paintingUuid;
  private final int paintingId;
  private final BlockPos blockPos;
  private final Direction facing;
  private Group currentGroup = null;
  private PaintingData currentPainting = null;
  private PaintingEditScreenPage currentPage;
  private PaintingEditScreenPage nextPage;
  private Action onPageSwitch = null;
  private int selectedIndex = 0;

  private boolean pagesInitialized = false;
  private GroupSelectPage groupSelectPage;
  private PaintingSelectPage paintingSelectPage;

  private static final Predicate<Entity> DECORATION_PREDICATE = (
      entity) -> entity instanceof AbstractDecorationEntity;

  public PaintingEditScreen(UUID paintingUuid, int paintingId, BlockPos blockPos, Direction facing) {
    super(Text.translatable("custompaintings.painting.title"));
    this.paintingUuid = paintingUuid;
    this.paintingId = paintingId;
    this.blockPos = blockPos;
    this.facing = facing;
  }

  public void resetFilters() {
    // TODO
  }

  public void setSearchQuery(String text) {
    // TODO
  }

  public Collection<Group> getGroups() {
    return allPaintings.values();
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
    if (allPaintings.isEmpty()) {
      refreshPaintings();
    }

    if (allPaintings.isEmpty()) {
      saveEmpty();
    }

    initPages();

    if (!hasMultipleGroups()) {
      currentGroup = allPaintings.values().stream().findFirst().get();
      setPageImmediate(this.paintingSelectPage);
    }

    this.currentPage.init();

    if (selectedIndex >= 0 && selectedIndex < children().size()) {
      setInitialFocus(children().get(selectedIndex));
    }
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

  public void markCurrentSelectedIndex() {
    this.selectedIndex = getSelectedIndex();
  }

  public boolean hasMultipleGroups() {
    return allPaintings.keySet().size() > 1;
  }

  public boolean hasMultiplePaintings() {
    return currentGroup.paintings().size() > 1;
  }

  public void saveEmpty() {
    saveSelection(PaintingData.EMPTY);
  }

  public void saveCurrentSelection() {
    if (currentGroup == null || currentPainting == null) {
      saveEmpty();
    }
    saveSelection(currentPainting);
  }

  public void saveSelection(PaintingData paintingData) {
    ClientNetworking.sendSetPaintingPacket(paintingUuid, paintingData);
    close();
  }

  private void setPage(PaintingEditScreenPage page, Action onSwitch) {
    nextPage = page;
    onPageSwitch = onSwitch;
  }

  private void checkAndAdvanceState() {
    if (currentPage != nextPage) {
      currentPage = nextPage;

      if (onPageSwitch != null) {
        onPageSwitch.execute();
        onPageSwitch = null;
      }

      selectedIndex = 0;
      clearAndInit();
    }
  }

  private void setPageImmediate(PaintingEditScreenPage page) {
    currentPage = page;
    nextPage = page;
    onPageSwitch = null;
  }

  public void selectGroup(String id) {
    if (!allPaintings.containsKey(id) || allPaintings.get(id).paintings().isEmpty()) {
      return;
    }

    setPage(this.paintingSelectPage, () -> {
      this.currentGroup = allPaintings.get(id);
      this.currentPainting = allPaintings.get(id).paintings().get(0);
    });
  }

  public void clearGroup() {
    setPage(this.groupSelectPage, () -> {
      this.currentGroup = null;
      this.currentPainting = null;
    });
  }

  public Group getCurrentGroup() {
    return this.currentGroup;
  }

  public PaintingData getCurrentPainting() {
    return this.currentPainting;
  }

  public void setCurrentPainting(PaintingData paintingData) {
    if (this.currentPainting.equals(paintingData)) {
      return;
    }

    this.currentPainting = paintingData;
    markCurrentSelectedIndex();
    clearAndInit();
  }

  public void setCurrentPainting(Function<PaintingData, PaintingData> mapper) {
    setCurrentPainting(mapper.apply(this.currentPainting));
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

          allPaintings.get(groupId).paintings().add(new PaintingData(
              vanillaVariant,
              allPaintings
                  .get(groupId)
                  .paintings()
                  .size()));
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
                        painting.index(),
                        painting.width().orElse(1),
                        painting.height().orElse(1),
                        painting.name().orElse(""),
                        painting.artist().orElse("")));
              });
        });

    allPaintings.values().forEach((group) -> {
      group.paintings().forEach((paintingData) -> {
        String sizeString = paintingData.width() + "x" + paintingData.height();
        if (!canStayHashMap.containsKey(sizeString)) {
          canStayHashMap.put(sizeString, canStay(paintingData));
        }
      });
    });
  }

  public boolean canStay(PaintingData paintingData) {
    String sizeString = paintingData.width() + "x" + paintingData.height();
    if (canStayHashMap.containsKey(sizeString)) {
      return canStayHashMap.get(sizeString);
    }
    boolean result = canStay(paintingData.getScaledWidth(), paintingData.getScaledHeight());
    canStayHashMap.put(sizeString, result);
    return result;
  }

  public boolean canStay(int width, int height) {
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

  private void initPages() {
    if (this.pagesInitialized) {
      return;
    }

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

    this.currentPage = this.groupSelectPage;
    this.nextPage = this.groupSelectPage;
  }

  public record Group(String id, String name, ArrayList<PaintingData> paintings) {
  }

  @FunctionalInterface
  public interface Action {
    public abstract void execute();
  }
}
