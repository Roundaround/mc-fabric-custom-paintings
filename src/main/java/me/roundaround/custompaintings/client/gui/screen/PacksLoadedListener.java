package me.roundaround.custompaintings.client.gui.screen;

public interface PacksLoadedListener {
  void onPacksLoaded();

  default void onPackTexturesInitialized() {
  }
}
