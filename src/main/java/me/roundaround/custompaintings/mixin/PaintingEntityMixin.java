package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingEntityExtensions;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin extends AbstractDecorationEntity implements PaintingEntityExtensions {
  @Unique
  private PaintingData data = PaintingData.EMPTY;

  @Unique
  private UUID editor = null;

  protected PaintingEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
    super(entityType, world);
  }

  @Shadow
  protected abstract void setVariant(RegistryEntry<PaintingVariant> variant);

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
    boolean changed = !this.data.equals(data);
    this.data = data;
    if (changed) {
      this.updateAttachmentPosition();
    }
  }

  @Override
  public PaintingData custompaintings$getData() {
    return this.data;
  }

  @Override
  public void custompaintings$setVariant(CustomId id) {
    this.getEntityWorld()
        .getRegistryManager()
        .getOrThrow(RegistryKeys.PAINTING_VARIANT)
        .streamEntries()
        .filter((ref) -> ref.matchesId(id.toIdentifier()))
        .findFirst()
        .ifPresent(this::setVariant);
  }

  @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
  private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
    nbt.getCompound("PaintingData").ifPresent((dataRoot) -> {
      PaintingData.CODEC.decode(NbtOps.INSTANCE, dataRoot)
          .ifSuccess((result) -> this.custompaintings$setData(result.getFirst()));
    });
  }

  @ModifyExpressionValue(
      method = "calculateBoundingBox",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/entry/RegistryEntry;value()Ljava/lang/Object;")
  )
  private Object getAltVariant(Object original) {
    PaintingData data = this.custompaintings$getData();
    if (data.isEmpty() || data.vanilla()) {
      return original;
    }
    return data.toVariant();
  }
}
