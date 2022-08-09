package me.roundaround.custompaintings.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.client.CustomPaintingManager;
import me.roundaround.custompaintings.client.CustomPaintingsClientMod;
import me.roundaround.custompaintings.entity.decoration.painting.CustomPaintingInfo;
import me.roundaround.custompaintings.entity.decoration.painting.HasCustomPaintingInfo;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin extends AbstractDecorationEntity implements HasCustomPaintingInfo {
  private static final TrackedData<CustomPaintingInfo> CUSTOM_INFO = DataTracker.registerData(
      PaintingEntity.class,
      CustomPaintingsMod.CUSTOM_PAINTING_INFO_HANDLER);

  protected PaintingEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
    super(entityType, world);
  }

  @Override
  public void setCustomInfo(CustomPaintingInfo info) {
    dataTracker.set(CUSTOM_INFO, info);
  }

  @Override
  public void setCustomInfo(Identifier id, int width, int height) {
    setCustomInfo(new CustomPaintingInfo(id, width, height));
  }

  @Override
  public CustomPaintingInfo getCustomPaintingInfo() {
    return dataTracker.get(CUSTOM_INFO);
  }

  @Inject(method = "initDataTracker", at = @At(value = "TAIL"))
  private void initDataTracker(CallbackInfo info) {
    dataTracker.startTracking(CUSTOM_INFO, CustomPaintingInfo.EMPTY);
  }

  @Inject(method = "onTrackedDataSet", at = @At(value = "TAIL"))
  private void onTrackedDataSet(TrackedData<?> data, CallbackInfo info) {
    if (CUSTOM_INFO.equals(data)) {
      updateAttachmentPosition();
    }
  }

  @Inject(method = "writeCustomDataToNbt", at = @At(value = "HEAD"))
  private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo info) {
    CustomPaintingInfo customPaintingInfo = getCustomPaintingInfo();
    if (!customPaintingInfo.isEmpty()) {
      nbt.put("CustomPaintingInfo", customPaintingInfo.writeToNbt());
    }
  }

  @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
  private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
    if (nbt.contains("CustomPaintingInfo", NbtElement.COMPOUND_TYPE)) {
      setCustomInfo(CustomPaintingInfo.fromNbt(nbt.getCompound("CustomPaintingInfo")));
    }
  }

  @Inject(method = "getWidthPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getWidthPixels(CallbackInfoReturnable<Integer> info) {
    CustomPaintingInfo customPaintingInfo = getCustomPaintingInfo();
    if (customPaintingInfo.isEmpty()) {
      return;
    }

    info.setReturnValue(customPaintingInfo.getScaledWidth());
  }

  @Inject(method = "getHeightPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getHeightPixels(CallbackInfoReturnable<Integer> info) {
    CustomPaintingInfo customPaintingInfo = getCustomPaintingInfo();
    if (customPaintingInfo.isEmpty()) {
      return;
    }

    info.setReturnValue(customPaintingInfo.getScaledHeight());
  }

  @Inject(method = "placePainting", at = @At(value = "RETURN"), cancellable = true)
  private static void placePainting(World world, BlockPos pos, Direction facing,
      CallbackInfoReturnable<Optional<PaintingEntity>> info) {
    if (info.getReturnValue().isEmpty()) {
      return;
    }
    PaintingEntity entity = info.getReturnValue().get();

    Identifier id = new Identifier("cincity", "davinci_mona_lisa");

    // TODO: Painting picker!

    // This is client only so it will crash servers. Need to replace with messaging
    CustomPaintingManager paintingManager = CustomPaintingsClientMod.customPaintingManager;
    if (!paintingManager.exists(id)) {
      return;
    }
    Pair<Integer, Integer> dimensions = paintingManager.getPaintingDimensions(id);
    ((HasCustomPaintingInfo) entity).setCustomInfo(id, dimensions.getLeft(), dimensions.getRight());

    info.setReturnValue(Optional.of(entity));
  }
}
