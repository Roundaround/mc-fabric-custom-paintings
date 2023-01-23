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
  public static final Identifier DECLARE_KNOWN_PAINTINGS_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "declare_known_paintings_packet");
  public static final Identifier OPEN_MANAGE_SCREEN_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "open_manage_screen_packet");
  public static final Identifier REQUEST_UNKNOWN_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "request_unknown_packet");
  public static final Identifier RESPOND_UNKNOWN_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "respond_unknown_packet");
  public static final Identifier REQUEST_OUTDATED_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "request_outdated_packet");
  public static final Identifier RESPOND_OUTDATED_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "respond_outdated_packet");
  public static final Identifier REASSIGN_ID_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "reassign_id_packet");
}
