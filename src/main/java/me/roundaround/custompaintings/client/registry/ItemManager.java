package me.roundaround.custompaintings.client.registry;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.roundaround.custompaintings.config.CustomPaintingsConfig;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.mixin.BakedModelManagerAccessor;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.util.CustomId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.data.ItemModels;
import net.minecraft.client.item.ItemAsset;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BakedSimpleModel;
import net.minecraft.client.render.model.ErrorCollectingSpriteGetter;
import net.minecraft.client.render.model.MissingModel;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.render.model.ReferencedModelsCollector;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.GeneratedItemModel;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class ItemManager {
  private static final Image HOOK_IMAGE = generateItemHookImage();

  private static ItemManager instance = null;

  private final MinecraftClient client;

  public void generateSprites(
      Collection<PaintingData> paintings,
      Function<CustomId, Image> imageSupplier,
      Consumer<SpriteContents> addSprite) {
    if (!CustomPaintingsConfig.getInstance().renderArtworkOnItems.getPendingValue()) {
      return;
    }

    paintings.forEach((painting) -> {
      this.generateSprite(painting, imageSupplier.apply(painting.id())).ifPresent(addSprite);
    });
  }

  public Optional<SpriteContents> generateSprite(PaintingData painting, Image image) {
    int minWidth = 9;
    int maxWidth = 13;
    int minHeight = 8;
    int maxHeight = 12;
    int blockWidth = painting.width();
    int blockHeight = painting.height();

    float scale = Math.min(maxWidth / (float) blockWidth, maxHeight / (float) blockHeight);
    int width = Math.max(minWidth, MathHelper.floor(blockWidth * scale));
    int height = Math.max(minHeight, MathHelper.floor(blockHeight * scale));

    int tx = MathHelper.ceil((16 - width) / 2f);
    int ty = 2 + MathHelper.ceil((13 - height) / 2f);

    Image itemSprite = image.apply(
        Image.Operation.scale(width, height,
            Image.Resampler.combine(
                0.2f,
                Image.Resampler.BILINEAR,
                Image.Resampler.NEAREST_NEIGHBOR_FRAME_PRESERVING)),
        Image.Operation.resize(16, 16),
        Image.Operation.translate(tx, ty),
        Image.Operation.embed(HOOK_IMAGE, 6, ty - 3),
        new Image.Operation() {
          @Override
          public Text getName() {
            return Text.of("Shadow");
          }

          @Override
          public Image.Hashless apply(Image.Hashless source) {
            int minX = tx;
            int maxX = minX + width - 1;

            ArrayList<Image.Color> colors = new ArrayList<>();
            int sampleY = height - 1 + ty;
            for (int x = minX; x <= maxX; x++) {
              Image.Color color = source.getPixel(x, sampleY);
              if (color.getAlphaFloat() > 0.5f) {
                colors.add(color.removeAlpha());
              }
            }
            Image.Color shadow = Image.Color.average(colors).darken(0.15f);

            Image.Color[] pixels = source.copyPixels();
            for (int x = minX; x <= maxX; x++) {
              pixels[Image.getIndex(source.height(), x, sampleY + 1)] = shadow;
            }
            return new Image.Hashless(pixels, source.width(), source.height());
          }
        });

    return this.getSpriteContents(
        CustomId.from(getItemModelId(painting.id())),
        itemSprite,
        width,
        height);
  }

  public CompletableFuture<Void> bakeModels(
      ErrorCollectingSpriteGetter spriteGetter,
      HashMap<CustomId, PaintingData> paintings,
      Executor executor) {
    HashMap<Identifier, UnbakedModel> unbakedModels = new HashMap<>();
    unbakedModels.put(Identifier.ofVanilla("item/generated"), getGeneratedItemModel());

    HashMap<Identifier, ItemAsset> itemAssets = new HashMap<>();
    paintings.values().forEach((painting) -> {
      Identifier id = getItemModelId(painting.id());

      JsonObject json = new JsonObject();
      json.addProperty("parent", "minecraft:item/generated");
      JsonObject textures = new JsonObject();
      textures.addProperty("layer0", id.toString());
      json.add("textures", textures);
      JsonUnbakedModel unbakedModel = JsonUnbakedModel.deserialize(new StringReader(json.toString()));

      unbakedModels.put(id, unbakedModel);
      itemAssets.put(id, new ItemAsset(ItemModels.basic(id), ItemAsset.Properties.DEFAULT));
    });

    ReferencedModelsCollector collector = new ReferencedModelsCollector(unbakedModels, MissingModel.create());
    collector.addSpecialModel(GeneratedItemModel.GENERATED, new GeneratedItemModel());
    itemAssets.forEach((id, asset) -> collector.resolve(asset.model()));
    BakedSimpleModel missingModel = collector.getMissingModel();
    Map<Identifier, BakedSimpleModel> simpleModels = collector.collectModels();

    ModelBaker baker = new ModelBaker(
        LoadedEntityModels.EMPTY,
        Map.of(),
        itemAssets,
        simpleModels,
        missingModel);

    return baker.bake(spriteGetter, executor).thenAccept((bakedModels) -> {
      BakedModelManagerAccessor accessor = (BakedModelManagerAccessor) this.client.getBakedModelManager();
      HashMap<Identifier, ItemModel> itemModels = new HashMap<>(accessor.getBakedItemModels());
      itemModels.putAll(bakedModels.itemStackModels());
      accessor.setBakedItemModels(itemModels);
    });
  }

  private Optional<SpriteContents> getSpriteContents(CustomId id, Image image, int width, int height) {
    if (image == null || image.isEmpty()) {
      return Optional.empty();
    }
    NativeImage nativeImage = image.toNativeImage();
    return Optional.of(new SpriteContents(
        id.toIdentifier(),
        new SpriteDimensions(image.width(), image.height()),
        nativeImage,
        ResourceMetadata.NONE));
  }

  public static ItemManager getInstance() {
    if (instance == null) {
      instance = new ItemManager(MinecraftClient.getInstance());
    }
    return instance;
  }

  public static Identifier getItemModelId(CustomId id) {
    return Identifier.of(Constants.MOD_ID, "item/" + id.pack() + "/" + id.resource());
  }

  private static Image generateItemHookImage() {
    Image.Color[] pixels = new Image.Color[5 * 3];
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = Image.Color.transparent();
    }

    pixels[Image.getIndex(3, 2, 0)] = new Image.Color(99, 99, 99, 255);
    pixels[Image.getIndex(3, 1, 1)] = new Image.Color(99, 99, 99, 255);
    pixels[Image.getIndex(3, 2, 1)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 3, 1)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 0, 2)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 1, 2)] = new Image.Color(53, 53, 53, 255);
    pixels[Image.getIndex(3, 3, 2)] = new Image.Color(38, 38, 38, 255);
    pixels[Image.getIndex(3, 4, 2)] = new Image.Color(53, 53, 53, 255);
    return Image.fromPixels(pixels, 5, 3);
  }

  private static UnbakedModel getGeneratedItemModel() {
    JsonObject json = new JsonObject();
    json.addProperty("parent", "builtin/generated");
    json.addProperty("gui_light", "front");
    JsonObject display = new JsonObject();
    display.add("ground", genDisplayJsonObject(
        0, 0, 0,
        0, 2, 0,
        0.5f, 0.5f, 0.5f));
    display.add("head", genDisplayJsonObject(
        0, 180, 0,
        0, 13, 7,
        1, 1, 1));
    display.add("thirdperson_righthand", genDisplayJsonObject(
        0, 0, 0,
        0, 3, 1,
        0.55f, 0.55f, 0.55f));
    display.add("firstperson_righthand", genDisplayJsonObject(
        0, -90, 25,
        1.13f, 3.2f, 1.13f,
        0.68f, 0.68f, 0.68f));
    display.add("fixed", genDisplayJsonObject(
        0, 180, 0,
        0, 0, 0,
        1, 1, 1));
    json.add("display", display);
    return JsonUnbakedModel.deserialize(new StringReader(json.toString()));
  }

  private static JsonObject genDisplayJsonObject(float... values) {
    if (values.length != 9) {
      throw new IllegalArgumentException("Values must contain 9 values");
    }

    JsonArray rotation = new JsonArray();
    rotation.add(values[0]);
    rotation.add(values[1]);
    rotation.add(values[2]);

    JsonArray translation = new JsonArray();
    translation.add(values[3]);
    translation.add(values[4]);
    translation.add(values[5]);

    JsonArray scale = new JsonArray();
    scale.add(values[6]);
    scale.add(values[7]);
    scale.add(values[8]);

    JsonObject display = new JsonObject();
    display.add("rotation", rotation);
    display.add("translation", translation);
    display.add("scale", scale);

    return display;
  }

  private ItemManager(MinecraftClient client) {
    this.client = client;
  }
}
