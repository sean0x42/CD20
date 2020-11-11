package cd20.codegen.generators;

import cd20.StringUtils;
import cd20.codegen.Operation;

public abstract class WordFilledGenerator<T> implements Generator<T> {
  int currentByte = 0;
  int lines = 0;
  StringBuilder builder = new StringBuilder();

  /**
   * Insert a byte.
   * @param bite Byte to insert.
   */
  public void insertByte(int bite) {
    if (lines == 0) {
      lines++;
    }

    // Append a new line once we run out space
    if (currentByte == 8) {
      builder.append('\n');
      currentByte = 0;
      lines++;
    }

    String formattedByte = StringUtils.leftPad(2, bite + "", '0');
    builder.append(StringUtils.leftPad(4, formattedByte, ' '));
    currentByte++;
  }

  /**
   * Fill remaining space on line with HALT ops (00).
   */
  public void fill() {
    if (lines == 0) return;

    while (currentByte < 8) {
      insertByte(Operation.HALT.getCode());
    }
  }

  public int getSize() {
    return lines;
  }

  public String getBody() {
    return builder.toString();
  }
}
