package me.roundaround.custompaintings.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketDecoder;
import net.minecraft.network.codec.ValueFirstEncoder;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class CustomCodecs {
  private CustomCodecs() {
  }

  public static <T extends CustomPayload> PacketCodec<RegistryByteBuf, T> empty(Supplier<T> supplier) {
    return PacketCodec.of((val, buf) -> {
    }, (buf) -> supplier.get());
  }

  public static <B extends PacketByteBuf, V> PacketCodec<B, List<V>> forList(PacketCodec<PacketByteBuf, V> entryCodec) {
    return forList((value, buf) -> entryCodec.encode(buf, value), entryCodec::decode);
  }

  public static <B extends ByteBuf, V> PacketCodec<B, List<V>> forList(
      final ValueFirstEncoder<B, V> encoder, final PacketDecoder<B, V> decoder
  ) {
    return new PacketCodec<>() {
      @Override
      public List<V> decode(B buf) {
        int size = buf.readInt();
        List<V> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
          list.add(decoder.decode(buf));
        }
        return list;
      }

      @Override
      public void encode(B buf, List<V> list) {
        buf.writeInt(list.size());
        for (V entry : list) {
          encoder.encode(entry, buf);
        }
      }
    };
  }
}
