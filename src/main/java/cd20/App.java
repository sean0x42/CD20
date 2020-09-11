package cd20;

import java.io.IOException;
import java.io.Reader;

import cd20.output.OutputController;
import cd20.parser.Node;
import cd20.parser.Parser;
import cd20.scanner.Scanner;

public class App {
  private final Parser parser;
  private final Scanner scanner;
  private final OutputController outputController = new OutputController();

  public App(Reader reader) {
    this.scanner = new Scanner(reader, outputController);
    this.parser = new Parser(scanner, outputController);
  }

  /**
   * Run the application
   */
  public void run() throws IOException {
    Node rootNode = parser.parse();

    System.out.println(outputController.toString());
    outputController.writeToFile("listing.txt");

    System.out.println();
    printNode(rootNode);
  }

  private void printNode(Node current) {
    if (current == null) {
      System.out.println("Nothing to print! Perhaps an error occurred?");
      return;
    }

    System.out.print(current.toString());

    if (current.getLeftChild() != null) {
      printNode(current.getLeftChild());
    }

    if (current.getRightChild() != null) {
      printNode(current.getRightChild());
    }
  }
}
