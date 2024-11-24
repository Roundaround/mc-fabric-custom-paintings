package me.roundaround.custompaintings.util;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public record CustomId(String pack, String resource) implements Comparable<CustomId> {
  public static final PacketCodec<PacketByteBuf, CustomId> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.STRING, CustomId::pack, PacketCodecs.STRING, CustomId::resource, CustomId::new);

  private static final Predicate<String> IS_VALID_PART = Pattern.compile("^[^\\s:'\"]+$").asMatchPredicate();
  private static final Predicate<String> IS_VALID_ID = Pattern.compile("^(?:[^\\s:'\"]+:)?[^\\s:'\"]+$")
      .asMatchPredicate();
  private static final Predicate<String> IS_VALID_SHAPE = Pattern.compile("^(?:[^:]+:)?[^:]+$").asMatchPredicate();

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof CustomId that))
      return false;
    return Objects.equals(this.pack, that.pack) && Objects.equals(this.resource, that.resource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.pack, this.resource);
  }

  @Override
  public int compareTo(@NotNull CustomId other) {
    int i = this.resource.compareTo(other.resource);
    if (i == 0) {
      i = this.pack.compareTo(other.pack);
    }

    return i;
  }

  @Override
  public String toString() {
    return String.format("%s:%s", this.pack(), this.resource());
  }

  public Identifier toIdentifier() {
    return Identifier.of(this.pack(), this.resource());
  }

  public String toTranslationKey() {
    return String.format("%s.%s", this.pack(), this.resource());
  }

  public String toTranslationKey(String prefix) {
    return prefix + "." + this.toTranslationKey();
  }

  public String toTranslationKey(String prefix, String suffix) {
    return prefix + "." + this.toTranslationKey() + "." + suffix;
  }

  public void write(PacketByteBuf buf) {
    PACKET_CODEC.encode(buf, this);
  }

  public static CustomId read(PacketByteBuf buf) {
    return PACKET_CODEC.decode(buf);
  }

  public static CustomId from(Identifier identifier) {
    return new CustomId(identifier.getNamespace(), identifier.getPath());
  }

  public static CustomId parse(String value) {
    int separatorIndex = value.indexOf(":");
    if (separatorIndex == -1) {
      return new CustomId(Identifier.DEFAULT_NAMESPACE, value);
    }
    return new CustomId(value.substring(0, separatorIndex), value.substring(separatorIndex + 1));
  }

  public static boolean isPartValid(String part) {
    return IS_VALID_PART.test(part);
  }

  public static boolean isValid(String value) {
    return IS_VALID_ID.test(value);
  }

  private static void validatePartOrThrow(String part) throws IllegalArgumentException {
    if (part == null) {
      throw new IllegalArgumentException("Cannot be null");
    }
    if (part.isEmpty()) {
      throw new IllegalArgumentException("Must be at least 1 character");
    }
    if (!IS_VALID_PART.test(part)) {
      throw new IllegalArgumentException("Cannot contain any of [:'\"]");
    }
  }

  public static void validatePart(String part) throws InvalidIdException {
    try {
      validatePartOrThrow(part);
    } catch (IllegalArgumentException e) {
      throw new InvalidIdException(part, e);
    }
  }

  public static void validatePart(String part, String resource) throws InvalidIdException {
    try {
      validatePartOrThrow(part);
    } catch (IllegalArgumentException e) {
      throw new InvalidIdException(part, resource, e);
    }
  }

  private static void validateOrThrow(String value) throws IllegalArgumentException {
    if (value == null) {
      throw new IllegalArgumentException("Cannot be null");
    }

    if (!IS_VALID_SHAPE.test(value)) {
      throw new IllegalArgumentException("Must either be a single ID (no ':') or a full ID in \"first:second\" format");
    }

    if ((!value.contains(":") && !IS_VALID_PART.test(value)) || !IS_VALID_ID.test(value)) {
      throw new IllegalArgumentException("Cannot contain single or double quotes ('\")");
    }
  }

  public static void validate(String part) throws InvalidIdException {
    try {
      validateOrThrow(part);
    } catch (IllegalArgumentException e) {
      throw new InvalidIdException(part, e);
    }
  }

  public static void validate(String part, String resource) throws InvalidIdException {
    try {
      validateOrThrow(part);
    } catch (IllegalArgumentException e) {
      throw new InvalidIdException(part, resource, e);
    }
  }
}
