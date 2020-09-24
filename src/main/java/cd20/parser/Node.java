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

  public Node(NodeType type) {
    this(type, null);
  }

  public Node(NodeType type, String value) {
    this.type = type;
    this.value = value;
  }

  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner("");

    joiner.add(StringUtils.rightPad(7, type.toString()));

    if (value != null) {
      StringBuilder builder = new StringBuilder(value);

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

  public void setNextChild(Node child) {
    // Do not continue if setting null
    if (child == null) return;

    if (left == null) {
      this.setLeftChild(child);
    } else if (centre == null) {
      this.setCentreChild(child);
    } else {
      this.setRightChild(child);
    }
  }

  public boolean hasChildren() {
    return left != null || centre != null || right != null;
  }
}
