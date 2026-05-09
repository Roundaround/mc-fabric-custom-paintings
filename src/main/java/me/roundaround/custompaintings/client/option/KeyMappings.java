package me.roundaround.custompaintings.client.option;

import com.mojang.blaze3d.platform.InputConstants;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.roundalib.event.MinecraftClientEvents;
import me.roundaround.roundalib.event.ScreenInputEvent;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

public class KeyMappings {
  public static KeyMapping openMenu;

  private KeyMappings() {
  }

  public static void register() {
    openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        "custompaintings.key.openMainMenu",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        KeyMapping.Category.MISC
    ));

    MinecraftClientEvents.HANDLE_INPUT.register((client) -> {
      while (openMenu.consumeClick()) {
        client.setScreen(new MainMenuScreen(client.screen));
      }
    });

    ScreenInputEvent.EVENT.register((screen, input) -> {
      Minecraft client = Minecraft.getInstance();
      if (client.screen != null && !(client.screen instanceof TitleScreen)) {
        return false;
      }
      if (openMenu.matches(input)) {
        client.setScreen(new MainMenuScreen(client.screen));
        return true;
      }
      return false;
    });
  }
}
