package cd20.output;

import java.util.List;
import java.util.Map;

import cd20.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages source code listing.
 */
public class OutputController {
  private List<String> lines;
  private Map<Integer, List<Annotation>> annotationMap;

  public OutputController() {
    lines = new ArrayList<>();
    annotationMap = new HashMap<>();
  }

  /**
   * Add a single {@link Character} to a line.
   *
   * Note: Characters must be added in order, and no line may be skipped.
   */
  public void addCharacter(int line, Character ch) {
    addString(line, ch.toString());
  }

  /**
   * Add a {@link String} to a line.
   *
   * Note: Strings must be added in order, and no line may be skipped.
   */
  public void addString(int line, String string) {
    if (lines.size() < line) {
      lines.add(line - 1, "");
    }

    lines.set(line - 1, lines.get(line - 1).concat(string));
  }

  /**
   * Add a new annotation.
   */
  public void addAnnotation(Annotation annotation) {
    int line = annotation.getLine() - 1;

    // Add new bucket if one doesn't exist
    if (!annotationMap.containsKey(line)) {
      annotationMap.put(line, new ArrayList<>());
    }

    annotationMap.get(line).add(annotation);
  }

  /**
   * Write the contents of the controller to a file.
   *
   * Warning: this will replace any existing file.
   */
  public void writeToFile(String path) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(path));
    writer.append(toString());
    writer.close();
  }

  /**
   * Turn all output into a single string.
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    int lineNumberWidth = String.valueOf(lines.size()).length();

    // Include each line
    for (int idx = 0; idx < lines.size(); idx++) {
      String line = lines.get(idx);
      String lineNumber = StringUtils.leftPad(lineNumberWidth, String.valueOf(idx + 1));

      builder.append(lineNumber + " | " + line);

      // Are there any annotationMap for this line?
      if (!annotationMap.containsKey(idx)) {
        continue;
      }

      // Append any annotationMap
      for (Annotation annotation : annotationMap.get(idx)) {
        builder.append(annotation.format(lineNumberWidth) + "\n");
      }
    }

    return builder.toString();
  }
}
