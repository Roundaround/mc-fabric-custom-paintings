package me.roundaround.custompaintings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.roundaround.custompaintings.entity.decoration.painting.CustomPaintingInfo;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class CustomPaintingsMod implements ModInitializer {
  public static final String MOD_ID = "custompaintings";
  public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

  public static final TrackedDataHandler<CustomPaintingInfo> CUSTOM_PAINTING_INFO_HANDLER = new TrackedDataHandler.ImmutableHandler<CustomPaintingInfo>() {
    @Override
    public void write(PacketByteBuf packetByteBuf, CustomPaintingInfo info) {
      if (info.isEmpty()) {
        packetByteBuf.writeBoolean(false);
        return;
      }
      packetByteBuf.writeBoolean(true);
      packetByteBuf.writeIdentifier(info.getId());
      packetByteBuf.writeInt(info.getWidth());
      packetByteBuf.writeInt(info.getHeight());
    }

    @Override
    public CustomPaintingInfo read(PacketByteBuf packetByteBuf) {
      if (!packetByteBuf.readBoolean()) {
        return CustomPaintingInfo.EMPTY;
      }
      Identifier id = packetByteBuf.readIdentifier();
      int width = packetByteBuf.readInt();
      int height = packetByteBuf.readInt();
      return new CustomPaintingInfo(id, width, height);
    }
  };

  @Override
  public void onInitialize() {
    TrackedDataHandlerRegistry.register(CUSTOM_PAINTING_INFO_HANDLER);
  }
}
