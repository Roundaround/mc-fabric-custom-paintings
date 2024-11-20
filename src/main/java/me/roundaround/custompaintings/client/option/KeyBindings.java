package me.roundaround.custompaintings.client.option;

import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.roundalib.client.event.MinecraftClientEvents;
import me.roundaround.roundalib.client.event.ScreenInputEvent;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
  public static KeyBinding openMenu;

  private KeyBindings() {
  }

  public static void register() {
    openMenu = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("custompaintings.key.openMainMenu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U,
            "custompaintings.keyCategory"
        ));

    MinecraftClientEvents.HANDLE_INPUT.register((client) -> {
      while (openMenu.wasPressed()) {
        client.setScreen(new MainMenuScreen(client.currentScreen));
      }
    });

    ScreenInputEvent.EVENT.register((screen, keyCode, scanCode, modifiers) -> {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.currentScreen != null && !(client.currentScreen instanceof TitleScreen)) {
        return false;
      }
      if (openMenu.matchesKey(keyCode, scanCode)) {
        client.setScreen(new MainMenuScreen(client.currentScreen));
        return true;
      }
      return false;
    });
  }
}
