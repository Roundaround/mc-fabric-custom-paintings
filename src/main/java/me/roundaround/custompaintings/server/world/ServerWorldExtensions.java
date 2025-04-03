package me.roundaround.custompaintings.server.world;

import me.roundaround.custompaintings.server.ServerInfo;
import me.roundaround.custompaintings.server.ServerPaintingManager;

public interface ServerWorldExtensions {
  default ServerPaintingManager custompaintings$getPaintingManager() {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }

  default ServerInfo custompaintings$getServerInfo() {
    throw new UnsupportedOperationException("Unable to call directly from injected interface. Implemented in mixin.");
  }
}
