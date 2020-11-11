package cd20.symboltable;

import java.util.ArrayList;
import java.util.List;

import cd20.scanner.Token;
import cd20.symboltable.attribute.Attribute;

public class Symbol {
  private final SymbolType type;
  private final String name;
  private final int line;
  private final int column;

  private String scope;
  private List<Attribute> attributes = new ArrayList<>();

  private BaseRegister register;
  private int offset = Integer.MIN_VALUE;

  /**
   * Construct a symbol from a token.
   * @param type Symbol type.
   * @param token Token to use as basis for symbol.
   */
  public Symbol(SymbolType type, Token token) {
    this(type, token.getLexeme(), token.getLine(), token.getColumn());
  }

  /**
   * Construct a symbol.
   * @param type Symbol type.
   * @param name Symbol's unique name within the current scope.
   * @param line Line that symbol was encountered.
   * @param column Column that symbol was encountere.
   */
  public Symbol(SymbolType type, String name, int line, int column) {
    this.type = type;
    this.name = name;
    this.line = line;
    this.column = column;
  }

  /**
   * Add a new attribute.
   * @param attribute Attribute to add.
   */
  public void addAttribute(Attribute attribute) {
    attributes.add(attribute);
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getScope() {
    return scope;
  }

  public SymbolType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public BaseRegister getRegister() {
    return register;
  }

  public void setRegister(BaseRegister register) {
    this.register = register;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public List<Attribute> getAttributes() {
    return attributes;
  }

  /**
   * Get all matching attributes.
   * @param clazz Class to get attributes for.
   * @return A list of matching attributes.
   */
  public <T extends Attribute> List<Attribute> getAttributes(Class<T> clazz) {
    List<Attribute> matching = new ArrayList<>();

    for (Attribute attribute : attributes) {
      if (clazz.isInstance(attribute)) {
        matching.add(attribute);
      }
    }

    return matching;
  }

  /**
   * Get the first matching attribute.
   * @param clazz Class to get attributes for.
   * @return Matching attribute or null.
   */
  public <T extends Attribute> T getFirstAttribute(Class<T> clazz) {
    for (Attribute attribute : attributes) {
      if (clazz.isInstance(attribute)) {
        return clazz.cast(attribute);
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return String.format(
        "[%s] (%d, %d)",
        type.name(),
        register != null ? register.getId() : -1,
        offset
    );
  }
}
