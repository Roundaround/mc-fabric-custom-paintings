package me.roundaround.custompaintings.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public record PaintingHasCustomDataProperty() implements BooleanProperty {
  public static final MapCodec<PaintingHasCustomDataProperty> CODEC = MapCodec
      .unit(new PaintingHasCustomDataProperty());

  @Override
  public boolean test(
      ItemStack stack,
      ClientWorld world,
      LivingEntity entity,
      int seed,
      ItemDisplayContext context) {
    NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    if (nbt.isEmpty()) {
      return false;
    }

    String paintingId = nbt.getString(PaintingData.PAINTING_NBT_KEY).orElse(null);
    if (paintingId == null) {
      return false;
    }

    PaintingData data = ClientPaintingRegistry.getInstance().get(CustomId.parse(paintingId));
    if (data == null || data.isEmpty()) {
      return false;
    }

    return true;
  }

  @Override
  public MapCodec<PaintingHasCustomDataProperty> getCodec() {
    return CODEC;
  }

  public static void register() {
    BooleanProperties.ID_MAPPER.put(Identifier.of(Constants.MOD_ID, "has_custom_painting"), CODEC);
  }
}
