package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin extends AbstractDecorationEntity implements ExpandedPaintingEntity {
  private static final TrackedData<PaintingData> CUSTOM_DATA = DataTracker.registerData(PaintingEntity.class,
      CustomPaintingsMod.CUSTOM_PAINTING_DATA_HANDLER
  );

  private UUID editor = null;

  protected PaintingEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
    super(entityType, world);
  }

  @Override
  public void setCustomData(
      Identifier id, int index, int width, int height, String name, String artist, boolean isVanilla
  ) {
    setCustomData(new PaintingData(id, index, width, height, name, artist, isVanilla));
  }

  @Override
  public void setCustomData(PaintingData paintingData) {
    dataTracker.set(CUSTOM_DATA, paintingData);

    if (paintingData.hasLabel()) {
      setCustomNameVisible(true);
      setCustomName(getPaintingName());
    } else {
      setCustomNameVisible(false);
      setCustomName(null);
    }
  }

  public Text getPaintingName() {
    PaintingData paintingData = getCustomData();
    if (paintingData == null) {
      return super.getDisplayName();
    }

    if (!paintingData.hasLabel()) {
      return super.getDisplayName();
    }

    return paintingData.getLabel();
  }

  @Override
  public PaintingData getCustomData() {
    return dataTracker.get(CUSTOM_DATA);
  }

  @Override
  public void setEditor(UUID editor) {
    this.editor = editor;
  }

  @Override
  public UUID getEditor() {
    return editor;
  }

  @Override
  public void setVariant(Identifier id) {
    Registries.PAINTING_VARIANT.streamEntries()
        .filter((ref) -> ref.matchesId(id))
        .map((ref) -> ref.getKey())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .ifPresent((key) -> {
          Registries.PAINTING_VARIANT.getEntry(key).ifPresent((entry) -> {
            ((PaintingEntityAccessor) this).invokeSetVariant(entry);
          });
        });
  }

  @Inject(method = "initDataTracker", at = @At(value = "TAIL"))
  private void initDataTracker(DataTracker.Builder builder, CallbackInfo info) {
    builder.add(CUSTOM_DATA, PaintingData.EMPTY);
  }

  @Inject(method = "onTrackedDataSet", at = @At(value = "TAIL"))
  private void onTrackedDataSet(TrackedData<?> data, CallbackInfo info) {
    if (CUSTOM_DATA.equals(data)) {
      updateAttachmentPosition();
    }
  }

  @Inject(method = "writeCustomDataToNbt", at = @At(value = "HEAD"))
  private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo info) {
    PaintingData paintingData = getCustomData();
    if (!paintingData.isEmpty()) {
      nbt.put("PaintingData", paintingData.writeToNbt());
    }
  }

  @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
  private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
    if (nbt.contains("PaintingData", NbtElement.COMPOUND_TYPE)) {
      setCustomData(PaintingData.fromNbt(nbt.getCompound("PaintingData")));
    }
  }

  @Inject(method = "getWidthPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getWidthPixels(CallbackInfoReturnable<Integer> info) {
    PaintingData paintingData = getCustomData();
    if (paintingData.isEmpty()) {
      return;
    }

    info.setReturnValue(paintingData.getScaledWidth());
  }

  @Inject(method = "getHeightPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getHeightPixels(CallbackInfoReturnable<Integer> info) {
    PaintingData paintingData = getCustomData();
    if (paintingData.isEmpty()) {
      return;
    }

    info.setReturnValue(paintingData.getScaledHeight());
  }
}
