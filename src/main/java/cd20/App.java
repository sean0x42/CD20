package cd20;

import java.io.IOException;
import java.io.Reader;

import cd20.output.OutputController;
import cd20.scanner.Scanner;
import cd20.scanner.Token;
import cd20.scanner.TokenType;

public class App {
  private Scanner scanner;
  private OutputController outputController = new OutputController();

  public App(Reader reader) {
    this.scanner = new Scanner(reader, outputController);
  }

  /**
   * Run the application
   */
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
}
