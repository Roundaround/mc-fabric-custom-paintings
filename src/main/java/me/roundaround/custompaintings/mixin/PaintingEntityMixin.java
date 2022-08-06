package me.roundaround.custompaintings.mixin;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.collect.Streams;

import me.roundaround.custompaintings.PaintingManager;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.RegistryEntry;

@Mixin(PaintingEntity.class)
public abstract class PaintingEntityMixin {
  @Redirect(method = "placePainting", at = @At(value = "INVOKE", target = "net/minecraft/util/registry/Registry.iterateEntries(Lnet/minecraft/tag/TagKey;)Ljava/lang/Iterable;"))
  private Iterable<RegistryEntry<PaintingVariant>> iterateEntriesAndAddCustom(
      DefaultedRegistry<PaintingVariant> registry, TagKey<PaintingVariant> tag) {
    return Streams.concat(
        StreamSupport.stream(registry.iterateEntries(tag).spliterator(), false),
        StreamSupport.stream(PaintingManager.getRegistryEntries(tag).spliterator(), false))
        .collect(Collectors.toList());
  }
}
