package me.roundaround.custompaintings.server;

import me.roundaround.custompaintings.generated.Constants;
import me.roundaround.custompaintings.server.network.ImagePacketQueue;
import me.roundaround.gradle.api.annotation.Entrypoint;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;

import java.net.URI;

@Entrypoint(Entrypoint.SERVER)
public class CustomPaintingsServerMod implements DedicatedServerModInitializer {
  private static String homepage = null;

  @Override
  public void onInitializeServer() {
    ServerTickEvents.START_SERVER_TICK.register((server) -> {
      ImagePacketQueue.getInstance().tick();
    });

    homepage = FabricLoader.getInstance()
        .getModContainer(Constants.MOD_ID)
        .flatMap((container) -> container.getMetadata().getContact().get("homepage"))
        .filter(str -> !str.isBlank())
        .orElse(null);
  }

  public static Text getDownloadPrompt() {
    if (homepage == null) {
      return Text.of(
          "This server uses the Custom Paintings mod. Please install it in your client as well to get the full " +
          "experience!");
    }
    return Text.literal(
            "This server uses the Custom Paintings mod. Click here to download it and get the full experience!")
        .styled((style) -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(homepage))));
  }
}
