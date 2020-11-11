package cd20.parser;

import cd20.scanner.Token;

public class ParserException extends Exception {
  private final Token token;

  public ParserException(String message, Token token) {
    super(message);
    this.token = token;
  }

  public Token getToken() {
    return token;
  }
}
