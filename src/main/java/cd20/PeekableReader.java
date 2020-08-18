package cd20;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeekableReader {
  private BufferedReader reader;
  private List<Character> buffer = new ArrayList<Character>();

  public PeekableReader(BufferedReader reader) {
    this.reader = reader;
  }

  public Character read() throws IOException {
    // Fetch from buffer first
    if (!buffer.isEmpty()) {
      return buffer.remove(0);
    }

    int ch = reader.read();

    if (ch == -1) {
      return null;
    }

    return Character.valueOf((char) ch);
  }

  public Character peek() throws IOException {
    return peek(1);
  }

  public Character peek(int n) throws IOException {
    // Buffer is not large enough, expand
    while (buffer.size() < n) {
      int ch = reader.read();

      if (ch == -1) {
        return null;
      }

      buffer.add(Character.valueOf((char) ch));
    }

    return buffer.get(n - 1);
  }

//   public boolean pattern(String pattern) throws IOException {
    
//   }
}
