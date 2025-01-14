package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
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
public abstract class PaintingEntityMixin extends AbstractDecorationEntity implements ExpandedPaintingEntity {
  @Unique
  private PaintingData paintingData = PaintingData.EMPTY;

  @Unique
  private UUID editor = null;

  protected PaintingEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
    super(entityType, world);
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
  public void setCustomData(PaintingData paintingData) {
    boolean changed = !this.paintingData.equals(paintingData);
    this.paintingData = paintingData;
    if (changed) {
      this.updateAttachmentPosition();
    }
  }

  @Override
  public PaintingData getCustomData() {
    return this.paintingData;
  }

  @Override
  public void setVariant(CustomId id) {
    Registries.PAINTING_VARIANT.streamEntries()
        .filter((ref) -> ref.matchesId(id.toIdentifier()))
        .map(RegistryEntry.Reference::getKey)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .flatMap(Registries.PAINTING_VARIANT::getEntry)
        .ifPresent((entry) -> {
          ((PaintingEntityAccessor) this).invokeSetVariant(entry);
        });
  }

  @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
  private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
    if (nbt.contains("PaintingData", NbtElement.COMPOUND_TYPE)) {
      this.setCustomData(PaintingData.read(nbt.getCompound("PaintingData")));
    }
  }

  @Inject(method = "getWidthPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getWidthPixels(CallbackInfoReturnable<Integer> info) {
    PaintingData data = this.getCustomData();
    if (data.isEmpty() || data.vanilla()) {
      return;
    }

    info.setReturnValue(data.getScaledWidth());
  }

  @Inject(method = "getHeightPixels", at = @At(value = "HEAD"), cancellable = true)
  private void getHeightPixels(CallbackInfoReturnable<Integer> info) {
    PaintingData data = this.getCustomData();
    if (data.isEmpty() || data.vanilla()) {
      return;
    }

    info.setReturnValue(data.getScaledHeight());
  }
}
