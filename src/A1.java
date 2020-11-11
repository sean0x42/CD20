import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import cd20.output.ListingGenerator;
import cd20.scanner.Scanner;
import cd20.scanner.Token;
import cd20.scanner.TokenType;

public class A1 {
  private Scanner scanner;
  private ListingGenerator outputController = new ListingGenerator();

  public A1(Reader reader) {
    this.scanner = new Scanner(reader, outputController);
  }

  public void run() throws IOException {
    int lineWidth = 0;

    while (!scanner.eof()) {
      Token token = scanner.nextToken();

      // Wrap to new line
      if (lineWidth > 60 || token.getType() == TokenType.UNDEFINED) {
        lineWidth = 0;
        System.out.println();
      }

      // Handle lexical errors
      if (token.getType() == TokenType.UNDEFINED) {
        System.out.println(token.getType());
        System.out.print("lexical error ");
        System.out.println(token.getLexeme());
        continue;
      }

      // Print
      String tokenStr = token.toString();
      lineWidth += tokenStr.length();
      System.out.print(tokenStr);
    }

    System.out.println('\n');
    System.out.println(outputController.toString());
    outputController.writeToFile("listing.txt");
  }

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
      new A1(reader).run();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
