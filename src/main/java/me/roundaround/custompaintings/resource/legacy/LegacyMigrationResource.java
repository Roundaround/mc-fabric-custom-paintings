package me.roundaround.custompaintings.resource.legacy;

import java.util.List;

public record LegacyMigrationResource(String id, String description, List<List<String>> pairs) {
}
