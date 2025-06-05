package me.roundaround.custompaintings.resource.file.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import me.roundaround.custompaintings.resource.file.Migration;
import me.roundaround.custompaintings.resource.file.Painting;

public record LegacyCustomPaintingsJson(
    String id,
    String name,
    List<Painting> paintings,
    List<Migration> migrations) {
  public static class TypeAdapter extends com.google.gson.TypeAdapter<LegacyCustomPaintingsJson> {
    private final Gson gson = new Gson();
    private final com.google.gson.TypeAdapter<Painting> paintingAdapter = gson.getAdapter(
        Painting.class);
    private final com.google.gson.TypeAdapter<Migration> migrationAdapter = gson.getAdapter(
        Migration.class);

    @Override
    public void write(JsonWriter out, LegacyCustomPaintingsJson value) throws IOException {
      out.beginObject();

      out.name("id");
      out.value(value.id());

      out.name("name");
      out.value(value.name());

      out.name("paintings");
      out.beginArray();
      if (value.paintings() != null) {
        for (Painting painting : value.paintings()) {
          this.paintingAdapter.write(out, painting);
        }
      }
      out.endArray();

      if (value.migrations() != null && !value.migrations().isEmpty()) {
        out.name("migrations");
        out.beginArray();
        for (Migration migration : value.migrations()) {
          this.migrationAdapter.write(out, migration);
        }
        out.endArray();
      }

      out.endObject();
    }

    @Override
    public LegacyCustomPaintingsJson read(JsonReader in) throws IOException {
      String id = "";
      String name = "";
      ArrayList<Painting> paintings = new ArrayList<>();
      ArrayList<Migration> migrations = new ArrayList<>();

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

      return new LegacyCustomPaintingsJson(
          id,
          name,
          List.copyOf(paintings),
          List.copyOf(migrations));
    }
  }
}
