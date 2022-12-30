package me.roundaround.custompaintings.network;

import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.util.Identifier;

public class NetworkPackets {
  public static final Identifier EDIT_PAINTING_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "edit_painting_packet");
  public static final Identifier SET_PAINTING_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "set_painting_packet");
  public static final Identifier DECLARE_CUSTOM_PAINTING_USER_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "declare_custom_painting_user");
  public static final Identifier DECLARE_KNOWN_PAINTINGS = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "declare_known_paintings");
}
