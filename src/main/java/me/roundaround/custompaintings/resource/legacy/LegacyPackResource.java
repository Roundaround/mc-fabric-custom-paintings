package me.roundaround.custompaintings.resource.legacy;

import java.nio.file.Path;
import java.util.List;

public record LegacyPackResource(Path path, String packId, String name, String description,
                                 List<LegacyPaintingResource> paintings, List<LegacyMigrationResource> migrations) {
}
