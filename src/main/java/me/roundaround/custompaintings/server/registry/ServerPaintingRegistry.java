package me.roundaround.custompaintings.server.registry;

import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.resource.PaintingImage;
import me.roundaround.custompaintings.resource.PaintingPack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class ServerPaintingRegistry {
  private static ServerPaintingRegistry instance = null;

  private final MinecraftServer server;
  private final HashMap<String, PaintingPack> packs = new HashMap<>();
  private final HashMap<Identifier, PaintingImage> images = new HashMap<>();
  private final HashMap<Identifier, PaintingData> paintings = new HashMap<>();

  private ServerPaintingRegistry(MinecraftServer server) {
    this.server = server;
  }

  public static ServerPaintingRegistry getInstance(MinecraftServer server) {
    if (instance == null) {
      instance = new ServerPaintingRegistry(server);
    }
    return instance;
  }

  public static void close() {
    instance = null;
  }

  public void setPacks(HashMap<String, PaintingPack> packs) {
    this.packs.clear();
    this.paintings.clear();

    this.packs.putAll(packs);
    this.packs.values().forEach((pack) -> {
      pack.paintings().forEach((painting) -> {
        Identifier id = new Identifier(pack.id(), painting.id());
        this.paintings.put(
            id, new PaintingData(id, painting.width(), painting.height(), painting.name(), painting.artist()));
      });
    });

    if (!this.server.isDedicated()) {

    } else {
      
    }
  }
}
