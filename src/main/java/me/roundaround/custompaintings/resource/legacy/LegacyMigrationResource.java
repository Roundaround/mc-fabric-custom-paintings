package me.roundaround.custompaintings.resource.legacy;

import net.minecraft.util.Pair;

import java.util.List;

public record LegacyMigrationResource(String id, String description, List<Pair<String, String>> pairs) {
}
