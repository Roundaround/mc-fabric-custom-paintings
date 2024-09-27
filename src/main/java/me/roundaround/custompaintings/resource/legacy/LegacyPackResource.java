package me.roundaround.custompaintings.resource.legacy;

import java.util.List;

public record LegacyPackResource(String id, String name, List<LegacyPaintingResource> paintings,
                                 List<LegacyMigrationResource> migrations) {
}
