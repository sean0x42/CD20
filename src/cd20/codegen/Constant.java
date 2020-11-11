package cd20.codegen;

import cd20.symboltable.Symbol;

public class Constant<T> {
  private final T value;
  private final Symbol symbol;

  public Constant(T value, Symbol symbol) {
    this.value = value;
    this.symbol = symbol;
  }

  public T getValue() {
    return value;
  }

  public Symbol getSymbol() {
    return symbol;
  }
}
