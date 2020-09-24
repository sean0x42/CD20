import cd20.output.HTMLBuilder;
import cd20.output.OutputController;
import cd20.parser.Node;
import cd20.parser.Parser;
import cd20.scanner.Scanner;

import java.io.*;

public class A2 {
  private final Parser parser;
  private final Scanner scanner;
  private final OutputController outputController = new OutputController();

  private int printWidth = 0;
  private static final int MAX_PRINT_WIDTH = 70;

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
    traverseNodes(rootNode);

    // For better debugging of AST, print out to HTML
    HTMLBuilder builder = new HTMLBuilder(rootNode);
    builder.writeToFile("ast.html");
  }

  /**
   * Perform a pre-order traversal of the AST
   * @param root Root of AST
   */
  private void traverseNodes(Node root) {
    if (root == null) {
      System.out.println("Nothing to print! Perhaps an error occurred?");
      return;
    }

    printNode(root);

    if (root.getLeftChild() != null) {
      traverseNodes(root.getLeftChild());
    }

    if (root.getCentreChild() != null) {
      traverseNodes(root.getCentreChild());
    }

    if (root.getRightChild() != null) {
      traverseNodes(root.getRightChild());
    }
  }

  /**
   * Prints a node.
   * Automatically handles wrapping nodes once they go beyond 60 characters.
   * @param node Node to print.
   */
  private void printNode(Node node) {
    String str = node.toString();
    System.out.print(str);
    printWidth += str.length();

    if (printWidth >= MAX_PRINT_WIDTH) {
      System.out.println();
      printWidth = 0;
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
