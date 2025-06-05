package me.roundaround.custompaintings.resource.legacy;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

import me.roundaround.custompaintings.resource.file.Metadata;

public record LegacyPackCheckResult(Collection<Metadata> metas,
    HashMap<String, Path> globalConvertedIds, HashMap<String, Path> worldConvertedIds) {
}
