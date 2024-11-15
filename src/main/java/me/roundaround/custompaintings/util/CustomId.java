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

  private static final Predicate<String> IS_VALID_PART = Pattern.compile("^[\\s:'\"]+$").asMatchPredicate();
  private static final Predicate<String> IS_VALID_ID = Pattern.compile("^(?:[\\s:'\"]+:)?[\\s:'\"]+$").asMatchPredicate();

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
    return new Identifier(this.pack(), this.resource());
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
}
