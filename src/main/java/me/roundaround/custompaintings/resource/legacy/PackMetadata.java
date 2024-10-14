package me.roundaround.custompaintings.resource.legacy;

import me.roundaround.custompaintings.resource.Image;

public record PackMetadata(LegacyPackResource pack, Image icon, boolean converted, boolean ignored) {
}
