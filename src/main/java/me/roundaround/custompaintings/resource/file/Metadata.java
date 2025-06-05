package me.roundaround.custompaintings.resource.file;

import java.nio.file.Path;

public record Metadata(Path path, FileUid fileUid, Pack pack, Image icon, boolean isLegacy) {
}
