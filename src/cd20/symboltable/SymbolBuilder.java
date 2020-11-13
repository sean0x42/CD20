package cd20.symboltable;

import java.util.ArrayList;
import java.util.List;

import cd20.scanner.Token;
import cd20.symboltable.attribute.Attribute;

public class SymbolBuilder {
  private final SymbolType type;
  private String value;
  private int line;
  private int column;
  private List<Attribute> attributes = new ArrayList<>();

  public SymbolBuilder(SymbolType type) {
    this.type = type;
  }

  public static SymbolBuilder fromType(SymbolType type) {
    return new SymbolBuilder(type);
  }

  public SymbolBuilder withValue(String value) {
    this.value = value;
    return this;
  }

  public SymbolBuilder withTokenPosition(Token token) {
    this.line = token.getLine();
    this.column = token.getColumn();
    return this;
  }

  public SymbolBuilder withAttribute(Attribute attribute) {
    attributes.add(attribute);
    return this;
  }

  public Symbol build() {
    Symbol symbol = new Symbol(type, value, line, column);
    symbol.setAttributes(attributes);
    return symbol;
  }
}
