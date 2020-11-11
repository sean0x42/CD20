package cd20.symboltable;

import cd20.parser.Node;

public enum SymbolType {
  MODULE,
  FUNCTION,
  ARRAY,
  STRUCT,
  INTEGER_VARIABLE,
  INTEGER_CONSTANT,
  FLOAT_VARIABLE,
  FLOAT_CONSTANT,
  STRING_VARIABLE,
  STRING_CONSTANT,
  BOOLEAN_VARIABLE;

  public static SymbolType variableFromNode(Node node) {
    switch (node.getValue()) {
      case "int":
        return SymbolType.INTEGER_VARIABLE;
      case "real":
        return SymbolType.FLOAT_VARIABLE;
      case "bool":
        return SymbolType.BOOLEAN_VARIABLE;
      default:
        throw new UnsupportedOperationException();
    }
  }

  public boolean isVariable() {
    switch (this) {
      case INTEGER_VARIABLE:
      case FLOAT_VARIABLE:
      case STRING_VARIABLE:
      case BOOLEAN_VARIABLE:
        return true;
      default:
        return false;
    }
  }
}
