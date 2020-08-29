import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import cd20.App;

public class A1 {
  public static void main(String[] args) {
    Reader reader;

    if (args.length < 1) {
      // Read from stdin
      reader = new InputStreamReader(System.in);
    } else {
      // Read from file at path
      File file = new File(args[0]);

      try {
        reader = new FileReader(file);
      } catch (FileNotFoundException exception) {
        System.err.println("File not found: '" + args[0] + "'");
        return;
      }
    }

    // Attempt to run app
    try {
      new App(reader).run();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
