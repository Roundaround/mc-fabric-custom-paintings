package me.roundaround.custompaintings.util;

import java.util.List;

import net.minecraft.util.Pair;

public record Migration(
    String id,
    String description,
    String packId,
    int index,
    List<Pair<String, String>> pairs) {

  public Migration(List<Pair<String, String>> pairs) {
    this("", "", "", 0, pairs);
  }
}
