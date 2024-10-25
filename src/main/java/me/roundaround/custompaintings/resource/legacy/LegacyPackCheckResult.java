package me.roundaround.custompaintings.resource.legacy;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

public record LegacyPackCheckResult(Collection<PackMetadata> metas, HashMap<String, Path> globalConvertedIds,
                                    HashMap<String, Path> worldConvertedIds) {
}
