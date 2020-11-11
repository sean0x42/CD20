package cd20.codegen.generators;

import java.util.StringJoiner;

public abstract class LineGenerator<T> implements Generator<T> {
  int lines = 0;
  StringJoiner joiner = new StringJoiner("\n");

  /**
   * Insert a new line.
   * @param line Line to insert.
   */
  public void insertLine(String line) {
    lines++;
    joiner.add(line);
  }

  public int getSize() {
    return lines;
  }

  public String getBody() {
    return joiner.toString();
  }
}
