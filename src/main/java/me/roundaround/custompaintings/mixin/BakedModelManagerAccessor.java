package me.roundaround.custompaintings.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.util.Identifier;

@Mixin(BakedModelManager.class)
public interface BakedModelManagerAccessor {
  @Accessor("bakedItemModels")
  Map<Identifier, ItemModel> getBakedItemModels();

  @Accessor("bakedItemModels")
  void setBakedItemModels(Map<Identifier, ItemModel> bakedItemModels);
}
