package cd20;

import java.util.StringJoiner;

public class Token {
  private final TokenType type;
  private final String lexeme;
  private final int line;
  private final int column;

  public Token(TokenType type, int line, int column) {
    this(type, null, line, column);
  }

  public Token(TokenType type, String lexeme, int line, int column) {
    this.type = type;
    this.lexeme = lexeme;
    this.line = line;
    this.column = column;
  }

  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner(" ");
    joiner.add(this.type.toString());

    if (this.lexeme != null) {
      joiner.add(this.lexeme);
    }

    // @TODO debug text here
    joiner.add("(" + line + ":" + column + ")");

    return joiner.toString();
  }

  public TokenType getType() {
    return this.type;
  }

  public String getLexeme() {
    return this.lexeme;
  }

  public int getLine() {
    return this.line;
  }

  public int getColumn() {
    return this.column;
  }
}
