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

  public Annotation(Token token, String annotation) {
    this(token.getLine(), token.getColumn(), token.getLexeme().length(), annotation); }

  public Annotation(int line, int column, String annotation) {
    this(line, column, 1, annotation);
  }

  public Annotation(int line, int column, int length, String annotation) {
    this.line = line;
    this.column = column;
    this.length = length;
    this.annotation = annotation;
  }

  /**
   * Prepare this annotation for printing by converting it to a String.
   */
  public String format(int lineNumberWidth) {
    // Init
    StringJoiner joiner = new StringJoiner("\n");
    String indent = StringUtils.repeat(' ', lineNumberWidth + column + 2);

    // Generate actual annotation
    // TODO clean up
    joiner.add(indent + StringUtils.repeat('^', length));
    joiner.add(indent + annotation + " (" + line + ":" + column + ")");

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
