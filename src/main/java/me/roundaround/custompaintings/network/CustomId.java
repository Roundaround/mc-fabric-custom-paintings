package me.roundaround.custompaintings.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record CustomId(String pack, String resource) implements Comparable<CustomId> {
  public static final PacketCodec<PacketByteBuf, CustomId> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.STRING, CustomId::pack, PacketCodecs.STRING, CustomId::resource, CustomId::new);

  private static final String ENC_COLON = ":colon:";

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof CustomId that))
      return false;
    return Objects.equals(pack, that.pack) && Objects.equals(resource, that.resource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pack, resource);
  }

  @Override
  public int compareTo(@NotNull CustomId other) {
    int i = this.resource.compareTo(other.resource);
    if (i == 0) {
      i = this.pack.compareTo(other.pack);
    }

    return i;
  }

  public String getString() {
    return String.format("%s:%s", encode(this.pack()), encode(this.resource()));
  }

  public Identifier toIdentifier() {
    return new Identifier(pack(), resource());
  }

  public String toTranslationKey() {
    return this.pack + "." + this.resource;
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
    int separatorIndex = getSeparatorIndex(value);
    if (separatorIndex == -1) {
      return new CustomId(Identifier.DEFAULT_NAMESPACE, decode(value));
    }
    String encPack = value.substring(0, separatorIndex);
    String encMigration = value.substring(separatorIndex + 1);
    return new CustomId(decode(encPack), decode(encMigration));
  }

  private static String encode(String value) {
    return value.replaceAll(":", ENC_COLON);
  }

  private static String decode(String value) {
    return value.replaceAll(ENC_COLON, ":");
  }

  private static int getSeparatorIndex(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (i + ENC_COLON.length() <= value.length() && value.startsWith(ENC_COLON, i)) {
        // Skip over the ":colon:" placeholder
        i += ENC_COLON.length() - 1;
      } else if (value.charAt(i) == ':') {
        // Found the first colon that is not part of the placeholder
        return i;
      }
    }
    return -1;
  }
}
