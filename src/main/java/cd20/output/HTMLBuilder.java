package cd20.output;

import cd20.parser.Node;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class HTMLBuilder {
  private final Node rootNode;

  public HTMLBuilder(Node rootNode) {
    this.rootNode = rootNode;
  }

  public void writeToFile(String path) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(path));
      generate(writer);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void generate(BufferedWriter writer) throws IOException {
    writer.write("<!doctype html>");
    writer.write("<html>");
    writer.write("<head>");
    writer.write("<title>CD20 AST Preview</title>");
    writer.write("</head>");
    writer.write("<style>" +
        "body {" +
        "  font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\"," +
        "               Roboto, Oxygen-Sans, Ubuntu, Cantarell," +
        "               \"Helvetica Neue\", sans-serif;" +
        "}" +
        "ol {" +
        "  padding-left: 0;" +
        "  margin-left: 1rem;" +
        "  border-left: 1px dashed #888;" +
        "  list-style-type: none;" +
        "  line-height: 1.0;" +
        "}" +
        "li {" +
        "  margin-bottom: 1rem;" +
        "}" +
        "</style>");
    writer.write("<body>");

    writer.write("<h1>CD20 AST Preview</h1>");
    traverseNode(writer, rootNode);

    writer.write("</body>");
    writer.write("</html>");
  }

  private void traverseNode(BufferedWriter writer, Node node) throws IOException {
    writer.write("<ol>");

    writer.write("<li>" + node.toString() + "</li>");

    if (node.getLeftChild() != null) {
      traverseNode(writer, node.getLeftChild());
    }

    if (node.getCentreChild() != null) {
      traverseNode(writer, node.getCentreChild());
    }

    if (node.getRightChild() != null) {
      traverseNode(writer, node.getRightChild());
    }

    writer.write("</ol>");
  }
}
