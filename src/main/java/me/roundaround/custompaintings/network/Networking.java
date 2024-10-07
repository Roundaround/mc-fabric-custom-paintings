package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingPack;
import me.roundaround.custompaintings.resource.Image;
import me.roundaround.roundalib.network.CustomCodecs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
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
  public static final Identifier IMAGE_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "image_s2c");
  public static final Identifier IMAGE_IDS_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "image_ids_s2c");
  public static final Identifier DOWNLOAD_SUMMARY_S2C = new Identifier(
      CustomPaintingsMod.MOD_ID, "download_summary_s2c");
  public static final Identifier IMAGE_HEADER_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "image_header_s2c");
  public static final Identifier IMAGE_CHUNK_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "image_chunk_s2c");
  public static final Identifier EDIT_PAINTING_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "edit_painting_s2c");
  public static final Identifier SET_PAINTING_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "set_painting_s2c");
  public static final Identifier SYNC_ALL_DATA_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "sync_all_data_s2c");

  public static final Identifier HASHES_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "hashes_c2s");
  public static final Identifier SET_PAINTING_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "set_painting_c2s");

  public static void registerS2CPayloads() {
    PayloadTypeRegistry.playS2C().register(SummaryS2C.ID, SummaryS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ImageS2C.ID, ImageS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ImageIdsS2C.ID, ImageIdsS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(DownloadSummaryS2C.ID, DownloadSummaryS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ImageHeaderS2C.ID, ImageHeaderS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ImageChunkS2C.ID, ImageChunkS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(EditPaintingS2C.ID, EditPaintingS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(SetPaintingS2C.ID, SetPaintingS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(SyncAllDataS2C.ID, SyncAllDataS2C.CODEC);
  }

  public static void registerC2SPayloads() {
    PayloadTypeRegistry.playC2S().register(HashesC2S.ID, HashesC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(SetPaintingC2S.ID, SetPaintingC2S.CODEC);
  }

  public record SummaryS2C(UUID serverId, List<PaintingPack> packs, String combinedImageHash) implements CustomPayload {
    public static final Id<SummaryS2C> ID = new Id<>(SUMMARY_S2C);
    public static final PacketCodec<RegistryByteBuf, SummaryS2C> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC,
        SummaryS2C::serverId, CustomCodecs.forList(PaintingPack.PACKET_CODEC), SummaryS2C::packs, PacketCodecs.STRING,
        SummaryS2C::combinedImageHash, SummaryS2C::new
    );

    @Override
    public Id<SummaryS2C> getId() {
      return ID;
    }
  }

  public record ImageS2C(Identifier id, Image image) implements CustomPayload {
    public static final Id<ImageS2C> ID = new Id<>(IMAGE_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageS2C> CODEC = PacketCodec.tuple(
        Identifier.PACKET_CODEC, ImageS2C::id, Image.PACKET_CODEC, ImageS2C::image, ImageS2C::new);

    @Override
    public Id<ImageS2C> getId() {
      return ID;
    }
  }

  public record ImageIdsS2C(List<Identifier> ids) implements CustomPayload {
    public static final Id<ImageIdsS2C> ID = new Id<>(IMAGE_IDS_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageIdsS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(Identifier.PACKET_CODEC), ImageIdsS2C::ids, ImageIdsS2C::new);

    @Override
    public Id<ImageIdsS2C> getId() {
      return ID;
    }
  }

  public record DownloadSummaryS2C(List<Identifier> ids, int imageCount, int packetCount, int byteCount) implements
      CustomPayload {
    public static final Id<DownloadSummaryS2C> ID = new Id<>(DOWNLOAD_SUMMARY_S2C);
    public static final PacketCodec<RegistryByteBuf, DownloadSummaryS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(Identifier.PACKET_CODEC), DownloadSummaryS2C::ids, PacketCodecs.INTEGER,
        DownloadSummaryS2C::imageCount, PacketCodecs.INTEGER, DownloadSummaryS2C::packetCount, PacketCodecs.INTEGER,
        DownloadSummaryS2C::byteCount, DownloadSummaryS2C::new
    );

    @Override
    public Id<DownloadSummaryS2C> getId() {
      return ID;
    }
  }

  public record ImageHeaderS2C(Identifier id, int width, int height, int totalChunks) implements CustomPayload {
    public static final Id<ImageHeaderS2C> ID = new Id<>(IMAGE_HEADER_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageHeaderS2C> CODEC = PacketCodec.tuple(Identifier.PACKET_CODEC,
        ImageHeaderS2C::id, PacketCodecs.INTEGER, ImageHeaderS2C::width, PacketCodecs.INTEGER, ImageHeaderS2C::height,
        PacketCodecs.INTEGER, ImageHeaderS2C::totalChunks, ImageHeaderS2C::new
    );

    @Override
    public Id<ImageHeaderS2C> getId() {
      return ID;
    }
  }

  public record ImageChunkS2C(Identifier id, int index, byte[] bytes) implements CustomPayload {
    public static final Id<ImageChunkS2C> ID = new Id<>(IMAGE_CHUNK_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageChunkS2C> CODEC = PacketCodec.tuple(Identifier.PACKET_CODEC,
        ImageChunkS2C::id, PacketCodecs.INTEGER, ImageChunkS2C::index, PacketCodecs.BYTE_ARRAY, ImageChunkS2C::bytes,
        ImageChunkS2C::new
    );

    @Override
    public Id<ImageChunkS2C> getId() {
      return ID;
    }
  }

  public record EditPaintingS2C(UUID paintingUuid, int paintingId, BlockPos pos, Direction facing) implements
      CustomPayload {
    public static final Id<EditPaintingS2C> ID = new Id<>(EDIT_PAINTING_S2C);
    public static final PacketCodec<RegistryByteBuf, EditPaintingS2C> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC,
        EditPaintingS2C::paintingUuid, PacketCodecs.INTEGER, EditPaintingS2C::paintingId, BlockPos.PACKET_CODEC,
        EditPaintingS2C::pos, Direction.PACKET_CODEC, EditPaintingS2C::facing, EditPaintingS2C::new
    );

    @Override
    public Id<EditPaintingS2C> getId() {
      return ID;
    }
  }

  public record SetPaintingS2C(int paintingId, Identifier dataId) implements CustomPayload {
    public static final Id<SetPaintingS2C> ID = new Id<>(SET_PAINTING_S2C);
    public static final PacketCodec<RegistryByteBuf, SetPaintingS2C> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER,
        SetPaintingS2C::paintingId, Identifier.PACKET_CODEC, SetPaintingS2C::dataId, SetPaintingS2C::new
    );

    @Override
    public Id<SetPaintingS2C> getId() {
      return ID;
    }
  }

  public record SyncAllDataS2C(List<PaintingIdPair> paintings) implements CustomPayload {
    public static final Id<SyncAllDataS2C> ID = new Id<>(SYNC_ALL_DATA_S2C);
    public static final PacketCodec<RegistryByteBuf, SyncAllDataS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(PaintingIdPair.PACKET_CODEC), SyncAllDataS2C::paintings, SyncAllDataS2C::new);

    @Override
    public Id<SyncAllDataS2C> getId() {
      return ID;
    }
  }

  public record HashesC2S(Map<Identifier, String> hashes) implements CustomPayload {
    public static final Id<HashesC2S> ID = new Id<>(HASHES_C2S);
    public static final PacketCodec<RegistryByteBuf, HashesC2S> CODEC = PacketCodec.tuple(
        CustomCodecs.forMap(Identifier.PACKET_CODEC, PacketCodecs.STRING), HashesC2S::hashes, HashesC2S::new);

    @Override
    public Id<HashesC2S> getId() {
      return ID;
    }
  }

  public record SetPaintingC2S(int paintingId, Identifier dataId) implements CustomPayload {
    public static final Id<SetPaintingC2S> ID = new Id<>(SET_PAINTING_C2S);
    public static final PacketCodec<RegistryByteBuf, SetPaintingC2S> CODEC = PacketCodec.of(
        SetPaintingC2S::write, SetPaintingC2S::read);

    private static SetPaintingC2S read(PacketByteBuf buf) {
      int paintingId = buf.readInt();
      Identifier dataId = null;
      if (buf.readBoolean()) {
        dataId = buf.readIdentifier();
      }
      return new SetPaintingC2S(paintingId, dataId);
    }

    private void write(PacketByteBuf buf) {
      buf.writeInt(this.paintingId);
      if (this.dataId == null) {
        buf.writeBoolean(false);
      } else {
        buf.writeBoolean(true);
        buf.writeIdentifier(this.dataId);
      }
    }

    @Override
    public Id<SetPaintingC2S> getId() {
      return ID;
    }
  }
}
