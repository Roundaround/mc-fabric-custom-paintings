package me.roundaround.custompaintings.resource.legacy;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record CustomPaintingsJson(String id, String name, List<LegacyPaintingResource> paintings,
                                  List<LegacyMigrationResource> migrations) {
  public static class TypeAdapter extends com.google.gson.TypeAdapter<CustomPaintingsJson> {
    private final Gson gson = new Gson();
    private final com.google.gson.TypeAdapter<LegacyPaintingResource> paintingAdapter = gson.getAdapter(
        LegacyPaintingResource.class);
    private final com.google.gson.TypeAdapter<LegacyMigrationResource> migrationAdapter = gson.getAdapter(
        LegacyMigrationResource.class);

    @Override
    public void write(JsonWriter out, CustomPaintingsJson value) throws IOException {
      out.beginObject();

      out.name("id");
      out.value(value.id());

      out.name("name");
      out.value(value.name());

      out.name("paintings");
      out.beginArray();
      if (value.paintings() != null) {
        for (LegacyPaintingResource painting : value.paintings()) {
          this.paintingAdapter.write(out, painting);
        }
      }
      out.endArray();

      if (value.migrations() != null && !value.migrations().isEmpty()) {
        out.name("migrations");
        out.beginArray();
        for (LegacyMigrationResource migration : value.migrations()) {
          this.migrationAdapter.write(out, migration);
        }
        out.endArray();
      }

      out.endObject();
    }

    @Override
    public CustomPaintingsJson read(JsonReader in) throws IOException {
      String id = "";
      String name = "";
      ArrayList<LegacyPaintingResource> paintings = new ArrayList<>();
      ArrayList<LegacyMigrationResource> migrations = new ArrayList<>();

      in.beginObject();
      while (in.hasNext()) {
        switch (in.nextName()) {
          case "id":
            id = in.nextString();
            break;
          case "name":
            name = in.nextString();
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

      return new CustomPaintingsJson(id, name, List.copyOf(paintings), List.copyOf(migrations));
    }
  }
}
