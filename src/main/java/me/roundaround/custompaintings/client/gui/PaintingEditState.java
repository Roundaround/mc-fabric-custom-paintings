package me.roundaround.custompaintings.client.gui;

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
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PaintingVariantTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;

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
  private PaintingData currentPainting = PaintingData.EMPTY;
  private List<PaintingData> currentPaintings = List.of();
  private StateChangedListener stateChangedListener = null;

  private static final Predicate<Entity> DECORATION_PREDICATE = (
      entity
  ) -> entity instanceof AbstractDecorationEntity;

  public PaintingEditState(
      MinecraftClient client, UUID paintingUuid, int paintingId, BlockPos blockPos, Direction facing
  ) {
    this.client = client;
    this.paintingUuid = paintingUuid;
    this.paintingId = paintingId;
    this.blockPos = blockPos;
    this.facing = facing;

    this.filtersState = new FiltersState(this::canStay);
    this.populatePaintings();
  }

  public void clearStateChangedListener() {
    this.setStateChangedListener(null);
  }

  public void setStateChangedListener(StateChangedListener listener) {
    this.stateChangedListener = listener;
    if (this.stateChangedListener != null) {
      this.stateChangedListener.onPaintingsListChanged();
      this.stateChangedListener.onCurrentPaintingChanged();
    }
  }

  public FiltersState getFilters() {
    return this.filtersState;
  }

  public Collection<Group> getGroups() {
    return this.allPaintings.values();
  }

  public void updatePaintingList() {
    if (this.currentGroup == null) {
      return;
    }

    List<PaintingData> previousCurrentPaintings = this.currentPaintings;
    this.currentPaintings = this.currentGroup.paintings().stream().filter(this.filtersState).toList();

    if (previousCurrentPaintings.size() == this.currentPaintings.size()) {
      boolean atLeastOneDifferent = false;
      for (int i = 0; i < this.currentPaintings.size(); i++) {
        if (!previousCurrentPaintings.get(i).idEquals(this.currentPaintings.get(i))) {
          atLeastOneDifferent = true;
          break;
        }
      }

      if (!atLeastOneDifferent) {
        return;
      }
    }

    if (this.stateChangedListener != null) {
      this.stateChangedListener.onPaintingsListChanged();
    }

    if (!this.currentPaintings.contains(this.currentPainting)) {
      this.setCurrentPainting(this.currentPaintings.isEmpty() ? PaintingData.EMPTY : this.currentPaintings.getFirst());
    }
  }

  public List<PaintingData> getCurrentPaintings() {
    return this.currentPaintings;
  }

  public boolean areAnyPaintingsFiltered() {
    if (this.currentGroup == null) {
      return false;
    }
    return this.currentPaintings.size() < this.currentGroup.paintings().size();
  }

  public boolean hasPaintingsToIterate() {
    return this.currentPaintings.size() > 1;
  }

  public void setPreviousPainting() {
    if (!this.hasPaintingsToIterate()) {
      return;
    }

    int currentIndex = this.currentPaintings.indexOf(this.currentPainting);
    if (currentIndex == -1) {
      this.setCurrentPainting(this.currentPaintings.getLast());
      return;
    }

    this.setCurrentPainting(currentIndex - 1);
  }

  public void setNextPainting() {
    if (!this.hasPaintingsToIterate()) {
      return;
    }

    int currentIndex = this.currentPaintings.indexOf(this.currentPainting);
    if (currentIndex == -1) {
      this.setCurrentPainting(this.currentPaintings.getFirst());
      return;
    }

    this.setCurrentPainting(currentIndex + 1);
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

  public void setCurrentGroup(String id) {
    if (!this.allPaintings.containsKey(id)) {
      return;
    }
    this.setCurrentGroup(this.allPaintings.get(id));
  }

  public void setCurrentGroup(Group group) {
    this.currentGroup = group;
    this.updatePaintingList();
  }

  public void setCurrentPainting(int index) {
    int size = this.currentPaintings.size();
    if (size < 1) {
      this.setCurrentPainting(PaintingData.EMPTY);
    }
    if (size == 1) {
      this.setCurrentPainting(this.currentPaintings.getFirst());
    }
    this.setCurrentPainting(this.currentPaintings.get((size + index) % size));
  }

  public void setCurrentPainting(PaintingData painting) {
    PaintingData prevPainting = this.currentPainting;
    this.currentPainting = painting;
    if (this.stateChangedListener != null && !prevPainting.idEquals(this.currentPainting)) {
      this.stateChangedListener.onCurrentPaintingChanged();
    }
  }

  public void populatePaintings() {
    if (!this.allPaintings.isEmpty()) {
      return;
    }

    Registries.PAINTING_VARIANT.stream().forEach((vanillaVariant) -> {
      Identifier id = Registries.PAINTING_VARIANT.getId(vanillaVariant);
      RegistryKey<PaintingVariant> key = RegistryKey.of(Registries.PAINTING_VARIANT.getKey(), id);
      Optional<RegistryEntry.Reference<PaintingVariant>> maybeEntry = Registries.PAINTING_VARIANT.getEntry(key);

      if (maybeEntry.isEmpty()) {
        return;
      }

      RegistryEntry<PaintingVariant> entry = maybeEntry.get();
      boolean placeable = entry.isIn(PaintingVariantTags.PLACEABLE);
      String groupId = id.getNamespace() + (placeable ? "" : "_unplaceable");

      if (!this.allPaintings.containsKey(groupId)) {
        String groupName = !placeable ?
            "Minecraft: The Hidden Ones" :
            FabricLoader.getInstance()
                .getModContainer(groupId)
                .map((mod) -> mod.getMetadata().getName())
                .orElse(groupId);
        this.allPaintings.put(groupId, new Group(groupId, groupName, new ArrayList<>()));
      }

      this.allPaintings.get(groupId).paintings().add(new PaintingData(vanillaVariant));
    });

    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    paintingManager.getPacks().forEach((pack) -> {
      String groupId = pack.id();
      String groupName = pack.name();

      if (!this.allPaintings.containsKey(groupId)) {
        this.allPaintings.put(groupId, new Group(groupId, groupName, new ArrayList<>()));
      }

      pack.paintings().forEach((painting) -> {
        this.allPaintings.get(groupId)
            .paintings()
            .add(new PaintingData(new Identifier(pack.id(), painting.id()), painting.width().orElse(1),
                painting.height().orElse(1), painting.name().orElse(""), painting.artist().orElse("")
            ));
      });
    });

    this.allPaintings.entrySet().removeIf((entry) -> entry.getValue().paintings().isEmpty());

    this.allPaintings.values().forEach((group) -> {
      group.paintings().forEach((paintingData) -> {
        String sizeString = paintingData.width() + "x" + paintingData.height();
        if (!this.canStayHashMap.containsKey(sizeString)) {
          this.canStayHashMap.put(sizeString, this.canStay(paintingData));
        }
      });
    });
  }

  public boolean canStay() {
    return this.canStay(this.getCurrentPainting());
  }

  public boolean canStay(PaintingData paintingData) {
    String sizeString = paintingData.width() + "x" + paintingData.height();
    if (this.canStayHashMap.containsKey(sizeString)) {
      return this.canStayHashMap.get(sizeString);
    }
    boolean result = this.canStay(paintingData.getScaledWidth(), paintingData.getScaledHeight());
    this.canStayHashMap.put(sizeString, result);
    return result;
  }

  @SuppressWarnings("deprecation")
  public boolean canStay(int width, int height) {
    World world = Objects.requireNonNull(this.client.player).getWorld();
    Box boundingBox = this.getBoundingBox(width, height);

    if (!world.isSpaceEmpty(boundingBox)) {
      return false;
    }

    int blocksWidth = Math.max(1, width / 16);
    int blocksHeight = Math.max(1, height / 16);
    BlockPos pos = this.blockPos.offset(this.facing.getOpposite());
    Direction direction = this.facing.rotateYCounterclockwise();
    BlockPos.Mutable mutable = new BlockPos.Mutable();

    for (int x = 0; x < blocksWidth; x++) {
      for (int z = 0; z < blocksHeight; z++) {
        mutable.set(pos).move(direction, x - (blocksWidth - 1) / 2).move(Direction.UP, z - (blocksHeight - 1) / 2);
        BlockState blockState = world.getBlockState(mutable);

        if (!blockState.isSolid() && !AbstractRedstoneGateBlock.isRedstoneGate(blockState)) {
          return false;
        }
      }
    }

    Entity entity = world.getEntityById(this.paintingId);
    PaintingEntity currentPainting = entity instanceof PaintingEntity ? (PaintingEntity) entity : null;

    return world.getOtherEntities(currentPainting, boundingBox, DECORATION_PREDICATE).isEmpty();
  }

  private Box getBoundingBox(int width, int height) {
    double posX = this.blockPos.getX() + 0.5 - this.facing.getOffsetX() * 0.46875 +
        this.facing.rotateYCounterclockwise().getOffsetX() * this.offsetForEven(width);
    double posY = this.blockPos.getY() + 0.5 + this.offsetForEven(height);
    double posZ = this.blockPos.getZ() + 0.5 - this.facing.getOffsetZ() * 0.46875 +
        this.facing.rotateYCounterclockwise().getOffsetZ() * this.offsetForEven(width);

    double sizeX = (this.facing.getAxis() == Direction.Axis.Z ? width : 1) / 32D;
    double sizeY = height / 32D;
    double sizeZ = (this.facing.getAxis() == Direction.Axis.Z ? 1 : width) / 32D;

    return new Box(posX - sizeX, posY - sizeY, posZ - sizeZ, posX + sizeX, posY + sizeY, posZ + sizeZ);
  }

  private double offsetForEven(int size) {
    return size % 32 == 0 ? 0.5 : 0;
  }

  public record Group(String id, String name, ArrayList<PaintingData> paintings) {
  }

  public interface StateChangedListener {
    void onPaintingsListChanged();

    void onCurrentPaintingChanged();
  }
}
