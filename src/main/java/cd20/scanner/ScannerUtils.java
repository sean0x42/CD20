package cd20.scanner;

import java.util.Set;

public class ScannerUtils {
  private static final Set<Character> specialChars = Set.of(
    ',', '[', ']', '(', ')', '=', '+', '-', '*',
    '%', '^', '<', '>', ':', '!', ';', '.'
  );

  /**
   * Determines whether the given character is whitespace, and correctly handles
   * null.
   */
  public static boolean isWhitespace(Character ch) {
    return ch == null || Character.isWhitespace(ch);
  }

  /**
   * Determines whether the given character is a special operator/delimiter
   * character in CD20.
   */
  public static boolean isSpecialCharacter(Character ch) {
    if (ch == null) return false;

    if (specialChars.contains(ch)) {
      return true;
    }

    return false;
  }
}
