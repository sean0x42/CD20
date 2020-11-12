package cd20.symboltable.attribute;

import cd20.parser.DataType;
import cd20.parser.Node;
import cd20.scanner.Token;
import cd20.symboltable.Symbol;
import cd20.symboltable.SymbolType;

public class AttributeUtils {
  public static DataType getDataType(Node node) {
    Symbol symbol = node.getSymbol();
    if (symbol == null) return null;

    DataTypeAttribute attr = symbol.getFirstAttribute(DataTypeAttribute.class);
    if (attr == null) return null;

    return attr.getType();
  }

  public static void propogateDataType(Node child, Node parent, Token parentToken) {
    DataType type = getDataType(child);

    if (parent.getSymbol() == null) {
      parent.setSymbol(new Symbol(SymbolType.TEMPORARY, parentToken));
    }

    parent.getSymbol().addAttribute(new DataTypeAttribute(type));
  }
}
