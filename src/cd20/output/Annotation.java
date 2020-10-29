package cd20.output;

import java.util.StringJoiner;

import cd20.StringUtils;
import cd20.scanner.Token;

/**
 * A source code annotation for printing error messages and warnings.
 */
public class Annotation {
  private final int line;
  private final int column;
  private final int length;
  private final String annotation;

  public Annotation(String annotation, Token token) {
    this(token.getLine(), token.getColumn(), token.getWidth(), annotation);
  }

  public Annotation(int line, int column, int length, String annotation) {
    this.line = line;
    this.column = column;
    this.length = length;
    this.annotation = annotation;
  }

  /**
   * Prepare this annotation for printing by converting it to a String.
   * @param lineNumberWidth Width of the line number column to include in
   * padding.
   */
  public String format(int lineNumberWidth) {
    StringJoiner joiner = new StringJoiner("\n");

    // Create indent strings
    String smallIndent = StringUtils.repeat(' ', lineNumberWidth + 3);
    String largeIndent = StringUtils.repeat(' ', lineNumberWidth + column + 2);

    // Generate arrow
    joiner.add(largeIndent + StringUtils.repeat('^', length));

    // Split at newlines and render each line
    String[] strs = annotation.split("\n");
    for (int idx = 0; idx < strs.length; idx++) {
      joiner.add(smallIndent + strs[idx]);

      // Print line number on final line
      if (idx == strs.length - 1) {
        joiner.add(smallIndent + "(" + line + ":" + column + ")\n");
      }
    }

    return joiner.toString();
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public int getLength() {
    return length;
  }

  public String getAnnotation() {
    return annotation;
  }
}
