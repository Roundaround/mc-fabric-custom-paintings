package me.roundaround.custompaintings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.network.SetPaintingPacket;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.network.PacketByteBuf;

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

  @Override
  public void onInitialize() {
    TrackedDataHandlerRegistry.register(CUSTOM_PAINTING_DATA_HANDLER);
    SetPaintingPacket.registerReceive();
  }
}
