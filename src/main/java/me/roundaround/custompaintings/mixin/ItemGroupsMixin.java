package me.roundaround.custompaintings.mixin;

import java.util.HashSet;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.registry.ServerPaintingRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.decoration.painting.PaintingVariants;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;

@Mixin(ItemGroups.class)
public class ItemGroupsMixin {
  @Inject(method = "addPaintings", at = @At("RETURN"))
  private static void afterAddPaintings(
      ItemGroup.Entries entries,
      RegistryWrapper.WrapperLookup registries,
      RegistryWrapper.Impl<PaintingVariant> registryWrapper,
      Predicate<RegistryEntry<PaintingVariant>> filter,
      ItemGroup.StackVisibility stackVisibility,
      CallbackInfo ci) {
    RegistryEntry<PaintingVariant> kebab = registryWrapper.getOrThrow(PaintingVariants.KEBAB);
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

        NbtCompound nbt = new NbtCompound();
        nbt.putString(PaintingData.PACK_NBT_KEY, pack.name());
        nbt.putString(PaintingData.PAINTING_NBT_KEY, id);

        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, (existing) -> {
          return NbtComponent.of(existing.copyNbt().copyFrom(nbt));
        });

        entries.add(stack, stackVisibility);
      });
    });
  }
}
