import cd20.output.OutputController;
import cd20.parser.Node;
import cd20.parser.Parser;
import cd20.scanner.Scanner;

import java.io.*;

public class A2 {
  private final Parser parser;
  private final Scanner scanner;
  private final OutputController outputController = new OutputController();

  public A2(Reader reader) {
    this.scanner = new Scanner(reader, outputController);
    this.parser = new Parser(scanner, outputController);
  }

  /**
   * Run the application
   */
  public void run() throws IOException {
    // Parse
    Node rootNode = parser.parse();

    // Output listing
    System.out.println(outputController.toString());
    outputController.writeToFile("listing.txt");

    // Print AST
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

    if (current.getCentreChild() != null) {
      printNode(current.getCentreChild());
    }

    if (current.getRightChild() != null) {
      printNode(current.getRightChild());
    }
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
      new A2(reader).run();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
