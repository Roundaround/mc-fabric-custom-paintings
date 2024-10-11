package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.custompaintings.client.registry.ClientPaintingRegistry;
import me.roundaround.custompaintings.resource.legacy.LegacyPackMigrator;
import me.roundaround.roundalib.client.event.ScreenInputEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class CustomPaintingsClientMod implements ClientModInitializer {
  public static KeyBinding MIGRATE_LEGACY;

  @Override
  public void onInitializeClient() {
    ClientNetworking.registerReceivers();

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      ClientPaintingRegistry.getInstance().close();
      ClientPaintingManager.getInstance().close();
    });

    MIGRATE_LEGACY = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("custompaintings.keybind.migrateLegacy", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_P,
            "custompaintings.keybind.category"
        ));

    ScreenInputEvent.EVENT.register((screen, keyCode, scanCode, modifiers) -> {
      if (!(screen instanceof TitleScreen)) {
        return false;
      }

      if (MIGRATE_LEGACY.matchesKey(keyCode, scanCode)) {
        LegacyPackMigrator.getInstance().checkForLegacyPacks(MinecraftClient.getInstance());
        return true;
      }

      return false;
    });
  }
}
