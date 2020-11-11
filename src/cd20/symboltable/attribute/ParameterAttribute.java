package cd20.symboltable.attribute;

import cd20.symboltable.Symbol;

public class ParameterAttribute implements Attribute {
  private final Symbol symbol;

  public ParameterAttribute(Symbol symbol) {
    this.symbol = symbol;
  }

  public Symbol getSymbol() {
    return symbol;
  }

  @Override
  public String toString() {
    return String.format("param %s : %s", symbol.getName(), symbol.getType());
  }
}
