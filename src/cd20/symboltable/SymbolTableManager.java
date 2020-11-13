package cd20.symboltable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import cd20.symboltable.attribute.IsParamAttribute;

/**
 * A class which manages a collection of symbol tables and scope.
 */
public class SymbolTableManager {
  private final Stack<SymbolTable> scope = new Stack<>();
  private final Map<String, SymbolTable> tables = new LinkedHashMap<>();
  private final SymbolTable constants = new SymbolTable("constants");

  private int registerOneCounter = 0;
  private int registerTwoParamCounter = -8;
  private int registerTwoCounter = 16;

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
    // Reset register two with each new scope
    registerTwoParamCounter = -8;
    registerTwoCounter = 16;

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
      symbol.setOffset(getNextAvailableOffset(symbol, register));
    }

    scope.peek().insertSymbol(symbol);
  }

  /**
   * Insert a new constant.
   * @param symbol Constant symbol.
   */
  public void insertConstant(Symbol symbol) {
    symbol.setRegister(BaseRegister.CONSTANTS);
    constants.insertSymbol(symbol);
  }

  /**
   * Fetch the next available offset for the given register.
   * @param symbol Symbol.
   * @param register Register to fetch offset from.
   */
  public int getNextAvailableOffset(Symbol symbol, BaseRegister register) {
    int offset = 0;

    switch (register) {
      case CONSTANTS:
        throw new RuntimeException("Attempted to insert a constant into symbol table.");
      case GLOBALS:
        offset = registerOneCounter;
        registerOneCounter += 8;
        break;
      case DECLARATIONS:
        if (symbol.hasAttribute(IsParamAttribute.class)) {
          offset = registerTwoParamCounter;
          registerTwoParamCounter -= 8;
        } else {
          offset = registerTwoCounter;
          registerTwoCounter += 8;
        }
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
   * Resolve a constant.
   * @param name Constant name.
   */
  public Symbol resolveConstant(String name) {
    return constants.resolve(name);
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

  public SymbolTable getConstants() {
    return constants;
  }

  public void printDebug() {
    System.out.println("\n==================");
    System.out.println("SYMBOL TABLE DEBUG");
    System.out.println("Number of tables: " + tables.size());
    System.out.println("Current scope: " + getScope());

    debugTable("constants", constants);

    for (String scope : tables.keySet()) {
      SymbolTable table = tables.get(scope);
      debugTable(scope, table);
    }

    System.out.println("==================");
  }

  private void debugTable(String scope, SymbolTable table) {
    System.out.println("\n[" + scope + "]");
    table.printDebug();
  }
}
