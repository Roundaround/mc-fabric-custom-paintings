package me.roundaround.custompaintings.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.roundaround.custompaintings.config.CustomPaintingsPerWorldConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import me.roundaround.roundalib.config.option.BooleanConfigOption;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.HashSet;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
  @Shadow
  @Final
  private HolderLookup.Provider registries;

  @WrapOperation(
      method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;" +
               "Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/item/crafting/RecipeMap;create(Ljava/lang/Iterable;)" +
               "Lnet/minecraft/world/item/crafting/RecipeMap;"
  )
  )
  private RecipeMap appendPreparedRecipes(Iterable<RecipeHolder<?>> recipes, Operation<RecipeMap> original) {
    CustomPaintingsPerWorldConfig config = CustomPaintingsPerWorldConfig.getInstance();
    BooleanConfigOption customOption = config.pickPaintingWithStoneCutter;
    BooleanConfigOption vanillaOption = config.pickVanillaPaintingWithStoneCutter;
    boolean addCustom = customOption != null && customOption.getValue();
    boolean addVanilla = vanillaOption != null && vanillaOption.getValue();
    if (!addCustom && !addVanilla) {
      return original.call(recipes);
    }

    Ingredient ingredient = Ingredient.of(Items.PAINTING);

    ArrayList<RecipeHolder<?>> expanded = new ArrayList<>();
    recipes.forEach(expanded::add);

    HashSet<Identifier> added = new HashSet<>();

    if (addCustom) {
      ServerPaintingRegistry.getInstance().getActivePacks().forEach((pack) -> {
        pack.paintings().forEach((painting) -> {
          Identifier id = painting.id().toIdentifier();
          if (added.contains(id)) {
            return;
          }

          ItemStack stack = new ItemStack(Items.PAINTING);

          CompoundTag nbt = new CompoundTag();
          nbt.putString(PaintingData.PACK_NBT_KEY, pack.name());
          nbt.putString(PaintingData.PAINTING_NBT_KEY, id.toString());

          stack.update(
              DataComponents.CUSTOM_DATA, CustomData.EMPTY, (existing) -> {
                return CustomData.of(existing.copyTag().merge(nbt));
              }
          );

          expanded.add(new RecipeHolder<>(
              ResourceKey.create(Registries.RECIPE, id),
              new StonecutterRecipe(new Recipe.CommonInfo(true), ingredient, ItemStackTemplate.fromNonEmptyStack(stack))
          ));
          added.add(id);
        });
      });
    }

    if (addVanilla && this.registries != null) {
      this.registries.lookup(Registries.PAINTING_VARIANT).ifPresent((variantLookup) -> {
        variantLookup.listElements().forEach((entry) -> {
          PaintingVariant variant = entry.value();
          Identifier id = variant.assetId();
          if (added.contains(id)) {
            return;
          }

          ItemStack stack = new ItemStack(Items.PAINTING);
          stack.set(DataComponents.PAINTING_VARIANT, entry);

          expanded.add(new RecipeHolder<>(
              ResourceKey.create(Registries.RECIPE, id),
              new StonecutterRecipe(new Recipe.CommonInfo(true), ingredient, ItemStackTemplate.fromNonEmptyStack(stack))
          ));
          added.add(id);
        });
      });
    }

    return original.call(expanded);
  }
}
