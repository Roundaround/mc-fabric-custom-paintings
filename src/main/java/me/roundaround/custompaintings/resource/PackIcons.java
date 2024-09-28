package me.roundaround.custompaintings.resource;

import me.roundaround.roundalib.client.gui.util.Dimensions;
import net.minecraft.util.Identifier;

public class PackIcons {
  public static final String MINECRAFT_PACK_ID = Identifier.DEFAULT_NAMESPACE;
  public static final String MINECRAFT_HIDDEN_PACK_ID = Identifier.DEFAULT_NAMESPACE + "_unplaceable";
  public static final String ICON_NAMESPACE = "__icon";
  public static final Identifier MINECRAFT_ICON_ID = identifier(MINECRAFT_PACK_ID);
  public static final Identifier MINECRAFT_HIDDEN_ICON_ID = identifier(MINECRAFT_HIDDEN_PACK_ID);

  public static Identifier identifier(String packId) {
    return new Identifier(ICON_NAMESPACE, packId);
  }

  public static Dimensions getScaledDimensions(int width, int height, int maxWidth, int maxHeight) {
    float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
    return new Dimensions(Math.round(scale * width), Math.round(scale * height));
  }

  private PackIcons() {
  }
}
