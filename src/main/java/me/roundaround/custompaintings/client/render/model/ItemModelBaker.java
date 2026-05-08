package me.roundaround.custompaintings.client.render.model;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import me.roundaround.custompaintings.CustomPaintingsMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.*;
import net.minecraft.util.Identifier;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemModelBaker {
  private final Map<Identifier, ItemAsset> itemAssets;
  private final Map<Identifier, BakedSimpleModel> simpleModels;
  private final BakedSimpleModel missingModel;

  public ItemModelBaker(
      Map<Identifier, ItemAsset> itemAssets,
      Map<Identifier, BakedSimpleModel> simpleModels,
      BakedSimpleModel missingModel
  ) {
    this.itemAssets = itemAssets;
    this.simpleModels = simpleModels;
    this.missingModel = missingModel;
  }

  public Map<Identifier, ItemModel> bake(ErrorCollectingSpriteGetter spriteGetter) {
    // TODO: Can I replace this?
    Baker.Vec3fInterner vec3fInterner = new Vec3fInternerImpl();
    ModelBaker.BlockItemModels blockItemModels = ModelBaker.BlockItemModels.bake(
        this.missingModel,
        spriteGetter,
        vec3fInterner
    );
    ItemModel missingItemModel = ModelBaker.BlockItemModels.bake(this.missingModel, spriteGetter, vec3fInterner).item();

    BakerImpl bakerImpl = new BakerImpl(spriteGetter, vec3fInterner, blockItemModels);
    HashMap<Identifier, ItemModel> itemModels = new HashMap<>();
    this.itemAssets.forEach((id, asset) -> {
      // TODO: Is null okay for SpriteHolder and PlayerSkinCache? Probably not...
      itemModels.put(
          id,
          asset.model()
              .bake(new ItemModel.BakeContext(
                  bakerImpl,
                  LoadedEntityModels.EMPTY,
                  null,
                  null,
                  missingItemModel,
                  asset.registrySwapper()
              ))
      );
    });

    return itemModels;
  }

  class BakerImpl implements Baker {
    private final ErrorCollectingSpriteGetter spriteGetter;
    private final Baker.Vec3fInterner interner;
    private final ModelBaker.BlockItemModels blockItemModels;
    private final Map<ResolvableCacheKey<Object>, Object> cache = new ConcurrentHashMap<>();

    BakerImpl(
        final ErrorCollectingSpriteGetter spriteGetter,
        final Baker.Vec3fInterner interner,
        final ModelBaker.BlockItemModels blockItemModels
    ) {
      this.spriteGetter = spriteGetter;
      this.interner = interner;
      this.blockItemModels = blockItemModels;
    }

    @Override
    public BlockModelPart getBlockPart() {
      return this.blockItemModels.blockPart();
    }

    @Override
    public ErrorCollectingSpriteGetter getSpriteGetter() {
      return this.spriteGetter;
    }

    @Override
    public Vec3fInterner getVec3fInterner() {
      return this.interner;
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

  @Environment(EnvType.CLIENT)
  static class Vec3fInternerImpl implements Baker.Vec3fInterner {
    private final Interner<Vector3fc> INTERNER = Interners.newStrongInterner();

    @Override
    public Vector3fc intern(Vector3fc vec) {
      return this.INTERNER.intern(vec);
    }
  }
}
