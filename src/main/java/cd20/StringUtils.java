package cd20;

import java.util.Arrays;

public class StringUtils {
  /**
   * Generate a string by repeating a {@link Character}.
   */
  public static String repeat(char ch, int times) {
    char[] array = new char[times];
    Arrays.fill(array, ch);
    return new String(array);
  }

  /**
   * Left pad a {@link String} with space characters.
   */
  public static String leftPad(int width, String string) {
    // Some strings will already be long enough
    if (string.length() >= width) {
      return string;
    }

    // Pad with spaces
    StringBuilder builder = new StringBuilder();
    while (builder.length() < width - string.length()) {
      builder.append(' ');
    }

    // Add original string
    builder.append(string);

    return builder.toString();
  }
  
  /**
   * Right pad a {@link String} with space characters.
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
}
