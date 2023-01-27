package me.roundaround.custompaintings.util;

import java.util.List;

import net.minecraft.util.Pair;

public record Migration(
    String id,
    String packId,
    int index,
    List<Pair<String, String>> pairs) {
}
