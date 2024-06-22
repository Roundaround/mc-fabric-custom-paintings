package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.util.Migration;
import me.roundaround.custompaintings.util.MismatchedPainting;
import me.roundaround.custompaintings.util.UnknownPainting;
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
import java.util.UUID;

public final class Networking {
  private Networking() {
  }

  public static final Identifier EDIT_PAINTING_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "edit_painting_s2c");
  public static final Identifier OPEN_MANAGE_SCREEN_S2C = new Identifier(
      CustomPaintingsMod.MOD_ID, "open_manage_screen_s2c");
  public static final Identifier LIST_UNKNOWN_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "list_unknown_s2c");
  public static final Identifier LIST_MISMATCHED_S2C = new Identifier(CustomPaintingsMod.MOD_ID, "list_mismatched_s2c");

  public static final Identifier SET_PAINTING_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "set_painting_c2s");
  public static final Identifier DECLARE_KNOWN_PAINTINGS_C2S = new Identifier(CustomPaintingsMod.MOD_ID,
      "declare_known_paintings_c2s"
  );
  public static final Identifier REQUEST_UNKNOWN_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "request_unknown_c2s");
  public static final Identifier REQUEST_MISMATCHED_C2S = new Identifier(CustomPaintingsMod.MOD_ID,
      "request_mismatched_c2s"
  );
  public static final Identifier REASSIGN_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "reassign_c2s");
  public static final Identifier REASSIGN_ALL_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "reassign_all_c2s");
  public static final Identifier UPDATE_PAINTING_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "update_painting_c2s");
  public static final Identifier REMOVE_PAINTING_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "remove_painting_c2s");
  public static final Identifier REMOVE_ALL_PAINTINGS_C2S = new Identifier(CustomPaintingsMod.MOD_ID,
      "remove_all_paintings_c2s"
  );
  public static final Identifier APPLY_MIGRATION_C2S = new Identifier(CustomPaintingsMod.MOD_ID, "apply_migration_c2s");

  public static void registerS2CPayloads() {
    PayloadTypeRegistry.playS2C().register(EditPaintingS2C.ID, EditPaintingS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(OpenManageScreenS2C.ID, OpenManageScreenS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ListUnknownS2C.ID, ListUnknownS2C.CODEC);
    PayloadTypeRegistry.playS2C().register(ListMismatchedS2C.ID, ListMismatchedS2C.CODEC);
  }

  public static void registerC2SPayloads() {
    PayloadTypeRegistry.playC2S().register(SetPaintingC2S.ID, SetPaintingC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(DeclareKnownPaintingsC2S.ID, DeclareKnownPaintingsC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(RequestUnknownC2S.ID, RequestUnknownC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(RequestMismatchedC2S.ID, RequestMismatchedC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(ReassignC2S.ID, ReassignC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(ReassignAllC2S.ID, ReassignAllC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(UpdatePaintingC2S.ID, UpdatePaintingC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(RemovePaintingC2S.ID, RemovePaintingC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(RemoveAllPaintingsC2S.ID, RemoveAllPaintingsC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(ApplyMigrationC2S.ID, ApplyMigrationC2S.CODEC);
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

  public record OpenManageScreenS2C() implements CustomPayload {
    public static final CustomPayload.Id<OpenManageScreenS2C> ID = new CustomPayload.Id<>(OPEN_MANAGE_SCREEN_S2C);
    public static final PacketCodec<RegistryByteBuf, OpenManageScreenS2C> CODEC = CustomCodecs.empty(
        OpenManageScreenS2C::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ListUnknownS2C(List<UnknownPainting> paintings) implements CustomPayload {
    public static final CustomPayload.Id<ListUnknownS2C> ID = new CustomPayload.Id<>(LIST_UNKNOWN_S2C);
    public static final PacketCodec<RegistryByteBuf, ListUnknownS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(UnknownPainting.PACKET_CODEC), ListUnknownS2C::paintings, ListUnknownS2C::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ListMismatchedS2C(List<MismatchedPainting> paintings) implements CustomPayload {
    public static final CustomPayload.Id<ListMismatchedS2C> ID = new CustomPayload.Id<>(LIST_MISMATCHED_S2C);
    public static final PacketCodec<RegistryByteBuf, ListMismatchedS2C> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(MismatchedPainting.PACKET_CODEC), ListMismatchedS2C::paintings, ListMismatchedS2C::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record SetPaintingC2S(UUID paintingUuid, PaintingData customPaintingInfo) implements CustomPayload {
    public static final CustomPayload.Id<SetPaintingC2S> ID = new CustomPayload.Id<>(SET_PAINTING_C2S);
    public static final PacketCodec<RegistryByteBuf, SetPaintingC2S> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC,
        SetPaintingC2S::paintingUuid, PaintingData.PACKET_CODEC, SetPaintingC2S::customPaintingInfo, SetPaintingC2S::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record DeclareKnownPaintingsC2S(List<PaintingData> paintings) implements CustomPayload {
    public static final CustomPayload.Id<DeclareKnownPaintingsC2S> ID = new CustomPayload.Id<>(
        DECLARE_KNOWN_PAINTINGS_C2S);
    public static final PacketCodec<RegistryByteBuf, DeclareKnownPaintingsC2S> CODEC = PacketCodec.tuple(
        CustomCodecs.forList(PaintingData.PACKET_CODEC), DeclareKnownPaintingsC2S::paintings,
        DeclareKnownPaintingsC2S::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record RequestUnknownC2S() implements CustomPayload {
    public static final CustomPayload.Id<RequestUnknownC2S> ID = new CustomPayload.Id<>(REQUEST_UNKNOWN_C2S);
    public static final PacketCodec<RegistryByteBuf, RequestUnknownC2S> CODEC = CustomCodecs.empty(
        RequestUnknownC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record RequestMismatchedC2S() implements CustomPayload {
    public static final CustomPayload.Id<RequestMismatchedC2S> ID = new CustomPayload.Id<>(REQUEST_MISMATCHED_C2S);
    public static final PacketCodec<RegistryByteBuf, RequestMismatchedC2S> CODEC = CustomCodecs.empty(
        RequestMismatchedC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ReassignC2S(UUID paintingUuid, Identifier id) implements CustomPayload {
    public static final CustomPayload.Id<ReassignC2S> ID = new CustomPayload.Id<>(REASSIGN_C2S);
    public static final PacketCodec<RegistryByteBuf, ReassignC2S> CODEC = PacketCodec.tuple(Uuids.PACKET_CODEC,
        ReassignC2S::paintingUuid, Identifier.PACKET_CODEC, ReassignC2S::id, ReassignC2S::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ReassignAllC2S(Identifier from, Identifier to) implements CustomPayload {
    public static final CustomPayload.Id<ReassignAllC2S> ID = new CustomPayload.Id<>(REASSIGN_ALL_C2S);
    public static final PacketCodec<RegistryByteBuf, ReassignAllC2S> CODEC = PacketCodec.tuple(Identifier.PACKET_CODEC,
        ReassignAllC2S::from, Identifier.PACKET_CODEC, ReassignAllC2S::to, ReassignAllC2S::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record UpdatePaintingC2S(UUID paintingUuid) implements CustomPayload {
    public static final CustomPayload.Id<UpdatePaintingC2S> ID = new CustomPayload.Id<>(UPDATE_PAINTING_C2S);
    public static final PacketCodec<RegistryByteBuf, UpdatePaintingC2S> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, UpdatePaintingC2S::paintingUuid, UpdatePaintingC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record RemovePaintingC2S(UUID paintingUuid) implements CustomPayload {
    public static final CustomPayload.Id<RemovePaintingC2S> ID = new CustomPayload.Id<>(REMOVE_PAINTING_C2S);
    public static final PacketCodec<RegistryByteBuf, RemovePaintingC2S> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, RemovePaintingC2S::paintingUuid, RemovePaintingC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record RemoveAllPaintingsC2S(Identifier id) implements CustomPayload {
    public static final CustomPayload.Id<RemoveAllPaintingsC2S> ID = new CustomPayload.Id<>(REMOVE_ALL_PAINTINGS_C2S);
    public static final PacketCodec<RegistryByteBuf, RemoveAllPaintingsC2S> CODEC = PacketCodec.tuple(
        Identifier.PACKET_CODEC, RemoveAllPaintingsC2S::id, RemoveAllPaintingsC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }

  public record ApplyMigrationC2S(Migration migration) implements CustomPayload {
    public static final CustomPayload.Id<ApplyMigrationC2S> ID = new CustomPayload.Id<>(APPLY_MIGRATION_C2S);
    public static final PacketCodec<RegistryByteBuf, ApplyMigrationC2S> CODEC = PacketCodec.tuple(
        Migration.PACKET_CODEC, ApplyMigrationC2S::migration, ApplyMigrationC2S::new);

    @Override
    public Id<? extends CustomPayload> getId() {
      return ID;
    }
  }
}
