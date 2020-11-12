package cd20.symboltable;

import cd20.parser.DataType;
import cd20.parser.Node;

public enum SymbolType {
  TEMPORARY,
  FUNCTION,
  ARRAY,
  STRUCT,
  INTEGER_VARIABLE,
  INTEGER_CONSTANT,
  FLOAT_VARIABLE,
  FLOAT_CONSTANT,
  STRING_CONSTANT,
  BOOLEAN_VARIABLE,
  STRUCT_OR_ARRAY_VARIABLE;

  public static SymbolType fromDataType(DataType type) {
    if (type.isInteger()) {
      return SymbolType.INTEGER_VARIABLE;
    } else if (type.isReal()) {
      return SymbolType.FLOAT_VARIABLE;
    } else if (type.isBoolean()) {
      return SymbolType.BOOLEAN_VARIABLE;
    } else {
      return SymbolType.STRUCT_OR_ARRAY_VARIABLE;
    }
  }

  /**
   * @deprecated
   */
  public static SymbolType variableFromNode(Node node) {
    switch (node.getValue()) {
      case "int":
        return SymbolType.INTEGER_VARIABLE;
      case "real":
        return SymbolType.FLOAT_VARIABLE;
      case "bool":
        return SymbolType.BOOLEAN_VARIABLE;
      default:
        throw new UnsupportedOperationException(node.toString());
    }
  }

  public boolean isVariable() {
    switch (this) {
      case INTEGER_VARIABLE:
      case FLOAT_VARIABLE:
      case BOOLEAN_VARIABLE:
        return true;
      default:
        return false;
    }
  }
}
