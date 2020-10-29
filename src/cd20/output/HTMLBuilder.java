package cd20.output;

import cd20.parser.Node;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class HTMLBuilder {
  private final Node rootNode;

  private static final String TITLE = "CD20 AST Explorer";

  public HTMLBuilder(Node rootNode) {
    this.rootNode = rootNode;
  }

  /**
   * Write HTML to the given file.
   * @param path Path to HTML file.
   */
  public void writeToFile(String path) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(path));
      generate(writer);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Generate HTML for this AST.
   * @param writer A buffered writer to print output to.
   */
  private void generate(BufferedWriter writer) throws IOException {
    writer.write("<!doctype html>");
    writer.write("<html>");
    writer.write("<head>");
    writer.write("<title>" + TITLE + "</title>");
    writer.write("</head>");
    writer.write(
        "<style>" +
        "body {" +
        "  font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\"," +
        "               Roboto, Oxygen-Sans, Ubuntu, Cantarell," +
        "               \"Helvetica Neue\", sans-serif;" +
        "}" +
        "details {" +
        "  border-left: 1px dashed #888;" +
        "}" +
        "code {" +
        "  font-size: 16px;" +
        "  line-height: 1.6;" +
        "}" +
        ".child-nodes {" +
        "  margin-left: 1.75rem;" +
        "}" +
        ".leaf {" +
        "  display: block;" +
        "}" +
        "</style>"
    );
    writer.write("<body>");

    writer.write("<h1>" + TITLE + "</h1>");
    traverseNode(writer, rootNode);

    writer.write("</body>");
    writer.write("</html>");
  }

  /**
   * Recursively traverse the tree.
   * Performs a pre-order traversal.
   * @param writer A buffered writer to print output to.
   * @param node Current root of tree.
   */
  private void traverseNode(BufferedWriter writer, Node node) throws IOException {
    if (!node.hasChildren()) {
      writer.write("<code class=\"leaf\">" + node.toString() + "</code>");
      return;
    }

    writer.write("<details>");
    writer.write("<summary><code>" + node.toString() + "</code></summary>");
    writer.write("<div class=\"child-nodes\">");

    if (node.getLeftChild() != null) {
      traverseNode(writer, node.getLeftChild());
    }

    if (node.getCentreChild() != null) {
      traverseNode(writer, node.getCentreChild());
    }

    if (node.getRightChild() != null) {
      traverseNode(writer, node.getRightChild());
    }

    writer.write("</div>");
    writer.write("</details>");
  }
}
