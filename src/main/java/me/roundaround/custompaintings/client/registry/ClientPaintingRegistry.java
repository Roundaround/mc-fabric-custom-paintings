package me.roundaround.custompaintings.client.registry;

import com.google.common.collect.ImmutableList;
import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.registry.CustomPaintingRegistry;
import me.roundaround.custompaintings.resource.PaintingImage;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.*;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ClientPaintingRegistry extends CustomPaintingRegistry implements AutoCloseable {
  private static final Identifier PAINTING_BACK_ID = new Identifier(Identifier.DEFAULT_NAMESPACE, "back");

  private static ClientPaintingRegistry instance = null;

  private final MinecraftClient client;
  private final SpriteAtlasTexture atlas;
  private final HashSet<Identifier> spriteIds = new HashSet<>();

  private ClientPaintingRegistry(MinecraftClient client) {
    this.client = client;
    this.atlas = new SpriteAtlasTexture(new Identifier(CustomPaintingsMod.MOD_ID, "textures/atlas/paintings.png"));
    client.getTextureManager().registerTexture(this.atlas.getId(), this.atlas);

    MinecraftClientEvents.ON_CLOSE_EVENT_BUS.register(this::close);
  }

  public static ClientPaintingRegistry getInstance() {
    if (instance == null) {
      instance = new ClientPaintingRegistry(MinecraftClient.getInstance());
    }
    return instance;
  }

  public Identifier getAtlasId() {
    return this.atlas.getId();
  }

  public Sprite getMissingSprite() {
    return this.atlas.getSprite(MissingSprite.getMissingSpriteId());
  }

  public Sprite getBackSprite() {
    return this.atlas.getSprite(PAINTING_BACK_ID);
  }

  public Sprite getSprite(Identifier id) {
    if (!this.spriteIds.contains(id)) {
      return this.getMissingSprite();
    }
    return this.atlas.getSprite(id);
  }

  public Sprite getSprite(PaintingData data) {
    if (data.isEmpty()) {
      return this.getBackSprite();
    }
    if (data.isVanilla()) {
      return this.client.getPaintingManager().getPaintingSprite(Registries.PAINTING_VARIANT.get(data.id()));
    }
    return this.getSprite(data.id());
  }

  @Override
  public void close() {
    this.atlas.clear();
  }

  @Override
  protected void onImagesChanged() {
    List<SpriteContents> sprites = new ArrayList<>();
    sprites.add(this.getMissingSpriteContents());
    sprites.add(this.getBackSpriteContents());
    this.images.forEach((id, image) -> sprites.add(getSpriteContents(id, image)));
    this.atlas.upload(SpriteLoader.fromAtlas(this.atlas).stitch(sprites, 0, Util.getMainWorkerExecutor()));

    this.spriteIds.clear();
    this.spriteIds.addAll(sprites.stream().map(SpriteContents::getId).toList());
  }

  private SpriteContents getMissingSpriteContents() {
    return MissingSprite.createSpriteContents();
  }

  private SpriteContents getBackSpriteContents() {
    SpriteOpener opener = SpriteOpener.create(SpriteLoader.METADATA_READERS);
    return opener.loadSprite(
        PAINTING_BACK_ID, new Resource(this.client.getDefaultResourcePack(), this.client.getDefaultResourcePack()
            .open(ResourceType.CLIENT_RESOURCES, new Identifier("textures/painting/back.png"))));
  }

  private static NativeImage getNativeImage(PaintingImage paintingImage) {
    NativeImage nativeImage = new NativeImage(paintingImage.width(), paintingImage.height(), false);
    for (int x = 0; x < paintingImage.width(); x++) {
      for (int y = 0; y < paintingImage.height(); y++) {
        nativeImage.setColor(x, y, paintingImage.getABGR(x, y));
      }
    }
    return nativeImage;
  }

  private static SpriteContents getSpriteContents(Identifier id, PaintingImage paintingImage) {
    NativeImage nativeImage = getNativeImage(paintingImage);
    return new SpriteContents(id, new SpriteDimensions(paintingImage.width(), paintingImage.height()), nativeImage,
        getResourceMetadata(paintingImage)
    );
  }

  private static ResourceMetadata getResourceMetadata(PaintingImage paintingImage) {
    return new ResourceMetadata.Builder().add(AnimationResourceMetadata.READER,
        new AnimationResourceMetadata(ImmutableList.of(new AnimationFrameResourceMetadata(0, -1)),
            paintingImage.width(), paintingImage.height(), 1, false
        )
    ).build();
  }
}
