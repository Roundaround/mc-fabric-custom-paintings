package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.ServerPaintingManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(PaintingEntity.class)
@SuppressWarnings("AddedMixinMembersNamePattern")
public abstract class PaintingEntityMixin extends AbstractDecorationEntity implements ExpandedPaintingEntity {
  @Unique
  @SuppressWarnings("WrongEntityDataParameterClass")
  private static final TrackedData<PaintingData> CUSTOM_DATA = DataTracker.registerData(PaintingEntity.class,
      CustomPaintingsMod.CUSTOM_PAINTING_DATA_HANDLER
  );

  @Unique
  private UUID editor = null;

  protected PaintingEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
    super(entityType, world);
  }

  @Override
  public void setCustomData(PaintingData paintingData) {
    this.dataTracker.set(CUSTOM_DATA, paintingData);
  }

  @Override
  public PaintingData getCustomData() {
    return this.dataTracker.get(CUSTOM_DATA);
  }

  @Override
  public void setEditor(UUID editor) {
    this.editor = editor;
  }

  @Override
  public UUID getEditor() {
    return this.editor;
  }

  @Override
  public void setVariant(Identifier id) {
    Registries.PAINTING_VARIANT.streamEntries()
        .filter((ref) -> ref.matchesId(id))
        .map(RegistryEntry.Reference::getKey)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .flatMap(Registries.PAINTING_VARIANT::getEntry)
        .ifPresent((entry) -> {
          ((PaintingEntityAccessor) this).invokeSetVariant(entry);
        });
  }

  @Inject(method = "onBreak", at = @At(value = "HEAD"))
  private void onBreak(Entity entity, CallbackInfo info) {
    if (this.getWorld().isClient) {
      return;
    }
    ServerPaintingManager.getInstance((ServerWorld) this.getWorld()).remove(this.getUuid());
  }

  @Inject(method = "initDataTracker", at = @At(value = "TAIL"))
  private void initDataTracker(DataTracker.Builder builder, CallbackInfo info) {
    builder.add(CUSTOM_DATA, PaintingData.EMPTY);
  }

  // Yarn mapping for onTrackedDataSet broken in 1.20.5+. Need "onTrackedDataSet" for dev and "method_5674" for publish
  //  @Inject(method = "method_5674", at = @At(value = "TAIL"))
  @Inject(method = "onTrackedDataSet", at = @At(value = "TAIL"))
  private void onTrackedDataSet(TrackedData<?> data, CallbackInfo info) {
    if (CUSTOM_DATA.equals(data)) {
      this.updateAttachmentPosition();
    }
  }

  @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
  private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
    if (nbt.contains("PaintingData", NbtElement.COMPOUND_TYPE)) {
      this.setCustomData(PaintingData.fromNbt(nbt.getCompound("PaintingData")));
    }
  }

  @Inject(method = "getWidthPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getWidthPixels(CallbackInfoReturnable<Integer> info) {
    PaintingData paintingData = this.getCustomData();
    if (paintingData.isEmpty()) {
      return;
    }

    info.setReturnValue(paintingData.getScaledWidth());
  }

  @Inject(method = "getHeightPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getHeightPixels(CallbackInfoReturnable<Integer> info) {
    PaintingData paintingData = this.getCustomData();
    if (paintingData.isEmpty()) {
      return;
    }

    info.setReturnValue(paintingData.getScaledHeight());
  }
}
