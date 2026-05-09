package me.roundaround.custompaintings.client.render.model;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemModelBaker {
  private final Map<Identifier, ClientItem> itemAssets;
  private final Map<Identifier, ResolvedModel> simpleModels;
  private final ResolvedModel missingModel;

  public ItemModelBaker(
      Map<Identifier, ClientItem> itemAssets,
      Map<Identifier, ResolvedModel> simpleModels,
      ResolvedModel missingModel
  ) {
    this.itemAssets = itemAssets;
    this.simpleModels = simpleModels;
    this.missingModel = missingModel;
  }

  public Map<Identifier, ItemModel> bake(MaterialBaker materialBaker, SpriteGetter spriteGetter) {
    ModelBaker.Interner interner = new InternerImpl();
    ModelBakery.MissingModels missingModels = ModelBakery.MissingModels.bake(
        this.missingModel,
        materialBaker,
        interner
    );
    MissingItemModel missingItemModel = missingModels.item();

    BakerImpl bakerImpl = new BakerImpl(materialBaker, interner, missingModels);
    Minecraft client = Minecraft.getInstance();
    PlayerSkinRenderCache skinCache = client.playerSkinRenderCache();
    Matrix4f identity = new Matrix4f();
    HashMap<Identifier, ItemModel> itemModels = new HashMap<>();
    this.itemAssets.forEach((id, asset) -> {
      itemModels.put(
          id,
          asset.model().bake(
              new ItemModel.BakingContext(
                  bakerImpl,
                  EntityModelSet.EMPTY,
                  spriteGetter,
                  skinCache,
                  missingItemModel,
                  asset.registrySwapper()
              ),
              identity
          )
      );
    });

    return itemModels;
  }

  class BakerImpl implements ModelBaker {
    private final MaterialBaker materials;
    private final ModelBaker.Interner interner;
    private final ModelBakery.MissingModels missingModels;
    private final Map<SharedOperationKey<Object>, Object> cache = new ConcurrentHashMap<>();

    BakerImpl(
        final MaterialBaker materials,
        final ModelBaker.Interner interner,
        final ModelBakery.MissingModels missingModels
    ) {
      this.materials = materials;
      this.interner = interner;
      this.missingModels = missingModels;
    }

    @Override
    public BlockStateModelPart missingBlockModelPart() {
      return this.missingModels.blockPart();
    }

    @Override
    public MaterialBaker materials() {
      return this.materials;
    }

    @Override
    public Interner interner() {
      return this.interner;
    }

    @Override
    public ResolvedModel getModel(Identifier id) {
      ResolvedModel model = ItemModelBaker.this.simpleModels.get(id);
      if (model != null) {
        return model;
      }

      CustomPaintingsMod.LOGGER.warn("Requested a model that was not discovered previously: {}", id);
      return ItemModelBaker.this.missingModel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T compute(SharedOperationKey<T> key) {
      return (T) this.cache.computeIfAbsent((SharedOperationKey<Object>) key, k -> k.compute(this));
    }
  }

  static class InternerImpl implements ModelBaker.Interner {
    private final Interner<Vector3fc> vectors = Interners.newStrongInterner();
    private final Interner<BakedQuad.MaterialInfo> materialInfos = Interners.newStrongInterner();

    @Override
    public Vector3fc vector(Vector3fc vec) {
      return this.vectors.intern(vec);
    }

    @Override
    public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo info) {
      return this.materialInfos.intern(info);
    }
  }
}
