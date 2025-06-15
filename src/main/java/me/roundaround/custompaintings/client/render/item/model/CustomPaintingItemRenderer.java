package me.roundaround.custompaintings.client.render.item.model;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class CustomPaintingItemRenderer implements SpecialModelRenderer<CustomId> {
  @Override
  @Nullable
  public CustomId getData(ItemStack stack) {
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
  public void render(
      @Nullable CustomId id,
      ItemDisplayContext displayContext,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      boolean glint) {
    CustomPaintingsMod.LOGGER.info("Rendering custom model");
  }

  public static void register() {
    SpecialModelTypes.ID_MAPPER.put(Identifier.of(Constants.MOD_ID, "painting"), Unbaked.CODEC);
  }

  public record Unbaked() implements SpecialModelRenderer.Unbaked {
    public static final MapCodec<CustomPaintingItemRenderer.Unbaked> CODEC = MapCodec
        .unit(new CustomPaintingItemRenderer.Unbaked());

    @Override
    public MapCodec<CustomPaintingItemRenderer.Unbaked> getCodec() {
      return CODEC;
    }

    @Override
    public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
      return new CustomPaintingItemRenderer();
    }
  }
}
