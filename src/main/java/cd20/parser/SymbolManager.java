package cd20.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class SymbolManager {
  private final Stack<String> scopes;
  private final Map<String, SymbolTable> tables;

  public SymbolManager() {
    scopes = new Stack<>();
    tables = new HashMap<>();
  }

  /**
   * Push a new scope.
   */
  public void pushScope(String scope) {
    scopes.push(scope);
    tables.put(scope, new SymbolTable());
  }

  /**
   * Leave the current scope.
   */
  public String popScope() {
    return scopes.pop();
  }

  /**
   * Inserts a new symbol into the current {@link SymbolTable}.
   * @return Unique symbol identifier.
   */
  public int insertSymbol(Symbol symbol) {
    // Compute a unique symbol key by mushing together symbol name and scope
    int symbolKey = Objects.hash(symbol.getName(), scopes.peek());

    // Insert
    tables.get(scopes.peek()).insertSymbol(symbolKey, symbol);

    return symbolKey;
  }
}
