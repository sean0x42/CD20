package cd20.symboltable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A class which manages a collection of symbol tables and scope.
 */
public class SymbolTableManager {
  private final Stack<SymbolTable> scope = new Stack<>();
  private final Map<String, SymbolTable> tables = new LinkedHashMap<>();

  private int registerZeroCounter = 0;
  private int registerOneCounter = 0;
  private int registerTwoCounter = 0;

  /**
   * Get the name of the current scope.
   */
  public String getScope() {
    if (scope.isEmpty()) {
      return null;
    }

    return scope.peek().getScope();
  }

  /**
   * Creates a new symbol table at the given scope.
   * @param scope Name of scope.
   * @return A symbol table that exists at this scope.
   */
  public SymbolTable createScope(String scope) {
    SymbolTable table = new SymbolTable(scope);
    tables.put(scope, table);
    return enterScope(scope);
  }

  /**
   * Enter a new scope.
   * @return A symbol table that exists at this scope.
   */
  public SymbolTable enterScope(String scope) {
    registerTwoCounter = 0;
    this.scope.push(tables.get(scope));
    return tables.get(scope);
  }

  /**
   * Leave the current scope.
   */
  public void leaveScope() {
    scope.pop();
  }

  /**
   * Inserts a new symbol into the current SymbolTable.
   * @param symbol Symbol to insert.
   */
  public void insertSymbol(Symbol symbol) {
    insertSymbol(symbol, null);
  }

  /**
   * Inserts a new symbol into the current SymbolTable.
   * @param symbol Symbol to insert.
   * @param register Register to record symbol to.
   */
  public void insertSymbol(Symbol symbol, BaseRegister register) {
    symbol.setScope(getScope());

    if (register != null) {
      symbol.setRegister(register);
      symbol.setOffset(getNextAvailableOffset(register));
    }

    scope.peek().insertSymbol(symbol);
  }

  /**
   * Fetch the next available offset for the given register.
   * @param register Register to fetch offset from.
   */
  public int getNextAvailableOffset(BaseRegister register) {
    int offset = 0;

    switch (register) {
      case CONSTANTS:
        offset = registerZeroCounter;
        registerZeroCounter += 8;
        break;
      case GLOBALS:
        offset = registerOneCounter;
        registerOneCounter += 8;
        break;
      case DECLARATIONS:
        offset = registerTwoCounter;
        registerTwoCounter += 8;
        break;
    }

    return offset;
  }

  /**
   * Search for a symbol by name.
   * @param name Name of symbol to resolve.
   * @return Found symbol or null.
   */
  public Symbol resolve(String name) {
    // Clone the stack so that we can pop freely. We're just cloning references
    // (I believe) so this shouldn't cost too much space.
    Stack<SymbolTable> stack = (Stack<SymbolTable>) scope.clone();
    
    // Start at the current scope, moving gradually higher until we find
    // a matching symbol.
    while (stack.size() > 0) {
      SymbolTable table = stack.pop();
      Symbol symbol = table.resolve(name);
      
      if (symbol != null) {
        return symbol;
      }
    }

    return null;
  }

  /**
   * Determine whether a symbol with the given name exists in the current scope.
   * @param name Name of symbol to search for.
   * @return Whether the symbol exists within the current scope.
   */
  public boolean containsSymbol(String name) {
    SymbolTable table = scope.peek();
    return table.resolve(name) != null;
  }

  public Collection<SymbolTable> getTables() {
    return tables.values();
  }

  public void printDebug() {
    System.out.println("\n==================");
    System.out.println("SYMBOL TABLE DEBUG");
    System.out.println("Number of tables: " + tables.size());
    System.out.println("Current scope: " + getScope());

    for (String scope : tables.keySet()) {
      SymbolTable table = tables.get(scope);
      System.out.println("\n[" + scope + "]");
      table.printDebug();
    }

    System.out.println("==================");
  }
}
