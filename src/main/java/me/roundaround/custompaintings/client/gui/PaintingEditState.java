package me.roundaround.custompaintings.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import me.roundaround.custompaintings.client.CustomPaintingManager;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.tag.PaintingVariantTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class PaintingEditState {
  private final MinecraftClient client;
  private final HashMap<String, Group> allPaintings = new HashMap<>();
  private final HashMap<String, Boolean> canStayHashMap = new HashMap<>();
  private final UUID paintingUuid;
  private final int paintingId;
  private final BlockPos blockPos;
  private final Direction facing;
  private final FiltersState filtersState;

  private Group currentGroup = null;
  private PaintingData currentPainting = null;

  private static final Predicate<Entity> DECORATION_PREDICATE = (
      entity) -> entity instanceof AbstractDecorationEntity;

  public PaintingEditState(
      MinecraftClient client,
      UUID paintingUuid,
      int paintingId,
      BlockPos blockPos,
      Direction facing,
      Runnable onFilterChanged) {
    this.client = client;
    this.paintingUuid = paintingUuid;
    this.paintingId = paintingId;
    this.blockPos = blockPos;
    this.facing = facing;

    this.filtersState = new FiltersState(onFilterChanged, this::canStay);
  }

  public FiltersState getFilters() {
    return this.filtersState;
  }

  public Collection<Group> getGroups() {
    return this.allPaintings.values();
  }

  public boolean hasMultipleGroups() {
    return this.allPaintings.size() > 1;
  }

  public boolean hasMultiplePaintings() {
    if (this.currentGroup == null) {
      return false;
    }
    return this.currentGroup.paintings().size() > 1;
  }

  public boolean hasNoPaintings() {
    return this.allPaintings.isEmpty();
  }

  public boolean hasGroup(String id) {
    return this.allPaintings.containsKey(id)
        && !this.allPaintings.get(id).paintings().isEmpty();
  }

  public UUID getPaintingUuid() {
    return this.paintingUuid;
  }

  public Group getCurrentGroup() {
    return this.currentGroup;
  }

  public PaintingData getCurrentPainting() {
    return this.currentPainting;
  }

  public void selectFirstGroup() {
    this.currentGroup = this.allPaintings.values().iterator().next();
  }

  public void setCurrentGroup(String id) {
    if (!hasGroup(id)) {
      return;
    }
    setCurrentGroup(this.allPaintings.get(id));
  }

  public void setCurrentGroup(Group group) {
    this.currentGroup = group;
    if (group != null && !group.paintings().isEmpty()) {
      setCurrentPainting(group.paintings().get(0));
    } else {
      setCurrentPainting(null);
    }
  }

  public void clearGroup() {
    setCurrentGroup((Group) null);
  }

  public void setCurrentPainting(PaintingData painting) {
    this.currentPainting = painting;
  }

  public void populatePaintings() {
    if (!allPaintings.isEmpty()) {
      return;
    }

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
    World world = this.client.player.world;
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
}
