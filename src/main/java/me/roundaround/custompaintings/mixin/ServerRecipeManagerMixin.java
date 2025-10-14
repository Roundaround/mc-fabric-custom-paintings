package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.roundalib.config.option.BooleanConfigOption;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.HashSet;

@Mixin(ServerRecipeManager.class)
public abstract class ServerRecipeManagerMixin {
  @WrapOperation(
      method = "prepare(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)" +
               "Lnet/minecraft/recipe/PreparedRecipes;", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/recipe/PreparedRecipes;of(Ljava/lang/Iterable;)Lnet/minecraft/recipe/PreparedRecipes;"
  )
  )
  private PreparedRecipes appendPreparedRecipes(Iterable<RecipeEntry<?>> recipes, Operation<PreparedRecipes> original) {
    BooleanConfigOption config = CustomPaintingsPerWorldConfig.getInstance().pickPaintingWithStoneCutter;
    if (config == null || !config.getValue()) {
      return original.call(recipes);
    }

    Ingredient ingredient = Ingredient.ofItem(Items.PAINTING);

    ArrayList<RecipeEntry<?>> expanded = new ArrayList<>();
    recipes.forEach(expanded::add);

    HashSet<Identifier> added = new HashSet<>();
    ServerPaintingRegistry.getInstance().getActivePacks().forEach((pack) -> {
      pack.paintings().forEach((painting) -> {
        Identifier id = painting.id().toIdentifier();
        if (added.contains(id)) {
          return;
        }

        ItemStack stack = new ItemStack(Items.PAINTING);

        NbtCompound nbt = new NbtCompound();
        nbt.putString(PaintingData.PACK_NBT_KEY, pack.name());
        nbt.putString(PaintingData.PAINTING_NBT_KEY, id.toString());

        stack.apply(
            DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, (existing) -> {
              return NbtComponent.of(existing.copyNbt().copyFrom(nbt));
            }
        );

        expanded.add(new RecipeEntry<>(
            RegistryKey.of(RegistryKeys.RECIPE, id),
            new StonecuttingRecipe("", ingredient, stack)
        ));
        added.add(id);
      });
    });

    return original.call(expanded);
  }
}
