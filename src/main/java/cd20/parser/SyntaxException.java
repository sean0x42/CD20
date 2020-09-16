package cd20.parser;

import cd20.scanner.Token;

public class SyntaxException extends Exception {
  private final Token token;

  public SyntaxException(String message, Token token) {
    super("Syntax error: " + message);
    this.token = token;
  }

  public Token getToken() {
    return token;
  }
}
