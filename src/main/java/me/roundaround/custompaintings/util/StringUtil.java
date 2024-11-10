package me.roundaround.custompaintings.util;

import java.text.DecimalFormat;

public class StringUtil {
  private static final double KB = 1024;
  private static final double MB = KB * 1024;
  private static final double GB = MB * 1024;
  private static final double TB = GB * 1024;

  private StringUtil() {
  }

  public static String formatBytes(long bytes) {
    if (bytes >= TB / 2) {
      return String.format("%s TB", formatToTwoDecimals(bytes / TB));
    } else if (bytes >= GB / 2) {
      return String.format("%s GB", formatToTwoDecimals(bytes / GB));
    } else if (bytes >= MB / 2) {
      return String.format("%s MB", formatToTwoDecimals(bytes / MB));
    } else if (bytes >= KB / 2) {
      return String.format("%s KB", formatToTwoDecimals(bytes / KB));
    }
    return bytes + " B";
  }

  public static String formatToTwoDecimals(double value) {
    return new DecimalFormat("0.##").format(value);
  }
}
