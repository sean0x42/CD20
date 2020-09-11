package cd20.parser;

import cd20.scanner.Token;

public class Symbol {
  private final String name;
  private final int line;
  private final int column;

  public Symbol(Token token) {
    this(token.getLexeme(), token.getLine(), token.getColumn());
  }

  public Symbol(String name, int line, int column) {
    this.name = name;
    this.line = line;
    this.column = column;
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
}
