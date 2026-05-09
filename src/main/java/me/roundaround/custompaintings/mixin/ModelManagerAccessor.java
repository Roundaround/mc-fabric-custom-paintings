package me.roundaround.custompaintings.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;

@Mixin(ModelManager.class)
public interface ModelManagerAccessor {
  @Accessor("bakedItemStackModels")
  Map<Identifier, ItemModel> getBakedItemStackModels();

  @Accessor("bakedItemStackModels")
  void setBakedItemStackModels(Map<Identifier, ItemModel> bakedItemModels);
}
