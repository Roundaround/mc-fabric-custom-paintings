package me.roundaround.custompaintings.resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public record PaintingImage(Color[] pixels, int width, int height) {
  public static PaintingImage read(InputStream stream) throws IOException {
    BufferedImage image = ImageIO.read(stream);
    if (image == null) {
      throw new IOException("Invalid painting image file");
    }

    int width = image.getWidth();
    int height = image.getHeight();
    Color[] pixels = new Color[width * height];

    // Translate the pixels (read in as rows, stored as columns) and parse them into Color objects.
    int[] rawARGB = image.getRGB(0, 0, width, height, null, 0, width);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        pixels[getIndex(height, x, y)] = Color.fromARGB(rawARGB[y * width + x]);
      }
    }

    return new PaintingImage(pixels, width, height);
  }

  public static PaintingImage read(byte[] bytes) throws IOException {
    return read(new ByteArrayInputStream(bytes));
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

  public void write(File file) {
    try {
      ImageIO.write(this.toBufferedImage(), "png", file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

    private static int getAsInt(byte first, byte second, byte third, byte fourth) {
      return ((first & 0xFF) << 24) | ((second & 0xFF) << 16) | ((third & 0xFF) << 8) | (fourth & 0xFF);
    }
  }
}
