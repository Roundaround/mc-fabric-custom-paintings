package me.roundaround.custompaintings.resource.legacy;

import java.util.List;

public record LegacyPackResource(String filename, String packId, String name, String description,
                                 List<LegacyPaintingResource> paintings, List<LegacyMigrationResource> migrations) {
}
