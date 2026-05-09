package me.roundaround.custompaintings.mixin;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariants;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.minecraft.tags.PaintingVariantTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.function.Predicate;

@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsMixin {
  @Inject(method = "generatePresetPaintings", at = @At("RETURN"))
  private static void afterAddPaintings(
      CreativeModeTab.Output entries,
      HolderLookup.Provider registries,
      HolderLookup.RegistryLookup<PaintingVariant> registryWrapper,
      Predicate<Holder<PaintingVariant>> filter,
      CreativeModeTab.TabVisibility stackVisibility,
      CallbackInfo ci
  ) {
    Holder<PaintingVariant> kebab = registryWrapper.getOrThrow(PaintingVariants.KEBAB);
    if (!filter.test(kebab)) {
      return;
    }

    HashSet<String> added = new HashSet<>();

    ServerPaintingRegistry.getInstance().getActivePacks().forEach((pack) -> {
      pack.paintings().forEach((painting) -> {
        String id = painting.id().toString();
        if (added.contains(id)) {
          return;
        }
        added.add(id);

        ItemStack stack = new ItemStack(Items.PAINTING);

        CompoundTag nbt = new CompoundTag();
        nbt.putString(PaintingData.PACK_NBT_KEY, pack.name());
        nbt.putString(PaintingData.PAINTING_NBT_KEY, id);

        stack.update(
            DataComponents.CUSTOM_DATA, CustomData.EMPTY, (existing) -> {
              return CustomData.of(existing.copyTag().merge(nbt));
            }
        );

        entries.accept(stack, stackVisibility);
      });
    });

    registryWrapper.listElements().filter((entry) -> !entry.is(PaintingVariantTags.PLACEABLE)).forEach((entry) -> {
      String id = entry.value().assetId().toString();
      if (added.contains(id)) {
        return;
      }
      added.add(id);

      ItemStack stack = new ItemStack(Items.PAINTING);
      stack.set(DataComponents.PAINTING_VARIANT, entry);

      entries.accept(stack, stackVisibility);
    });
  }
}
