package me.roundaround.custompaintings.client;

import me.roundaround.custompaintings.client.gui.screen.manage.ManagePaintingsScreen;
import me.roundaround.custompaintings.client.network.ClientNetworking;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceType;

public class CustomPaintingsClientMod implements ClientModInitializer {
  public static CustomPaintingManager customPaintingManager;
  public static KeyBinding openManageScreenKeyBinding;

  @Override
  public void onInitializeClient() {
    MinecraftClientEvents.AFTER_INIT_EVENT_BUS.register(() -> {
      MinecraftClient client = MinecraftClient.getInstance();
      customPaintingManager = new CustomPaintingManager(client.getTextureManager());
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(customPaintingManager);
      MinecraftClientEvents.AFTER_INIT_EVENT_BUS.register(() -> {
        customPaintingManager.close();
        return false;
      });
      return false;
    });

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      customPaintingManager.sendKnownPaintingsToServer();
    });

    ClientNetworking.registerS2CHandlers();

    openManageScreenKeyBinding = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("custompaintings.key.manage", InputUtil.UNKNOWN_KEY.getCode(), KeyBinding.MISC_CATEGORY));

    MinecraftClientEvents.ON_INPUT_EVENT_BUS.register(() -> {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.currentScreen != null) {
        return false;
      }

      if (openManageScreenKeyBinding.wasPressed()) {
        client.setScreen(new ManagePaintingsScreen());
      }
      return false;
    });
  }
}
