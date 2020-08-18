package cd20;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class App {
  private Scanner scanner;

  public App(Reader reader) {
    this.scanner = new Scanner(reader);
  }

  public void run() throws IOException {
    while (!scanner.eof()) {
      Token token = scanner.nextToken();
      System.out.println(token);
    }
  }

  public static void main(String[] args) {
    Reader reader;

    if (args.length < 1) {
      // Read from stdin
      System.out.println("Reading from stdin...");
      reader = new InputStreamReader(System.in);
    } else {
      // Read from file at path
      System.out.println("Reading from file...");
      File file = new File(args[0]);

      try {
        reader = new FileReader(file);
      } catch (FileNotFoundException exception) {
        System.err.println("File not found: '" + args[0] + "'");
        return;
      }
    }

    try {
      new App(reader).run();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
