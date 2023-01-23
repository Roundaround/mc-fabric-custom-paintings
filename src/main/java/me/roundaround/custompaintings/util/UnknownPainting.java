package me.roundaround.custompaintings.util;

import net.minecraft.util.Identifier;

public record UnknownPainting(
    Identifier id,
    int count,
    Identifier autoFixId) {
}
