package me.roundaround.custompaintings.resource.file;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;

import io.netty.buffer.ByteBuf;
import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.network.codec.PacketCodec;

public record Image(Color[] pixels, int width, int height, String hash) {
  public static final PacketCodec<ByteBuf, Image> PACKET_CODEC = PacketCodec.of(
      Image::writeToByteBuf, Image::fromByteBuf);

  public static Image empty() {
    return new Image(new Color[0], 0, 0, "");
  }

  public static Image fromBytes(byte[] bytes, int width, int height) {
    int size = width * height;
    if (bytes.length < size * 4) {
      bytes = Arrays.copyOf(bytes, size * 4);
    }
    Color[] pixels = new Color[size];
    for (int i = 0; i < size; i++) {
      int ref = i * 4;
      pixels[i] = new Color(bytes[ref], bytes[ref + 1], bytes[ref + 2], bytes[ref + 3]);
    }
    return new Image(pixels, width, height, getHash(bytes));
  }

  public static Image fromPixels(Color[] pixels, int width, int height) {
    return new Image(pixels, width, height, getHash(pixels));
  }

  public static Image read(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    Color[] pixels = new Color[width * height];

    // Translate the pixels (read in as rows, stored as columns) and parse them into
    // Color objects.
    int[] rawARGB = image.getRGB(0, 0, width, height, null, 0, width);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        pixels[getIndex(height, x, y)] = Color.fromARGB(rawARGB[y * width + x]);
      }
    }

    return new Image(pixels, width, height, getHash(pixels));
  }

  public static Image read(InputStream stream) throws IOException {
    BufferedImage image = ImageIO.read(stream);
    if (image == null) {
      return null;
    }

    return read(image);
  }

  public static Image read(byte[] bytes) throws IOException {
    return read(new ByteArrayInputStream(bytes));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Image image)) {
      return false;
    }
    return this.hash.equals(image.hash) && this.width == image.width && this.height == image.height;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.hash, this.width, this.height);
  }

  public BufferedImage toBufferedImage() {
    BufferedImage bufferedImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
    for (int x = 0; x < this.width; x++) {
      for (int y = 0; y < this.height; y++) {
        bufferedImage.setRGB(x, y, this.getARGB(x, y));
      }
    }
    return bufferedImage;
  }

  public NativeImage toNativeImage() {
    NativeImage nativeImage = new NativeImage(this.width(), this.height(), false);
    for (int x = 0; x < this.width(); x++) {
      for (int y = 0; y < this.height(); y++) {
        nativeImage.setColorArgb(x, y, this.getARGB(x, y));
      }
    }
    return nativeImage;
  }

  public byte[] getBytes() {
    return getBytes(this.pixels);
  }

  public int getSize() {
    return this.pixels.length * 4;
  }

  public ByteSource getByteSource() {
    return ByteSource.wrap(this.getBytes());
  }

  public boolean isEmpty() {
    return this.width == 0 || this.height == 0;
  }

  public int getARGB(int x, int y) {
    return this.getColorInt(x, y, Color::getARGB);
  }

  public int getABGR(int x, int y) {
    return this.getColorInt(x, y, Color::getABGR);
  }

  private int getColorInt(int x, int y, Function<Color, Integer> supplier) {
    int index = this.getIndex(x, y);
    Color pixel = this.pixels[index];
    if (pixel == null) {
      return 0;
    }
    return supplier.apply(pixel);
  }

  private int getIndex(int x, int y) {
    return getIndex(this.height, x, y);
  }

  private static int getIndex(int height, int x, int y) {
    return (x * height + y);
  }

  private static byte[] getBytes(Color[] pixels) {
    byte[] bytes = new byte[pixels.length * 4];
    for (int i = 0; i < pixels.length; i++) {
      bytes[i * 4] = pixels[i].r;
      bytes[i * 4 + 1] = pixels[i].g;
      bytes[i * 4 + 2] = pixels[i].b;
      bytes[i * 4 + 3] = pixels[i].a;
    }
    return bytes;
  }

  private static String getHash(byte[] bytes) {
    try {
      return ByteSource.wrap(bytes).hash(Hashing.sha256()).toString();
    } catch (IOException e) {
      CustomPaintingsMod.LOGGER.warn("Exception raised while generating hash.", e);
      return "";
    }
  }

  private static String getHash(Color[] pixels) {
    return getHash(getBytes(pixels));
  }

  public void writeToByteBuf(ByteBuf buf) {
    buf.writeInt(this.width);
    buf.writeInt(this.height);
    buf.writeBytes(this.getBytes());
  }

  public static Image fromByteBuf(ByteBuf buf) {
    int width = buf.readInt();
    int height = buf.readInt();
    byte[] bytes = new byte[width * height * 4];
    buf.readBytes(bytes);
    return fromBytes(bytes, width, height);
  }

  public record Color(byte r, byte g, byte b, byte a) {
    public Color(int r, int g, int b, int a) {
      this((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public static Color fromARGB(int color) {
      return new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    public int getARGB() {
      return getAsInt(this.a, this.r, this.g, this.b);
    }

    public int getABGR() {
      return getAsInt(this.a, this.b, this.g, this.r);
    }

    public Color invert() {
      return new Color(255 - this.r, 255 - this.g, 255 - this.b, this.a);
    }

    private static int getAsInt(byte first, byte second, byte third, byte fourth) {
      return ((first & 0xFF) << 24) | ((second & 0xFF) << 16) | ((third & 0xFF) << 8) | (fourth & 0xFF);
    }
  }
}
