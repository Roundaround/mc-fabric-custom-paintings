package me.roundaround.custompaintings.resource;

import me.roundaround.custompaintings.util.CustomId;

import java.util.HashMap;

public record HashResult(String combinedImageHash, HashMap<CustomId, String> imageHashes) {
}
