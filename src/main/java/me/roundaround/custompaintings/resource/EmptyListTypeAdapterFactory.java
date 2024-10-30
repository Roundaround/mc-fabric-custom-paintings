package me.roundaround.custompaintings.resource;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EmptyListTypeAdapterFactory implements TypeAdapterFactory {
  private static EmptyListTypeAdapterFactory instance = null;

  private EmptyListTypeAdapterFactory() {
  }

  public static EmptyListTypeAdapterFactory getInstance() {
    if (instance == null) {
      instance = new EmptyListTypeAdapterFactory();
    }
    return instance;
  }

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    if (!List.class.isAssignableFrom(type.getRawType())) {
      return null;
    }

    TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
    return new TypeAdapter<>() {
      @Override
      public void write(JsonWriter out, T value) throws IOException {
        if (!(value instanceof List<?> listValue) || listValue.isEmpty()) {
          out.nullValue();
          return;
        }
        delegate.write(out, value);
      }

      @SuppressWarnings("unchecked")
      @Override
      public T read(JsonReader in) throws IOException {
        T list = delegate.read(in);
        return list != null ? list : (T) Collections.emptyList();
      }
    };
  }
}