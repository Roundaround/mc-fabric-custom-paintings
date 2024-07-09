package me.roundaround.custompaintings.resource;

import java.util.List;

public record PackResource(Integer format, String id, String name, List<PaintingResource> paintings) {
}
