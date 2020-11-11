package cd20.symboltable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTable {
  private final Map<String, Symbol> symbols = new LinkedHashMap<>();
  private final String scope;

  public SymbolTable(String scope) {
    this.scope = scope;
  }

  /**
   * Insert a new symbol into the table.
   * @param symbol Symbol to insert.
   */
  public void insertSymbol(Symbol symbol) {
    symbols.put(symbol.getName(), symbol);
  }

  /**
   * Search this symbol table for a symbol with the given name.
   * @param name Symbol name to search for.
   * @return Matching symbol of Null if not found.
   */
  public Symbol resolve(String name) {
    for (String symbolName : symbols.keySet()) {
      if (symbolName.equals(name)) {
        return symbols.get(symbolName);
      }
    }

    return null;
  }

  /**
   * Get a collection of all symbols stored within this table.
   */
  public Collection<Symbol> getSymbols() {
    return symbols.values();
  }

  public String getScope() {
    return scope;
  }

  public void printDebug() {
    for (String name : symbols.keySet()) {
      System.out.println(name + ": " + symbols.get(name).toString());
    }
  }
}
