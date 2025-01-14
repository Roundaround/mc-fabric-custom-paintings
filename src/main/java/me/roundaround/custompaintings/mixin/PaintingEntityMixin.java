package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import me.roundaround.custompaintings.entity.decoration.painting.ExpandedPaintingEntity;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin extends AbstractDecorationEntity implements ExpandedPaintingEntity {
  @Unique
  private UUID editor = null;
  
  @Unique
  private static final TrackedDataHandler<PaintingData> PAINTING_DATA = TrackedDataHandler.create(PaintingData.PACKET_CODEC);

  @Unique
  private static final TrackedData<PaintingData> CUSTOM_DATA = DataTracker.registerData(
		  PaintingEntity.class, PAINTING_DATA
  );
  
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
    boolean changed = !this.dataTracker.get(CUSTOM_DATA).equals(paintingData);
    this.dataTracker.set(CUSTOM_DATA, paintingData);
    if (changed) {
      this.updateAttachmentPosition();
    }
  }

  @Override
  public PaintingData getCustomData() {
	  return this.dataTracker.get(CUSTOM_DATA);
  }

  @Override
  public void setVariant(CustomId id) {
    this.getEntityWorld()
        .getRegistryManager()
        .getOrThrow(RegistryKeys.PAINTING_VARIANT)
        .streamEntries()
        .filter((ref) -> ref.matchesId(id.toIdentifier()))
        .findFirst()
        .ifPresent((entry) -> {
          ((PaintingEntityAccessor) this).invokeSetVariant(entry);
        });
  }
  
  @Inject(method = "initDataTracker", at = @At(value = "HEAD"))
  private void addCustomDataToTracker(DataTracker.Builder builder, CallbackInfo info) {
	  builder.add(CUSTOM_DATA, PaintingData.EMPTY);
  }
  
  @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
  private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo info) {
    if (nbt.contains("PaintingData", NbtElement.COMPOUND_TYPE)) {
      this.setCustomData(PaintingData.read(nbt.getCompound("PaintingData")));
    }
  }

  @ModifyExpressionValue(
      method = "calculateBoundingBox",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/entry/RegistryEntry;value()Ljava/lang/Object;")
  )
  private Object getAltVariant(Object original) {
    PaintingData data = this.getCustomData();
    if (data.isEmpty()) {
      return original;
    }
    return data.toVariant();
  }
  
  static {
	  TrackedDataHandlerRegistry.register(PAINTING_DATA);
  }
}
