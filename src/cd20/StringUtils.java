package cd20;

import java.util.Arrays;

public class StringUtils {
  /**
   * Generate a string by repeating a Character.
   */
  public static String repeat(char ch, int times) {
    char[] array = new char[times];
    Arrays.fill(array, ch);
    return new String(array);
  }

  /**
   * Left pad a String with space characters.
   */
  public static String leftPad(int width, String string) {
    return StringUtils.leftPad(width, string, ' ');
  }

  /**
   * Left pad a String.
   * @param width Minium string width.
   * @param string String to pad.
   * @param padChar Character to pad string with.
   */
  public static String leftPad(int width, String string, char padChar) {
    // Some strings will already be long enough
    if (string.length() >= width) {
      return string;
    }

    // Pad with padChar
    StringBuilder builder = new StringBuilder();
    while (builder.length() < width - string.length()) {
      builder.append(padChar);
    }

    // Add original string
    builder.append(string);

    return builder.toString();
  }
  
  /**
   * Right pad a String with space characters.
   */
  public static String rightPad(int width, String string) {
    // Some strings will already be long enough
    if (string.length() >= width) {
      return string;
    }

    // Add original string
    StringBuilder builder = new StringBuilder();
    builder.append(string);

    // Pad with spaces
    while (builder.length() < width) {
      builder.append(' ');
    }

    return builder.toString();
  }

  /**
   * Strips a file extension from a path
   * @param path Path to strip extension from.
   */
  public static String stripExtension(String path) {
    if (!path.contains(".")) {
      return path;
    }

    return path.substring(0, path.indexOf('.'));
  }
}
