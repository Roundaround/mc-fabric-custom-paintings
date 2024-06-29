package me.roundaround.custompaintings.resource;

import java.util.List;

public record PaintingPack(String id, String name, List<PaintingResource> paintings, List<MigrationResource> migrations) {

}
