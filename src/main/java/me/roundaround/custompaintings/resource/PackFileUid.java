package me.roundaround.custompaintings.resource;

public record PackFileUid(boolean isFile, String filename, long lastModified, long fileSize, String stringValue) {
  private static final int DIGITS_TIMESTAMP = 8;
  private static final int DIGITS_FILE_SIZE = 8;
  private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

  public PackFileUid(boolean isFile, String filename, long lastModified, long fileSize) {
    this(isFile, filename, lastModified, fileSize, getStringValue(isFile, filename, lastModified, fileSize));
  }

  public static String create(boolean isFile, String filename, long lastModified, long fileSize) {
    return new PackFileUid(isFile, filename, lastModified, fileSize).stringValue();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof PackFileUid that))
      return false;
    return this.stringValue().equals(that.stringValue());
  }

  private static String getStringValue(boolean isFile, String filename, long lastModified, long fileSize) {
    String fileBinary = String.valueOf(isFile ? 1 : 0);
    String nameHash = fnv1aHash(filename);
    String modifiedEncoded = zeroPad(toBase62(lastModified), DIGITS_TIMESTAMP);
    String sizeEncoded = zeroPad(toBase62(fileSize), DIGITS_FILE_SIZE);
    return String.format("%s%s%s%s", fileBinary, nameHash, modifiedEncoded, sizeEncoded);
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

  private static String toBase62(long number) {
    if (number == 0) {
      return "0";
    }

    StringBuilder encoded = new StringBuilder();
    while (number > 0) {
      int remainder = (int) (number % 62);
      encoded.insert(0, BASE62_CHARS.charAt(remainder));
      number /= 62;
    }

    return encoded.toString();
  }
}
