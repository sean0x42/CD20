package cd20.parser;

public enum SType {
  INT,
  REAL,
  STRING,
  BOOL,
  ARRAY,
  STRUCT;

  public static SType fromNode(Node node) {
    switch (node.getValue()) {
      case "int":
        return SType.INT;
      case "real":
        return SType.REAL;
      case "bool":
        return SType.BOOL;
      default:
        throw new UnsupportedOperationException();
    }
  }
}
