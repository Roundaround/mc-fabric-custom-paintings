package me.roundaround.custompaintings.resource;

import net.minecraft.util.Identifier;

import java.util.HashMap;

public record HashResult(String combinedImageHash, HashMap<Identifier, String> imageHashes) {
}
