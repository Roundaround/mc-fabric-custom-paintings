package me.roundaround.custompaintings.resource.legacy;

public record LegacyPackId(boolean isFile, String filename, long lastModified, long fileSize) {
  private static final int DIGITS_TIMESTAMP = 8;
  private static final int DIGITS_FILE_SIZE = 8;

  public String asString() {
    String fileBinary = String.valueOf(this.isFile() ? 1 : 0);
    String nameHash = fnv1aHash(this.filename());
    String modifiedEncoded = zeroPad(base62(this.lastModified()), DIGITS_TIMESTAMP);
    String sizeEncoded = zeroPad(base62(this.fileSize()), DIGITS_FILE_SIZE);
    return String.format("%s%s%s%s", fileBinary, nameHash, modifiedEncoded, sizeEncoded);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof LegacyPackId that))
      return false;
    return this.asString().equals(that.asString());
  }

  private static String fnv1aHash(String value) {
    final long FNV_64_PRIME = 0x100000001b3L;
    long hash = 0xcbf29ce484222325L;
    for (byte b : value.getBytes()) {
      hash ^= b;
      hash *= FNV_64_PRIME;
    }
    return String.format("%016x", hash);
  }

  private static String zeroPad(String value, int digits) {
    return String.format("%" + digits + "s", value).replace(' ', '0');
  }

  private static String base62(long number) {
    String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    StringBuilder encoded = new StringBuilder();

    // Handle the special case of zero
    if (number == 0) {
      return "0";
    }

    // Convert the number to base62
    while (number > 0) {
      int remainder = (int) (number % 62);
      encoded.insert(0, chars.charAt(remainder));
      number /= 62;
    }

    return encoded.toString();
  }
}
