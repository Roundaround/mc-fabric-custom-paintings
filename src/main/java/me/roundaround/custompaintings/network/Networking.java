package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;
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
  public static final Identifier MIGRATION_FINISH_S2C = new Identifier(
      CustomPaintingsMod.MOD_ID, "migration_finish_s2c");
  public static final Identifier OPEN_MENU_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "open_menu_s2c");

  public static final Identifier HASHES_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "hashes_c2s");
  public static final Identifier RELOAD_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "reload_c2s");
  public static final Identifier SET_PAINTING_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "set_painting_c2s");
  public static final Identifier RUN_MIGRATION_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "run_migration_c2s");

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
    PayloadTypeRegistry.playS2C().register(MigrationFinishS2C.ID, MigrationFinishS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(OpenMenuS2C.ID, OpenMenuS2C.CODEC);
  }

  public static void registerC2SPayloads() {
    PayloadTypeRegistry.playC2S().register(HashesC2S.ID, HashesC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(ReloadC2S.ID, ReloadC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(SetPaintingC2S.ID, SetPaintingC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(RunMigrationC2S.ID, RunMigrationC2S.CODEC);
  }

  public record SummaryS2C(UUID serverId, List<PackData> packs, String combinedImageHash,
                           Map<CustomId, Boolean> finishedMigrations, boolean skipped) implements CustomPayload {
    public static final Id<SummaryS2C> ID = new Id<>(SUMMARY_S2C);
    public static final PacketCodec<RegistryByteBuf, SummaryS2C> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC,
        SummaryS2C::serverId, CustomCodecs.forList(PackData.PACKET_CODEC), SummaryS2C::packs, PacketCodecs.STRING,
        SummaryS2C::combinedImageHash, CustomCodecs.forMap(CustomId.PACKET_CODEC, PacketCodecs.BOOL),
        SummaryS2C::finishedMigrations, PacketCodecs.BOOL, SummaryS2C::skipped, SummaryS2C::new
    );

    @Override
    public Id<SummaryS2C> getId() {
      return ID;
    }
  }

  public record ImageS2C(CustomId id, Image image) implements CustomPayload {
    public static final Id<ImageS2C> ID = new Id<>(IMAGE_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageS2C> CODEC = PacketCodec.tuple(
        CustomId.PACKET_CODEC, ImageS2C::id, Image.PACKET_CODEC, ImageS2C::image, ImageS2C::new);

    @Override
    public Id<ImageS2C> getId() {
      return ID;
    }
  }

  public record ImageIdsS2C(List<CustomId> ids) implements CustomPayload {
    public static final Id<ImageIdsS2C> ID = new Id<>(IMAGE_IDS_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageIdsS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(CustomId.PACKET_CODEC), ImageIdsS2C::ids, ImageIdsS2C::new);

    @Override
    public Id<ImageIdsS2C> getId() {
      return ID;
    }
  }

  public record DownloadSummaryS2C(List<CustomId> ids, int imageCount, int byteCount) implements CustomPayload {
    public static final Id<DownloadSummaryS2C> ID = new Id<>(DOWNLOAD_SUMMARY_S2C);
    public static final PacketCodec<RegistryByteBuf, DownloadSummaryS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(CustomId.PACKET_CODEC), DownloadSummaryS2C::ids, PacketCodecs.INTEGER,
        DownloadSummaryS2C::imageCount, PacketCodecs.INTEGER, DownloadSummaryS2C::byteCount, DownloadSummaryS2C::new
    );

    @Override
    public Id<DownloadSummaryS2C> getId() {
      return ID;
    }
  }

  public record ImageHeaderS2C(CustomId id, int width, int height, int totalChunks) implements CustomPayload {
    public static final Id<ImageHeaderS2C> ID = new Id<>(IMAGE_HEADER_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageHeaderS2C> CODEC = PacketCodec.tuple(CustomId.PACKET_CODEC,
        ImageHeaderS2C::id, PacketCodecs.INTEGER, ImageHeaderS2C::width, PacketCodecs.INTEGER, ImageHeaderS2C::height,
        PacketCodecs.INTEGER, ImageHeaderS2C::totalChunks, ImageHeaderS2C::new
    );

    @Override
    public Id<ImageHeaderS2C> getId() {
      return ID;
    }
  }

  public record ImageChunkS2C(CustomId id, int index, byte[] bytes) implements CustomPayload {
    public static final Id<ImageChunkS2C> ID = new Id<>(IMAGE_CHUNK_S2C);
    public static final PacketCodec<RegistryByteBuf, ImageChunkS2C> CODEC = PacketCodec.tuple(CustomId.PACKET_CODEC,
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

  public record SetPaintingS2C(PaintingAssignment assignment) implements CustomPayload {
    public static final Id<SetPaintingS2C> ID = new Id<>(SET_PAINTING_S2C);
    public static final PacketCodec<RegistryByteBuf, SetPaintingS2C> CODEC = PacketCodec.tuple(
        PaintingAssignment.PACKET_CODEC, SetPaintingS2C::assignment, SetPaintingS2C::new);

    @Override
    public Id<SetPaintingS2C> getId() {
      return ID;
    }
  }

  public record SyncAllDataS2C(List<PaintingAssignment> assignments) implements CustomPayload {
    public static final Id<SyncAllDataS2C> ID = new Id<>(SYNC_ALL_DATA_S2C);
    public static final PacketCodec<RegistryByteBuf, SyncAllDataS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(PaintingAssignment.PACKET_CODEC), SyncAllDataS2C::assignments, SyncAllDataS2C::new);

    @Override
    public Id<SyncAllDataS2C> getId() {
      return ID;
    }
  }

  public record MigrationFinishS2C(CustomId id, boolean succeeded) implements CustomPayload {
    public static final Id<MigrationFinishS2C> ID = new Id<>(MIGRATION_FINISH_S2C);
    public static final PacketCodec<RegistryByteBuf, MigrationFinishS2C> CODEC = PacketCodec.tuple(
        CustomId.PACKET_CODEC, MigrationFinishS2C::id, PacketCodecs.BOOL, MigrationFinishS2C::succeeded,
        MigrationFinishS2C::new
    );

    @Override
    public Id<MigrationFinishS2C> getId() {
      return ID;
    }
  }

  public record OpenMenuS2C() implements CustomPayload {
    public static final Id<OpenMenuS2C> ID = new Id<>(OPEN_MENU_S2C);
    public static final PacketCodec<RegistryByteBuf, OpenMenuS2C> CODEC = CustomCodecs.empty(OpenMenuS2C::new);

    @Override
    public Id<OpenMenuS2C> getId() {
      return ID;
    }
  }

  public record HashesC2S(Map<CustomId, String> hashes) implements CustomPayload {
    public static final Id<HashesC2S> ID = new Id<>(HASHES_C2S);
    public static final PacketCodec<RegistryByteBuf, HashesC2S> CODEC = PacketCodec.tuple(
        CustomCodecs.forMap(CustomId.PACKET_CODEC, PacketCodecs.STRING), HashesC2S::hashes, HashesC2S::new);

    @Override
    public Id<HashesC2S> getId() {
      return ID;
    }
  }

  public record ReloadC2S(List<String> toActivate, List<String> toDeactivate) implements CustomPayload {
    public static final Id<ReloadC2S> ID = new Id<>(RELOAD_C2S);
    public static final PacketCodec<RegistryByteBuf, ReloadC2S> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(PacketCodecs.STRING), ReloadC2S::toActivate, CustomCodecs.forList(PacketCodecs.STRING),
        ReloadC2S::toDeactivate, ReloadC2S::new
    );

    @Override
    public Id<ReloadC2S> getId() {
      return ID;
    }
  }

  public record SetPaintingC2S(int paintingId, CustomId dataId) implements CustomPayload {
    public static final Id<SetPaintingC2S> ID = new Id<>(SET_PAINTING_C2S);
    public static final PacketCodec<RegistryByteBuf, SetPaintingC2S> CODEC = PacketCodec.of(
        SetPaintingC2S::write, SetPaintingC2S::read);

    private static SetPaintingC2S read(PacketByteBuf buf) {
      int paintingId = buf.readInt();
      CustomId dataId = null;
      if (buf.readBoolean()) {
        dataId = CustomId.read(buf);
      }
      return new SetPaintingC2S(paintingId, dataId);
    }

    private void write(PacketByteBuf buf) {
      buf.writeInt(this.paintingId);
      if (this.dataId == null) {
        buf.writeBoolean(false);
      } else {
        buf.writeBoolean(true);
        this.dataId.write(buf);
      }
    }

    @Override
    public Id<SetPaintingC2S> getId() {
      return ID;
    }
  }

  public record RunMigrationC2S(CustomId id) implements CustomPayload {
    public static final Id<RunMigrationC2S> ID = new Id<>(RUN_MIGRATION_C2S);
    public static final PacketCodec<RegistryByteBuf, RunMigrationC2S> CODEC = PacketCodec.tuple(
        CustomId.PACKET_CODEC, RunMigrationC2S::id, RunMigrationC2S::new);

    @Override
    public Id<RunMigrationC2S> getId() {
      return ID;
    }
  }
}
