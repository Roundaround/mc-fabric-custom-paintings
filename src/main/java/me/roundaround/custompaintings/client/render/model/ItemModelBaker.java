package me.roundaround.custompaintings.client.render.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BakedSimpleModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ErrorCollectingSpriteGetter;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.util.Identifier;

public class ItemModelBaker {
  private final Map<Identifier, ItemAsset> itemAssets;
  private final Map<Identifier, BakedSimpleModel> simpleModels;
  private final BakedSimpleModel missingModel;

  public ItemModelBaker(
      Map<Identifier, ItemAsset> itemAssets,
      Map<Identifier, BakedSimpleModel> simpleModels,
      BakedSimpleModel missingModel) {
    this.itemAssets = itemAssets;
    this.simpleModels = simpleModels;
    this.missingModel = missingModel;
  }

  public Map<Identifier, ItemModel> bake(ErrorCollectingSpriteGetter spriteGetter) {
    // TODO: Can I replace this?
    ItemModel missingItemModel = ModelBaker.BlockItemModels.bake(this.missingModel, spriteGetter).item();

    BakerImpl bakerImpl = new BakerImpl(spriteGetter);
    HashMap<Identifier, ItemModel> itemModels = new HashMap<>();
    this.itemAssets.forEach((id, asset) -> {
      // TODO: Is null okay for SpriteHolder and PlayerSkinCache? Probably not...
      itemModels.put(
          id,
          asset.model().bake(
              new ItemModel.BakeContext(
                  bakerImpl,
                  LoadedEntityModels.EMPTY,
                  null,
                  null,
                  missingItemModel,
                  asset.registrySwapper())));
    });

    return itemModels;
  }

  class BakerImpl implements Baker {
    private final ErrorCollectingSpriteGetter spriteGetter;
    private final Map<ResolvableCacheKey<Object>, Object> cache = new ConcurrentHashMap<>();

    BakerImpl(ErrorCollectingSpriteGetter spriteGetter) {
      this.spriteGetter = spriteGetter;
    }

    @Override
    public ErrorCollectingSpriteGetter getSpriteGetter() {
      return this.spriteGetter;
    }

    @Override
    public BakedSimpleModel getModel(Identifier id) {
      BakedSimpleModel model = ItemModelBaker.this.simpleModels.get(id);
      if (model != null) {
        return model;
      }

      CustomPaintingsMod.LOGGER.warn("Requested a model that was not discovered previously: {}", id);
      return ItemModelBaker.this.missingModel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T compute(ResolvableCacheKey<T> key) {
      return (T) this.cache.computeIfAbsent((ResolvableCacheKey<Object>) key, k -> k.compute(this));
    }
  }
}
