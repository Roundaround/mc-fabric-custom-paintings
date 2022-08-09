package me.roundaround.custompaintings.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.CustomPaintingManager;
import me.roundaround.custompaintings.entity.decoration.painting.CustomPaintingInfo;
import me.roundaround.custompaintings.entity.decoration.painting.HasCustomPaintingInfo;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin extends AbstractDecorationEntity implements HasCustomPaintingInfo {
  private static final TrackedData<CustomPaintingInfo> CUSTOM_INFO = DataTracker.registerData(
      PaintingEntity.class,
      CustomPaintingManager.CUSTOM_PAINTING_INFO_HANDLER);

  protected PaintingEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
    super(entityType, world);
  }

  @Override
  public void setCustomInfo(CustomPaintingInfo info) {
    dataTracker.set(CUSTOM_INFO, info);
  }

  @Override
  public void setCustomInfo(String name, int width, int height) {
    setCustomInfo(new CustomPaintingInfo(name, width, height));
  }

  @Override
  public CustomPaintingInfo getCustomPaintingInfo() {
    return dataTracker.get(CUSTOM_INFO);
  }

  @Inject(method = "initDataTracker", at = @At(value = "TAIL"))
  private void initDataTracker(CallbackInfo info) {
    dataTracker.startTracking(CUSTOM_INFO, CustomPaintingInfo.EMPTY);
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

  @Inject(method = "placePainting", at = @At(value = "RETURN"), cancellable = true)
  private static void placePainting(World world, BlockPos pos, Direction facing, CallbackInfoReturnable<Optional<PaintingEntity>> info) {
    if (info.getReturnValue().isEmpty()) {
      return;
    }
    PaintingEntity entity = info.getReturnValue().get();
    ((HasCustomPaintingInfo) (Object) entity).setCustomInfo("test", 1, 1);

    // TODO: Painting picker!

    info.setReturnValue(Optional.of(entity));
  }
}
