package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.entity.decoration.painting.PackData;
import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.roundalib.network.RoundaLibPacketCodecs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Networking {
  private Networking() {
  }

  public static final Identifier SUMMARY_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "summary_s2c");
  public static final Identifier IMAGE_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "image_s2c");
  public static final Identifier IMAGE_IDS_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "image_ids_s2c");
  public static final Identifier DOWNLOAD_SUMMARY_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "download_summary_s2c"
  );
  public static final Identifier IMAGE_HEADER_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "image_header_s2c"
  );
  public static final Identifier IMAGE_CHUNK_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "image_chunk_s2c");
  public static final Identifier EDIT_PAINTING_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "edit_painting_s2c"
  );
  public static final Identifier SET_PAINTING_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "set_painting_s2c"
  );
  public static final Identifier SYNC_ALL_DATA_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "sync_all_data_s2c"
  );
  public static final Identifier MIGRATION_FINISH_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "migration_finish_s2c"
  );
  public static final Identifier OPEN_MENU_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "open_menu_s2c");
  public static final Identifier LIST_UNKNOWN_S2C = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "list_unknown_s2c"
  );

  public static final Identifier HASHES_C2S = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hashes_c2s");
  public static final Identifier RELOAD_C2S = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "reload_c2s");
  public static final Identifier SET_PAINTING_C2S = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "set_painting_c2s"
  );
  public static final Identifier RUN_MIGRATION_C2S = Identifier.fromNamespaceAndPath(
      Constants.MOD_ID,
      "run_migration_c2s"
  );

  public static void registerS2CPayloads() {
    PayloadTypeRegistry.clientboundPlay().register(SummaryS2C.ID, SummaryS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(ImageS2C.ID, ImageS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(ImageIdsS2C.ID, ImageIdsS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(DownloadSummaryS2C.ID, DownloadSummaryS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(ImageHeaderS2C.ID, ImageHeaderS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(ImageChunkS2C.ID, ImageChunkS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(EditPaintingS2C.ID, EditPaintingS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(SetPaintingS2C.ID, SetPaintingS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(SyncAllDataS2C.ID, SyncAllDataS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(MigrationFinishS2C.ID, MigrationFinishS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(OpenMenuS2C.ID, OpenMenuS2C.CODEC);
    PayloadTypeRegistry.clientboundPlay().register(ListUnknownS2C.ID, ListUnknownS2C.CODEC);
  }

  public static void registerC2SPayloads() {
    PayloadTypeRegistry.serverboundPlay().register(HashesC2S.ID, HashesC2S.CODEC);
    PayloadTypeRegistry.serverboundPlay().register(ReloadC2S.ID, ReloadC2S.CODEC);
    PayloadTypeRegistry.serverboundPlay().register(SetPaintingC2S.ID, SetPaintingC2S.CODEC);
    PayloadTypeRegistry.serverboundPlay().register(RunMigrationC2S.ID, RunMigrationC2S.CODEC);
  }

  public record SummaryS2C(UUID serverId,
                           List<PackData> packs,
                           String combinedImageHash,
                           Map<CustomId, Boolean> finishedMigrations,
                           boolean skipped,
                           int loadErrorOrSkipCount) implements CustomPacketPayload {
    public static final Type<SummaryS2C> ID = new Type<>(SUMMARY_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, SummaryS2C> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        SummaryS2C::serverId,
        RoundaLibPacketCodecs.forList(PackData.PACKET_CODEC),
        SummaryS2C::packs,
        ByteBufCodecs.STRING_UTF8,
        SummaryS2C::combinedImageHash,
        RoundaLibPacketCodecs.forMap(CustomId.PACKET_CODEC, ByteBufCodecs.BOOL),
        SummaryS2C::finishedMigrations,
        ByteBufCodecs.BOOL,
        SummaryS2C::skipped,
        ByteBufCodecs.INT,
        SummaryS2C::loadErrorOrSkipCount,
        SummaryS2C::new
    );

    @Override
    public Type<SummaryS2C> type() {
      return ID;
    }
  }

  public record ImageS2C(CustomId id, Image image) implements CustomPacketPayload {
    public static final Type<ImageS2C> ID = new Type<>(IMAGE_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        ImageS2C::id,
        Image.PACKET_CODEC,
        ImageS2C::image,
        ImageS2C::new
    );

    @Override
    public Type<ImageS2C> type() {
      return ID;
    }
  }

  public record ImageIdsS2C(List<CustomId> ids) implements CustomPacketPayload {
    public static final Type<ImageIdsS2C> ID = new Type<>(IMAGE_IDS_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageIdsS2C> CODEC =
        StreamCodec.composite(RoundaLibPacketCodecs.forList(CustomId.PACKET_CODEC),
        ImageIdsS2C::ids,
        ImageIdsS2C::new
    );

    @Override
    public Type<ImageIdsS2C> type() {
      return ID;
    }
  }

  public record DownloadSummaryS2C(List<CustomId> ids, int imageCount, int byteCount) implements CustomPacketPayload {
    public static final Type<DownloadSummaryS2C> ID = new Type<>(DOWNLOAD_SUMMARY_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadSummaryS2C> CODEC = StreamCodec.composite(
        RoundaLibPacketCodecs.forList(CustomId.PACKET_CODEC),
        DownloadSummaryS2C::ids,
        ByteBufCodecs.INT,
        DownloadSummaryS2C::imageCount,
        ByteBufCodecs.INT,
        DownloadSummaryS2C::byteCount,
        DownloadSummaryS2C::new
    );

    @Override
    public Type<DownloadSummaryS2C> type() {
      return ID;
    }
  }

  public record ImageHeaderS2C(CustomId id, int width, int height, int totalChunks) implements CustomPacketPayload {
    public static final Type<ImageHeaderS2C> ID = new Type<>(IMAGE_HEADER_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageHeaderS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        ImageHeaderS2C::id,
        ByteBufCodecs.INT,
        ImageHeaderS2C::width,
        ByteBufCodecs.INT,
        ImageHeaderS2C::height,
        ByteBufCodecs.INT,
        ImageHeaderS2C::totalChunks,
        ImageHeaderS2C::new
    );

    @Override
    public Type<ImageHeaderS2C> type() {
      return ID;
    }
  }

  public record ImageChunkS2C(CustomId id, int index, byte[] bytes) implements CustomPacketPayload {
    public static final Type<ImageChunkS2C> ID = new Type<>(IMAGE_CHUNK_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageChunkS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        ImageChunkS2C::id,
        ByteBufCodecs.INT,
        ImageChunkS2C::index,
        ByteBufCodecs.BYTE_ARRAY,
        ImageChunkS2C::bytes,
        ImageChunkS2C::new
    );

    @Override
    public Type<ImageChunkS2C> type() {
      return ID;
    }
  }

  public record EditPaintingS2C(UUID paintingUuid, int paintingId, BlockPos pos, Direction facing) implements
      CustomPacketPayload {
    public static final Type<EditPaintingS2C> ID = new Type<>(EDIT_PAINTING_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, EditPaintingS2C> CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        EditPaintingS2C::paintingUuid,
        ByteBufCodecs.INT,
        EditPaintingS2C::paintingId,
        BlockPos.STREAM_CODEC,
        EditPaintingS2C::pos,
        Direction.STREAM_CODEC,
        EditPaintingS2C::facing,
        EditPaintingS2C::new
    );

    @Override
    public Type<EditPaintingS2C> type() {
      return ID;
    }
  }

  public record SetPaintingS2C(PaintingAssignment assignment) implements CustomPacketPayload {
    public static final Type<SetPaintingS2C> ID = new Type<>(SET_PAINTING_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPaintingS2C> CODEC =
        StreamCodec.composite(PaintingAssignment.PACKET_CODEC,
        SetPaintingS2C::assignment,
        SetPaintingS2C::new
    );

    @Override
    public Type<SetPaintingS2C> type() {
      return ID;
    }
  }

  public record SyncAllDataS2C(List<PaintingAssignment> assignments) implements CustomPacketPayload {
    public static final Type<SyncAllDataS2C> ID = new Type<>(SYNC_ALL_DATA_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAllDataS2C> CODEC = StreamCodec.composite(
        RoundaLibPacketCodecs.forList(PaintingAssignment.PACKET_CODEC),
        SyncAllDataS2C::assignments,
        SyncAllDataS2C::new
    );

    @Override
    public Type<SyncAllDataS2C> type() {
      return ID;
    }
  }

  public record MigrationFinishS2C(CustomId id, boolean succeeded) implements CustomPacketPayload {
    public static final Type<MigrationFinishS2C> ID = new Type<>(MIGRATION_FINISH_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, MigrationFinishS2C> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        MigrationFinishS2C::id,
        ByteBufCodecs.BOOL,
        MigrationFinishS2C::succeeded,
        MigrationFinishS2C::new
    );

    @Override
    public Type<MigrationFinishS2C> type() {
      return ID;
    }
  }

  public record OpenMenuS2C() implements CustomPacketPayload {
    public static final Type<OpenMenuS2C> ID = new Type<>(OPEN_MENU_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuS2C> CODEC = RoundaLibPacketCodecs.empty(
        OpenMenuS2C::new);

    @Override
    public Type<OpenMenuS2C> type() {
      return ID;
    }
  }

  public record ListUnknownS2C(Map<CustomId, Integer> counts) implements CustomPacketPayload {
    public static final Type<ListUnknownS2C> ID = new Type<>(LIST_UNKNOWN_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, ListUnknownS2C> CODEC = StreamCodec.composite(
        RoundaLibPacketCodecs.forMap(CustomId.PACKET_CODEC, ByteBufCodecs.INT),
        ListUnknownS2C::counts,
        ListUnknownS2C::new
    );

    @Override
    public Type<ListUnknownS2C> type() {
      return ID;
    }
  }

  public record HashesC2S(Map<CustomId, String> hashes) implements CustomPacketPayload {
    public static final Type<HashesC2S> ID = new Type<>(HASHES_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, HashesC2S> CODEC = StreamCodec.composite(
        RoundaLibPacketCodecs.forMap(CustomId.PACKET_CODEC, ByteBufCodecs.STRING_UTF8),
        HashesC2S::hashes,
        HashesC2S::new
    );

    @Override
    public Type<HashesC2S> type() {
      return ID;
    }
  }

  public record ReloadC2S(List<String> toActivate, List<String> toDeactivate) implements CustomPacketPayload {
    public static final Type<ReloadC2S> ID = new Type<>(RELOAD_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadC2S> CODEC = StreamCodec.composite(
        RoundaLibPacketCodecs.forList(ByteBufCodecs.STRING_UTF8),
        ReloadC2S::toActivate,
        RoundaLibPacketCodecs.forList(ByteBufCodecs.STRING_UTF8),
        ReloadC2S::toDeactivate,
        ReloadC2S::new
    );

    @Override
    public Type<ReloadC2S> type() {
      return ID;
    }
  }

  public record SetPaintingC2S(int paintingId, CustomId dataId) implements CustomPacketPayload {
    public static final Type<SetPaintingC2S> ID = new Type<>(SET_PAINTING_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPaintingC2S> CODEC = StreamCodec.ofMember(
        SetPaintingC2S::write,
        SetPaintingC2S::read
    );

    private static SetPaintingC2S read(FriendlyByteBuf buf) {
      int paintingId = buf.readInt();
      CustomId dataId = null;
      if (buf.readBoolean()) {
        dataId = CustomId.read(buf);
      }
      return new SetPaintingC2S(paintingId, dataId);
    }

    private void write(FriendlyByteBuf buf) {
      buf.writeInt(this.paintingId);
      if (this.dataId == null) {
        buf.writeBoolean(false);
      } else {
        buf.writeBoolean(true);
        this.dataId.write(buf);
      }
    }

    @Override
    public Type<SetPaintingC2S> type() {
      return ID;
    }
  }

  public record RunMigrationC2S(CustomId id) implements CustomPacketPayload {
    public static final Type<RunMigrationC2S> ID = new Type<>(RUN_MIGRATION_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, RunMigrationC2S> CODEC = StreamCodec.composite(
        CustomId.PACKET_CODEC,
        RunMigrationC2S::id,
        RunMigrationC2S::new
    );

    @Override
    public Type<RunMigrationC2S> type() {
      return ID;
    }
  }
}
