package me.roundaround.custompaintings.resource;

import java.util.List;

public record PaintingPack(int format, String id, String name, List<PaintingResource> paintings) {
}
