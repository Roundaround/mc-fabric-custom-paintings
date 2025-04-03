package me.roundaround.custompaintings.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.roundaround.custompaintings.client.gui.screen.MainMenuScreen;
import me.roundaround.gradle.api.annotation.Entrypoint;

@Entrypoint(Entrypoint.MOD_MENU)
public class ModMenuImpl implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return MainMenuScreen::new;
  }
}
