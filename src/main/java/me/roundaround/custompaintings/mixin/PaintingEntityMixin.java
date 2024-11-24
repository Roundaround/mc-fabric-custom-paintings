package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    this.getEntityWorld()
        .getRegistryManager()
        .get(RegistryKeys.PAINTING_VARIANT)
        .streamEntries()
        .filter((ref) -> ref.matchesId(id.toIdentifier()))
        .findFirst()
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

  @ModifyArg(
      method = "calculateBoundingBox", at = @At(
      value = "INVOKE", target = "Lnet/minecraft/entity/decoration/painting/PaintingEntity;getOffset(I)D", ordinal = 0
  )
  )
  private int getAltWidth(int originalWidth) {
    PaintingData paintingData = this.getCustomData();
    if (paintingData.isEmpty()) {
      return originalWidth;
    }

    return paintingData.width();
  }

  @ModifyArg(
      method = "calculateBoundingBox", at = @At(
      value = "INVOKE", target = "Lnet/minecraft/entity/decoration/painting/PaintingEntity;getOffset(I)D", ordinal = 1
  )
  )
  private int getAltHeight(int originalHeight) {
    PaintingData paintingData = this.getCustomData();
    if (paintingData.isEmpty()) {
      return originalHeight;
    }

    return paintingData.height();
  }
}
