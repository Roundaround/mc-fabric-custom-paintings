package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.resource.PaintingImage;
import me.roundaround.roundalib.network.CustomCodecs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Networking {
  private Networking() {
  }

  public static final Identifier SUMMARY_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "summary_s2c");
  public static final Identifier IMAGES_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "images_s2c");
  public static final Identifier IMAGE_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "image_s2c");
  public static final Identifier EDIT_PAINTING_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "edit_painting_s2c");
  public static final Identifier SET_PAINTING_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "set_painting_s2c");

  public static final Identifier HASHES_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "hashes_c2s");
  public static final Identifier SET_PAINTING_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "set_painting_c2s");

  public static void registerS2CPayloads() {
    PayloadTypeRegistry.playS2C().register(SummaryS2C.ID, SummaryS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ImagesS2C.ID, ImagesS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ImageS2C.ID, ImageS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(EditPaintingS2C.ID, EditPaintingS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(SetPaintingS2C.ID, SetPaintingS2C.CODEC);
  }

  public static void registerC2SPayloads() {
    PayloadTypeRegistry.playC2S().register(HashesC2S.ID, HashesC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(SetPaintingC2S.ID, SetPaintingC2S.CODEC);
  }

  public record SummaryS2C(List<PaintingPack> packs, String combinedImageHash) implements CustomPayload {
    public static final CustomPayload.Id<SummaryS2C> ID = new CustomPayload.Id<>(SUMMARY_S2C);
    public static final PacketCodec<RegistryByteBuf, SummaryS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(PaintingPack.PACKET_CODEC), SummaryS2C::packs, PacketCodecs.STRING,
        SummaryS2C::combinedImageHash, SummaryS2C::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ImageS2C(Identifier id, PaintingImage image) implements CustomPayload {
    public static final CustomPayload.Id<ImageS2C> ID = new CustomPayload.Id<>(IMAGE_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageS2C> CODEC = PacketCodec.tuple(
        Identifier.PACKET_CODEC, ImageS2C::id, PaintingImage.PACKET_CODEC, ImageS2C::image, ImageS2C::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ImagesS2C(List<Identifier> ids) implements CustomPayload {
    public static final CustomPayload.Id<ImagesS2C> ID = new CustomPayload.Id<>(IMAGES_S2C);
    public static final PacketCodec<RegistryByteBuf, ImagesS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(Identifier.PACKET_CODEC), ImagesS2C::ids, ImagesS2C::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record EditPaintingS2C(UUID paintingUuid, int paintingId, BlockPos pos, Direction facing) implements
      CustomPayload {
    public static final CustomPayload.Id<EditPaintingS2C> ID = new CustomPayload.Id<>(EDIT_PAINTING_S2C);
    public static final PacketCodec<RegistryByteBuf, EditPaintingS2C> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC,
        EditPaintingS2C::paintingUuid, PacketCodecs.INTEGER, EditPaintingS2C::paintingId, BlockPos.PACKET_CODEC,
        EditPaintingS2C::pos, Direction.PACKET_CODEC, EditPaintingS2C::facing, EditPaintingS2C::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record SetPaintingS2C(int paintingId, Identifier dataId) implements CustomPayload {
    public static final CustomPayload.Id<SetPaintingS2C> ID = new CustomPayload.Id<>(SET_PAINTING_C2S);
    public static final PacketCodec<RegistryByteBuf, SetPaintingS2C> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER,
        SetPaintingS2C::paintingId, Identifier.PACKET_CODEC, SetPaintingS2C::dataId, SetPaintingS2C::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record HashesC2S(Map<Identifier, String> hashes) implements CustomPayload {
    public static final CustomPayload.Id<HashesC2S> ID = new CustomPayload.Id<>(HASHES_C2S);
    public static final PacketCodec<RegistryByteBuf, HashesC2S> CODEC = PacketCodec.tuple(
        CustomCodecs.forMap(Identifier.PACKET_CODEC, PacketCodecs.STRING), HashesC2S::hashes, HashesC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record SetPaintingC2S(int paintingId, Identifier dataId) implements CustomPayload {
    public static final CustomPayload.Id<SetPaintingC2S> ID = new CustomPayload.Id<>(SET_PAINTING_C2S);
    public static final PacketCodec<RegistryByteBuf, SetPaintingC2S> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER,
        SetPaintingC2S::paintingId, Identifier.PACKET_CODEC, SetPaintingC2S::dataId, SetPaintingC2S::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }
}
