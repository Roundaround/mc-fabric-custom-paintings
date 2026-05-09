package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingExtensions;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Painting.class)
public abstract class PaintingMixin extends HangingEntity implements PaintingExtensions {
  @Unique
  private PaintingData data = null;

  @Unique
  private UUID editor = null;

  protected PaintingMixin(EntityType<? extends HangingEntity> entityType, Level world) {
    super(entityType, world);
  }

  @Shadow
  protected abstract void setVariant(Holder<PaintingVariant> variant);

  @Override
  public void custompaintings$setEditor(UUID editor) {
    this.editor = editor;
  }

  @Override
  public UUID custompaintings$getEditor() {
    return this.editor;
  }

  @Override
  public void custompaintings$setData(PaintingData data) {
    boolean changed = !this.custompaintings$getData().equals(data);
    this.data = data;
    if (changed) {
      this.recalculateBoundingBox();
    }
  }

  @Override
  public PaintingData custompaintings$getData() {
    if (this.data == null) {
      this.data = PaintingData.EMPTY;
    }
    return this.data;
  }

  @Override
  public void custompaintings$setVariant(CustomId id) {
    this.level()
        .registryAccess()
        .lookupOrThrow(Registries.PAINTING_VARIANT)
        .listElements()
        .filter((ref) -> ref.is(id.toIdentifier()))
        .findFirst()
        .ifPresent(this::setVariant);
  }

  @Inject(
      method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN")
  )
  private void initA(EntityType<?> type, Level level, CallbackInfo ci) {
    this.data = PaintingData.EMPTY;
  }

  @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
  private void initB(Level level, BlockPos blockPos, CallbackInfo ci) {
    this.data = PaintingData.EMPTY;
  }

  @Inject(
      method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;" +
               "Lnet/minecraft/core/Holder;)V", at = @At("RETURN")
  )
  private void initC(Level level, BlockPos blockPos, Direction direction, Holder<?> variant, CallbackInfo ci) {
    this.data = PaintingData.EMPTY;
  }

  @Inject(method = "readAdditionalSaveData", at = @At(value = "HEAD"))
  private void readCustomDataFromNbt(ValueInput view, CallbackInfo info) {
    view.read("PaintingData", PaintingData.CODEC).ifPresent(this::custompaintings$setData);
  }

  @ModifyExpressionValue(
      method = "calculateBoundingBox",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;")
  )
  private Object getAltVariant(Object original) {
    PaintingData data = this.custompaintings$getData();
    if (data.isEmpty() || data.vanilla()) {
      return original;
    }
    return data.toVariant();
  }
}
