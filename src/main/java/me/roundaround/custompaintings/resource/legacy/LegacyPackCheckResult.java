package me.roundaround.custompaintings.resource.legacy;

import me.roundaround.custompaintings.resource.PackMetadata;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

public record LegacyPackCheckResult(Collection<PackMetadata<LegacyPackResource>> metas,
                                    HashMap<String, Path> globalConvertedIds, HashMap<String, Path> worldConvertedIds) {
}
