package cd20.parser;

import java.util.StringJoiner;

import cd20.StringUtils;

/**
 * A node within a CD20 AST.
 */
public class Node {
  private final NodeType type;
  private final String value;

  private Node left = null;
  private Node centre = null;
  private Node right = null;

  /**
   * Construct a node.
   * @param type Node type.
   */
  public Node(NodeType type) {
    this(type, null);
  }

  /**
   * Construct a node.
   * @param type Node type.
   * @param value Node string value.
   */
  public Node(NodeType type, String value) {
    this.type = type;
    this.value = value;
  }

  /**
   * Convert this node into a string.
   */
  @Override
  public String toString() {
    // Create a new joiner
    StringJoiner joiner = new StringJoiner("");
    joiner.add(StringUtils.rightPad(7, type.toString()));

    // Add value if necessary
    if (value != null) {
      String str = value;

      // Add " around string node values
      if (type == NodeType.STRING) {
        str = "\"" + str + "\"";
      }

      // Append whitespace if needed
      StringBuilder builder = new StringBuilder(str);
      while (builder.length() % 7 != 0) {
        builder.append(" ");
      }

      joiner.add(builder.toString());
    }

    return joiner.toString();
  }

  public NodeType getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public Node getLeftChild() {
    return left;
  }

  public void setLeftChild(Node left) {
    this.left = left;
  }

  public Node getCentreChild() {
    return centre;
  }

  public void setCentreChild(Node centre) {
    this.centre = centre;
  }

  public Node getRightChild() {
    return right;
  }

  public void setRightChild(Node right) {
    this.right = right;
  }

  /**
   * Set the next available child.
   * @param node Node to set as child.
   */
  public void setNextChild(Node child) {
    // Do not continue if setting null
    if (child == null) return;

    if (left == null) {
      this.setLeftChild(child);
    } else if (centre == null) {
      this.setCentreChild(child);
    } else if (right == null) {
      this.setRightChild(child);
    } else {
      // Create a copy of the current node type
      Node node = new Node(type);
      node.setNextChild(right);
      node.setNextChild(child);
      this.setRightChild(node);
    }
  }

  public boolean hasChildren() {
    return left != null || centre != null || right != null;
  }
}
