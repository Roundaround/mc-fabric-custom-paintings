package me.roundaround.custompaintings.client.gui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.Streams;

import me.roundaround.custompaintings.client.CustomPaintingManager;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.client.gui.PaintingButtonWidget;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.SetPaintingPacket;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.tag.PaintingVariantTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.World;

public class PaintingEditScreen extends Screen {
  private final UUID paintingUuid;
  private final BlockPos blockPos;
  private final Direction facing;
  private PaintingData paintingData = PaintingData.EMPTY;

  protected static final Predicate<Entity> DECORATION_PREDICATE = (
      entity) -> entity instanceof AbstractDecorationEntity;

  public PaintingEditScreen(UUID paintingUuid, BlockPos blockPos, Direction facing) {
    super(Text.translatable("painting.edit"));
    this.paintingUuid = paintingUuid;
    this.blockPos = blockPos;
    this.facing = facing;
  }

  @Override
  public void init() {
    HashMap<String, Group> allPaintings = new HashMap<>();

    // TODO: Replace with Registry.PAINTING_VARIANT.stream() to include all
    Streams.stream(Registry.PAINTING_VARIANT.iterateEntries(PaintingVariantTags.PLACEABLE))
        .map(RegistryEntry::value)
        .filter(this::canStay)
        .forEach((vanillaVariant) -> {
          Identifier id = Registry.PAINTING_VARIANT.getId(vanillaVariant);
          String groupId = id.getNamespace();

          if (!allPaintings.containsKey(groupId)) {
            String groupName = FabricLoader.getInstance()
                .getModContainer(groupId)
                .map((mod) -> mod.getMetadata().getName()).orElse(groupId);
            allPaintings.put(groupId, new Group(groupId, groupName, new ArrayList<>()));
          }

          allPaintings.get(groupId).paintings().add(new PaintingData(vanillaVariant));
        });

    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    paintingManager.getEntries().stream()
        .filter(this::canStay)
        .forEach((paintingData) -> {
          Identifier id = paintingData.getId();
          String groupId = id.getNamespace();

          if (!allPaintings.containsKey(groupId)) {
            String groupName = paintingManager.getPack(groupId)
                .map((pack) -> pack.name()).orElse(groupId);
            allPaintings.put(groupId, new Group(groupId, groupName, new ArrayList<>()));
          }

          allPaintings.get(groupId).paintings().add(paintingData);
        });

    Optional.ofNullable(allPaintings.get("cincity")).ifPresent((group) -> {
      PaintingData paintingData = group.paintings().get(0);

      int maxWidth = width / 8;
      int maxHeight = height / 4;

      int scaledWidth = PaintingButtonWidget.getScaledWidth(paintingData, maxWidth, maxHeight);
      int scaledHeight = PaintingButtonWidget.getScaledHeight(paintingData, maxWidth, maxHeight);

      addDrawableChild(new PaintingButtonWidget(
          (width - scaledWidth) / 2,
          (height - scaledHeight) / 2,
          maxWidth,
          maxHeight,
          (button) -> {
            clearSelection();
            ((PaintingButtonWidget) button).setSelected(true);
            choosePainting(paintingData.getId());
          },
          paintingData));
    });

    addDrawableChild(
        new ButtonWidget(this.width / 2 - 100, this.height / 4 + 120, 200, 20, ScreenTexts.DONE, button -> {
          super.close();
        }));
  }

  @Override
  public void close() {
    paintingData = PaintingData.EMPTY;
    super.close();
  }

  @Override
  public void removed() {
    SetPaintingPacket.sendToServer(paintingUuid, paintingData);
  }

  private void clearSelection() {
    children()
        .stream()
        .filter((child) -> child instanceof PaintingButtonWidget)
        .map((child) -> (PaintingButtonWidget) child)
        .forEach((button) -> {
          button.setSelected(false);
        });
  }

  private void choosePainting(PaintingVariant vanillaVariant) {
    paintingData = new PaintingData(vanillaVariant);
  }

  private void choosePainting(Identifier id) {
    if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
      choosePainting(Registry.PAINTING_VARIANT.get(id));
      return;
    }

    Pair<Integer, Integer> dimensions = CustomPaintingsClientMod.customPaintingManager.getPaintingDimensions(id);
    paintingData = new PaintingData(
        id,
        dimensions.getLeft(),
        dimensions.getRight());
  }

  private boolean canStay(PaintingData customPaintingInfo) {
    return canStay(customPaintingInfo.getScaledWidth(), customPaintingInfo.getScaledHeight());
  }

  private boolean canStay(PaintingVariant vanillaVariant) {
    return canStay(vanillaVariant.getWidth(), vanillaVariant.getHeight());
  }

  private boolean canStay(int width, int height) {
    World world = client.player.world;
    Box boundingBox = getBoundingBox(width, height);

    if (!world.isSpaceEmpty(boundingBox)) {
      return false;
    }

    // TODO: Gross
    int blocksHorizontal = Math.max(1, width / 16);
    int j = Math.max(1, height / 16);
    BlockPos pos = blockPos.offset(facing.getOpposite());
    Direction direction = facing.rotateYCounterclockwise();
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    for (int k = 0; k < blocksHorizontal; ++k) {
      for (int l = 0; l < j; ++l) {
        int m = (blocksHorizontal - 1) / -2;
        int n = (j - 1) / -2;
        mutable.set(pos).move(direction, k + m).move(Direction.UP, l + n);
        BlockState blockState = world.getBlockState(mutable);
        if (blockState.getMaterial().isSolid() || AbstractRedstoneGateBlock.isRedstoneGate(blockState))
          continue;
        return false;
      }
    }

    return world.getEntitiesByClass(Entity.class, boundingBox, DECORATION_PREDICATE).isEmpty();
  }

  private Box getBoundingBox(int width, int height) {
    double posX = blockPos.getX() + 0.5 + facing.getOffsetX() * (offsetForEven(width) - 0.46875);
    double posY = blockPos.getY() + 0.5 + offsetForEven(height);
    double posZ = blockPos.getZ() + 0.5 + facing.getOffsetZ() * (offsetForEven(width) - 0.46875);

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
}
