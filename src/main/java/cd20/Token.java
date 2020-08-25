package cd20;

/**
 * Represents a token within a CD20 source file.
 */
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
    String out = type.toString();

    if (lexeme != null) {
      if (type == TokenType.STRING_LITERAL) {
        out += "\"" + lexeme + "\"";
      } else {
        out += lexeme;
      }

      out += " ";

      // Append whitespace to fill out one column
      while (out.length() % 6 != 0) {
        out += " ";
      }
    }

    return out;
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
