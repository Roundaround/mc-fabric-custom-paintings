package me.roundaround.custompaintings.resource.legacy;

public record LegacyPackId(boolean isFile, String filename, long lastModified) {
  public String asString() {
    return String.format("%s%s%s", this.isFile ? 1 : 0, fnv1aHash(this.filename), zeroPad(this.lastModified));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof LegacyPackId that))
      return false;
    return this.asString().equals(that.asString());
  }

  private static String fnv1aHash(String filename) {
    final long FNV_64_PRIME = 0x100000001b3L;
    long hash = 0xcbf29ce484222325L;
    for (byte b : filename.getBytes()) {
      hash ^= b;
      hash *= FNV_64_PRIME;
    }
    return String.format("%016x", hash);
  }

  private static String zeroPad(long lastModified) {
    return String.format("%020d", lastModified);
  }
}
