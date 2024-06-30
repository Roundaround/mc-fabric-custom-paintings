package me.roundaround.custompaintings.resource;

import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class CustomPaintingsResourcePack implements ResourcePack {
  @Nullable
  @Override
  public InputSupplier<InputStream> openRoot(String... segments) {
    return null;
  }

  @Nullable
  @Override
  public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
    return null;
  }

  @Override
  public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {

  }

  @Override
  public Set<String> getNamespaces(ResourceType type) {
    return Set.of();
  }

  @Nullable
  @Override
  public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
    return null;
  }

  @Override
  public ResourcePackInfo getInfo() {
    return null;
  }

  @Override
  public void close() {

  }
}
