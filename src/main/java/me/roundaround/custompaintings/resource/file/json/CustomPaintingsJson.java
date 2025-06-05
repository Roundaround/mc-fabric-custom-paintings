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
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.InvalidIdException;

public record CustomPaintingsJson(
    int format,
    String id,
    String name,
    String description,
    String sourceLegacyPack,
    List<Painting> paintings,
    List<Migration> migrations) {
  public void validateIds() throws InvalidIdException {
    CustomId.validatePart(this.id, "pack");

    int i = 0;
    for (Painting painting : this.paintings) {
      painting.validateId(i);
      i++;
    }

    i = 0;
    for (Migration migration : this.migrations) {
      migration.validateIds(i);
      i++;
    }
  }

  public static class TypeAdapter extends com.google.gson.TypeAdapter<CustomPaintingsJson> {
    private final Gson gson = new Gson();
    private final com.google.gson.TypeAdapter<Painting> paintingAdapter = gson.getAdapter(
        Painting.class);
    private final com.google.gson.TypeAdapter<Migration> migrationAdapter = gson.getAdapter(
        Migration.class);

    @Override
    public void write(JsonWriter out, CustomPaintingsJson value) throws IOException {
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
    public CustomPaintingsJson read(JsonReader in) throws IOException {
      int format = 1;
      String id = "";
      String name = "";
      String description = null;
      String legacyPackId = null;
      ArrayList<Painting> paintings = new ArrayList<>();
      ArrayList<Migration> migrations = new ArrayList<>();

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

      return new CustomPaintingsJson(
          format,
          id,
          name,
          description,
          legacyPackId,
          List.copyOf(paintings),
          List.copyOf(migrations));
    }
  }
}
