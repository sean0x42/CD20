package cd20.parser;

import cd20.scanner.Token;
import cd20.scanner.TokenType;

public class UnexpectedTokenException extends Exception {
  private final Token found;

  public UnexpectedTokenException(TokenType expected, Token found) {
    super(String.format("Error: Expected token %s, found %s", expected, found.getType()));
    this.found = found;
  }

  public Token getFoundToken() {
    return found;
  }
}
