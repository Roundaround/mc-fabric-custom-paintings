package me.roundaround.custompaintings.client.render.item.property.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.render.item.property.select.SelectProperties;
import net.minecraft.client.render.item.property.select.SelectProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class CustomPaintingSelectProperty implements SelectProperty<CustomId> {
  public static final Codec<CustomId> VALUE_CODEC = CustomId.CODEC;
  public static final SelectProperty.Type<CustomPaintingSelectProperty, CustomId> TYPE = SelectProperty.Type.create(
      MapCodec.unit(new CustomPaintingSelectProperty()), VALUE_CODEC);

  @Override
  public CustomId getValue(
      ItemStack stack,
      ClientWorld world,
      LivingEntity user,
      int seed,
      ItemDisplayContext displayContext) {
    NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    if (nbt.isEmpty()) {
      return null;
    }

    String paintingId = nbt.getString(PaintingData.PAINTING_NBT_KEY).orElse(null);
    if (paintingId == null) {
      return null;
    }

    return CustomId.parse(paintingId);
  }

  @Override
  public SelectProperty.Type<CustomPaintingSelectProperty, CustomId> getType() {
    return TYPE;
  }

  @Override
  public Codec<CustomId> valueCodec() {
    return VALUE_CODEC;
  }

  public static void register() {
    SelectProperties.ID_MAPPER.put(Identifier.of(Constants.MOD_ID, "custom_painting"), TYPE);
  }
}
