package cd20.parser;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
  private final Map<Integer, Symbol> symbols;

  public SymbolTable() {
    symbols = new HashMap<>();
  }

  /**
   * Insert a new symbol into the table.
   */
  public void insertSymbol(int key, Symbol symbol) {
    symbols.put(key, symbol);
  }
}
