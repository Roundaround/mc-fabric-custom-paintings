package me.roundaround.custompaintings.resource.file;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import javax.imageio.ImageIO;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;

import io.netty.buffer.ByteBuf;
import me.roundaround.custompaintings.CustomPaintingsMod;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.text.Text;

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

  public Color getPixel(int x, int y) {
    return this.getPixel(x, y, Color.transparent());
  }

  public Color getPixel(int x, int y, Color empty) {
    if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
      return empty;
    }
    return this.pixels[getIndex(this.height, x, y)];
  }

  public Image apply(Operation... operations) {
    Hashless hashless = Hashless.fromImage(this);
    for (Operation operation : operations) {
      hashless = operation.apply(hashless);
    }
    return hashless.toImage();
  }

  public Image apply(Iterable<Operation> operations) {
    Hashless hashless = Hashless.fromImage(this);
    for (Operation operation : operations) {
      hashless = operation.apply(hashless);
    }
    return hashless.toImage();
  }

  public static int getIndex(int height, int x, int y) {
    return (x * height + y);
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

  public static Color[] generateEmpty(int width, int height) {
    return generateEmpty(width * height);
  }

  public static Color[] generateEmpty(int size) {
    Color[] pixels = new Color[size];
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = Color.transparent();
    }
    return pixels;
  }

  public record Color(byte r, byte g, byte b, byte a) {
    public Color(int r, int g, int b, int a) {
      this((byte) r, (byte) g, (byte) b, (byte) a);
    }

    public static Color fromARGB(int color) {
      return new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    public static Color transparent() {
      return new Color(0, 0, 0, 0);
    }

    public Color copy() {
      return new Color(this.r, this.g, this.b, this.a);
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

    public Color darken(float amount) {
      byte byteAmount = (byte) (amount * 255);
      return new Color(
          (byte) Math.max(0, this.r - byteAmount),
          (byte) Math.max(0, this.g - byteAmount),
          (byte) Math.max(0, this.b - byteAmount),
          this.a);
    }

    public Color removeAlpha() {
      return new Color(this.r, this.g, this.b, (byte) 255);
    }

    public boolean isTransparent() {
      return this.a == 0;
    }

    public float getRedFloat() {
      return this.r / 255f;
    }

    public float getGreenFloat() {
      return this.g / 255f;
    }

    public float getBlueFloat() {
      return this.b / 255f;
    }

    public float getAlphaFloat() {
      if (this.a == -1) {
        return 1f;
      }
      return this.a / 255f;
    }

    public static Color layer(Color top, Color bottom) {
      if (top.isTransparent()) {
        return bottom;
      }
      if (bottom.isTransparent()) {
        return top;
      }

      float topAlpha = top.a / 255f;
      float bottomAlpha = bottom.a / 255f;

      float alpha = topAlpha + bottomAlpha * (1 - topAlpha);

      return new Color(
          (byte) (top.r * topAlpha + bottom.r * bottomAlpha * (1 - topAlpha) / alpha),
          (byte) (top.g * topAlpha + bottom.g * bottomAlpha * (1 - topAlpha) / alpha),
          (byte) (top.b * topAlpha + bottom.b * bottomAlpha * (1 - topAlpha) / alpha),
          (byte) alpha);
    }

    public static Color average(Collection<Color> colors) {
      if (colors.isEmpty()) {
        return transparent();
      }

      long red = 0;
      long green = 0;
      long blue = 0;
      long alpha = 0;
      for (Color color : colors) {
        red += color.r;
        green += color.g;
        blue += color.b;
        alpha += color.a;
      }
      return new Color(
          (byte) (red / colors.size()),
          (byte) (green / colors.size()),
          (byte) (blue / colors.size()),
          (byte) (alpha / colors.size()));
    }

    private static int getAsInt(byte first, byte second, byte third, byte fourth) {
      return ((first & 0xFF) << 24) | ((second & 0xFF) << 16) | ((third & 0xFF) << 8) | (fourth & 0xFF);
    }
  }

  public record Hashless(Color[] pixels, int width, int height) {
    public Image toImage() {
      return new Image(this.pixels, this.width, this.height, getHash(this.pixels));
    }

    public static Hashless fromImage(Image image) {
      return new Hashless(image.pixels, image.width, image.height);
    }

    public Color getPixel(int x, int y) {
      return this.getPixel(x, y, Color.transparent());
    }

    public Color getPixel(int x, int y, Color empty) {
      if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
        return empty;
      }
      return this.pixels[getIndex(this.height, x, y)];
    }

    public Color[] copyPixels() {
      Color[] colors = new Color[this.pixels.length];
      for (int i = 0; i < this.pixels.length; i++) {
        colors[i] = this.pixels[i].copy();
      }
      return colors;
    }
  }

  public interface Operation {
    Text getName();

    Hashless apply(Hashless image);

    static Operation invert() {
      return new Operation() {
        @Override
        public Text getName() {
          // TODO: i18n
          return Text.of("Invert");
        }

        @Override
        public Hashless apply(Hashless source) {
          Color[] pixels = new Color[source.pixels.length];
          for (int i = 0; i < source.pixels.length; i++) {
            pixels[i] = source.pixels[i].invert();
          }
          return new Hashless(pixels, source.width, source.height);
        }
      };
    }

    static Operation scale(int width, int height) {
      return new Operation() {
        @Override
        public Text getName() {
          return Text.of("Scale");
        }

        @Override
        public Hashless apply(Hashless source) {
          Color[] pixels = new Color[width * height];
          for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
              pixels[getIndex(height, x, y)] = resample(source, width, height, x, y);
            }
          }
          return new Hashless(pixels, width, height);
        }

        private static Color resample(
            Hashless source,
            int targetWidth,
            int targetHeight,
            int x,
            int y) {
          int sourceX = (int) (x * (source.width / (double) targetWidth));
          int sourceY = (int) (y * (source.height / (double) targetHeight));
          return source.pixels[getIndex(source.height, sourceX, sourceY)];
        }
      };
    }

    static Operation translate(int x, int y) {
      return translate(x, y, Color.transparent());
    }

    static Operation translate(int x, int y, Color empty) {
      return new Operation() {
        @Override
        public Text getName() {
          return Text.of("Translate");
        }

        @Override
        public Hashless apply(Hashless source) {
          Color[] pixels = new Color[source.pixels.length];
          for (int cx = 0; cx < source.width; cx++) {
            for (int cy = 0; cy < source.height; cy++) {
              pixels[getIndex(source.height, cx, cy)] = source.getPixel(cx - x, cy - y, empty);
            }
          }
          return new Hashless(pixels, source.width, source.height);
        }
      };
    }

    static Operation resize(int width, int height) {
      return resize(width, height, Color.transparent());
    }

    static Operation resize(int width, int height, Color empty) {
      return new Operation() {
        @Override
        public Text getName() {
          return Text.of("Resize");
        }

        @Override
        public Hashless apply(Hashless source) {
          Color[] pixels = new Color[width * height];
          for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
              pixels[getIndex(height, x, y)] = source.getPixel(x, y, empty);
            }
          }
          return new Hashless(pixels, width, height);
        }
      };
    }

    static Operation embed(Image image, int x, int y) {
      return embed(image, x, y, false);
    }

    static Operation embed(Image image, int x, int y, boolean behind) {
      return new Operation() {
        @Override
        public Text getName() {
          return Text.of("Embed");
        }

        @Override
        public Hashless apply(Hashless source) {
          Color[] pixels = source.copyPixels();
          for (int ix = 0; ix < image.width; ix++) {
            int sx = ix + x;
            if (sx < 0 || sx >= source.width) {
              continue;
            }

            for (int iy = 0; iy < image.height; iy++) {
              int sy = iy + y;
              if (sy < 0 || sy >= source.height) {
                continue;
              }

              Color sourceColor = source.getPixel(sx, sy);
              Color imageColor = image.getPixel(ix, iy);
              pixels[getIndex(source.height, sx, sy)] = Color.layer(imageColor, sourceColor);
            }
          }

          return new Hashless(pixels, source.width, source.height);
        }
      };
    }
  }
}
