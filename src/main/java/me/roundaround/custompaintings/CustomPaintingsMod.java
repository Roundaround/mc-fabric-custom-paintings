package me.roundaround.custompaintings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.server.command.CustomPaintingsCommand;
import me.roundaround.custompaintings.server.command.sub.MoveSub.MoveDirectionArgumentType;
import me.roundaround.custompaintings.server.network.ServerNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  public static final TrackedDataHandler<PaintingData> CUSTOM_PAINTING_DATA_HANDLER = new TrackedDataHandler.ImmutableHandler<PaintingData>() {
    @Override
    public void write(PacketByteBuf packetByteBuf, PaintingData info) {
      info.writeToPacketByteBuf(packetByteBuf);
    }

    @Override
    public PaintingData read(PacketByteBuf packetByteBuf) {
      return PaintingData.fromPacketByteBuf(packetByteBuf);
    }
  };

  public static HashMap<UUID, HashSet<PaintingData>> knownPaintings = new HashMap<>();

  @Override
  public void onInitialize() {
    TrackedDataHandlerRegistry.register(CUSTOM_PAINTING_DATA_HANDLER);
    ServerNetworking.registerReceivers();

    ArgumentTypeRegistry.registerArgumentType(new Identifier(CustomPaintingsMod.MOD_ID, "move_direction"),
        MoveDirectionArgumentType.class,
        ConstantArgumentSerializer.of(MoveDirectionArgumentType::direction));

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      CustomPaintingsCommand.register(dispatcher);
    });
  }
}
