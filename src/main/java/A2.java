import cd20.output.HTMLBuilder;
import cd20.output.OutputController;
import cd20.parser.Node;
import cd20.parser.Parser;
import cd20.scanner.Scanner;

import java.awt.Desktop;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class A2 {
  private final Parser parser;
  private final Scanner scanner;
  private final OutputController outputController = new OutputController();

  private int printWidth = 0;
  private static final int MAX_PRINT_WIDTH = 70;
  private static final String LISTING_OUTPUT_PATH = "listing.txt";
  private static final String AST_OUTPUT_PATH = "ast.html";

  public A2(Reader reader) {
    this.scanner = new Scanner(reader, outputController);
    this.parser = new Parser(scanner, outputController);
  }

  /**
   * Run the application
   * @param args A list of command line arguments.
   */
  public void run(List<String> args) throws IOException {
    // Parse
    Node rootNode = parser.parse();

    // Output listing
    System.out.println(outputController.output());
    outputController.writeToFile(LISTING_OUTPUT_PATH);

    if (rootNode == null) {
      System.out.println("Nothing to print! Perhaps an error occurred?");
      return;
    }

    // Print AST
    traverseNodes(rootNode);

    // For better debugging of AST, print out to HTML
    HTMLBuilder builder = new HTMLBuilder(rootNode);
    builder.writeToFile(AST_OUTPUT_PATH);

    // Open AST in browser if possible
    if (args.contains("--open-ast")) {
      // Ensure functionality is supported
      if (!Desktop.isDesktopSupported()) {
        System.out.println("Error: Cannot automatically open AST in browser.");
        System.out.println("This functionality is not supported on this device.");
        System.out.println("Please open the file manually.");
        return;
      }

      Desktop.getDesktop().open(new File(AST_OUTPUT_PATH));
    } 
  }

  /**
   * Perform a pre-order traversal of the AST
   * @param root Root of AST
   */
  private void traverseNodes(Node root) {
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
    if (args.length == 0) {
      System.err.println("Path not provided.");
      return;
    }

    // Read from file at path
    File file = new File(args[0]);
    Reader reader;

    try {
      reader = new FileReader(file);
    } catch (FileNotFoundException exception) {
      System.err.println("File not found: '" + args[0] + "'");
      return;
    }

    // Attempt to run app
    try {
      new A2(reader).run(Arrays.asList(args));
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
