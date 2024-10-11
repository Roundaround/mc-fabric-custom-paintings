package me.roundaround.custompaintings.resource.legacy;

public class LegacyPackMigrator {
  private static LegacyPackMigrator instance = null;

  private LegacyPackMigrator() {
  }

  public static LegacyPackMigrator getInstance() {
    if (instance == null) {
      instance = new LegacyPackMigrator();
    }
    return instance;
  }

  public void checkForLegacyPacks() {

  }
}
