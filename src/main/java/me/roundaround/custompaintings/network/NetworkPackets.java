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
  public static final Identifier LIST_UNKNOWN_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "list_unknown_packet");
  public static final Identifier REQUEST_MISMATCHED_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "request_mismatched_packet");
  public static final Identifier LIST_MISMATCHED_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "list_mismatched_packet");
  public static final Identifier REASSIGN_ID_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "reassign_id_packet");
  public static final Identifier REASSIGN_ALL_IDS_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "reassign_all_ids_packet");
  public static final Identifier UPDATE_PAINTING_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "update_painting_packet");
  public static final Identifier REMOVE_PAINTING_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "remove_painting_packet");
  public static final Identifier REMOVE_ALL_PAINTINGS_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "remove_all_paintings_packet");
  public static final Identifier APPLY_MIGRATION_PACKET = new Identifier(
      CustomPaintingsMod.MOD_ID,
      "apply_migration_packet");
}
