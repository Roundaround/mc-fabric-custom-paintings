package me.roundaround.custompaintings.resource;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import me.roundaround.custompaintings.entity.decoration.painting.PackData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record PackResource(Integer format, String id, String name, String description, String sourceLegacyPack,
                           List<PaintingResource> paintings, List<MigrationResource> migrations) {
  public PackData toData(String packFileUid) {
    return new PackData(packFileUid, this.id(), this.name(), this.description(), this.sourceLegacyPack(),
        this.paintings().stream().map((painting) -> painting.toData(this.id())).toList(),
        this.migrations().stream().map((migration) -> migration.toData(this.id())).toList()
    );
  }

  public static class TypeAdapter extends com.google.gson.TypeAdapter<PackResource> {
    private final Gson gson = new Gson();
    private final com.google.gson.TypeAdapter<PaintingResource> paintingAdapter = this.gson.getAdapter(
        PaintingResource.class);
    private final com.google.gson.TypeAdapter<MigrationResource> migrationAdapter = this.gson.getAdapter(
        MigrationResource.class);

    @Override
    public void write(JsonWriter out, PackResource value) throws IOException {
      out.beginObject();

      out.name("format");
      out.value(value.format());

      out.name("id");
      out.value(value.id());

      out.name("name");
      out.value(value.name());

      if (value.description() != null && !value.description().isBlank()) {
        out.name("description");
        out.value(value.description());
      }

      if (value.sourceLegacyPack() != null && !value.sourceLegacyPack().isBlank()) {
        out.name("sourceLegacyPack");
        out.value(value.sourceLegacyPack());
      }

      out.name("paintings");
      out.beginArray();
      if (value.paintings() != null) {
        for (PaintingResource painting : value.paintings()) {
          this.paintingAdapter.write(out, painting);
        }
      }
      out.endArray();

      if (value.migrations() != null && !value.migrations().isEmpty()) {
        out.name("migrations");
        out.beginArray();
        for (MigrationResource migration : value.migrations()) {
          this.migrationAdapter.write(out, migration);
        }
        out.endArray();
      }

      out.endObject();
    }

    @Override
    public PackResource read(JsonReader in) throws IOException {
      int format = 1;
      String id = "";
      String name = "";
      String description = null;
      String legacyPackId = null;
      ArrayList<PaintingResource> paintings = new ArrayList<>();
      ArrayList<MigrationResource> migrations = new ArrayList<>();

      in.beginObject();
      while (in.hasNext()) {
        switch (in.nextName()) {
          case "format":
            if (in.peek() == JsonToken.NULL) {
              in.nextNull();
            } else {
              format = in.nextInt();
            }
            break;
          case "id":
            id = in.nextString();
            break;
          case "name":
            name = in.nextString();
            break;
          case "description":
            if (in.peek() == JsonToken.NULL) {
              in.nextNull();
            } else {
              description = in.nextString();
            }
            break;
          case "sourceLegacyPack":
            if (in.peek() == JsonToken.NULL) {
              in.nextNull();
            } else {
              legacyPackId = in.nextString();
            }
            break;
          case "paintings":
            if (in.peek() == JsonToken.NULL) {
              in.nextNull();
            } else {
              in.beginArray();
              while (in.hasNext()) {
                paintings.add(this.paintingAdapter.read(in));
              }
              in.endArray();
            }
            break;
          case "migrations":
            if (in.peek() == JsonToken.NULL) {
              in.nextNull();
            } else {
              in.beginArray();
              while (in.hasNext()) {
                migrations.add(this.migrationAdapter.read(in));
              }
              in.endArray();
            }
            break;
          default:
            in.skipValue();
        }
      }
      in.endObject();

      return new PackResource(
          format, id, name, description, legacyPackId, List.copyOf(paintings), List.copyOf(migrations));
    }
  }
}
